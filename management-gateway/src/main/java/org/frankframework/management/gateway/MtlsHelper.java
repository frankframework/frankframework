/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.management.gateway;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MtlsHelper {
	@Getter
	private final PrivateKey privateKey;
	@Getter
	private final PublicKey publicKey;
	private HttpClient httpClient;
	private final SSLContext sslContext;

	@Getter
	private final KeyStore keyStore;
	@Getter
	private final SSLProperties sslProperties;

	public MtlsHelper() {
		this.sslProperties = new SSLProperties();
		KeyStore clientKs = loadStoreWithFallback(
				sslProperties.getKeyStore(),
				sslProperties.getKeyStorePassword(),
				"PKCS12", "JKS"
		);
		this.keyStore = clientKs;

		try {
			Enumeration<String> aliases = clientKs.aliases();
			if (!aliases.hasMoreElements()) {
				throw new IllegalArgumentException("Keystore contains no aliases");
			}
			String alias = aliases.nextElement();
			Key key = clientKs.getKey(alias, sslProperties.getKeyStorePassword().toCharArray());
			if (!(key instanceof PrivateKey)) {
				throw new IllegalArgumentException("Alias " + alias + " is not a private key");
			}
			this.privateKey = (PrivateKey) key;
			Certificate cert = clientKs.getCertificate(alias);
			if (cert == null) {
				throw new IllegalArgumentException("No certificate found under alias " + alias);
			}
			this.publicKey = cert.getPublicKey();
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to extract keys from keystore", e);
		}

		this.sslContext = buildSSLContext(clientKs, sslProperties);
	}

	protected HttpClient getHttpClient() {
		if (httpClient == null) {
			SSLParameters sslParameters = new SSLParameters();
			sslParameters.setEndpointIdentificationAlgorithm("");
			httpClient = HttpClient.newBuilder()
					.sslContext(sslContext)
					.sslParameters(sslParameters)
					.connectTimeout(Duration.ofSeconds(5))
					.build();
		}
		return httpClient;
	}

	private SSLContext buildSSLContext(KeyStore clientKs, SSLProperties sslProperties) {
		try {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(clientKs, sslProperties.getKeyStorePassword().toCharArray());

			KeyStore trustKs = loadStoreWithFallback(
					sslProperties.getTrustStore(),
					sslProperties.getTrustStorePassword(),
					"PKCS12", "JKS"
			);
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(trustKs);

			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
			return ctx;
		} catch (GeneralSecurityException e) {
			log.error("Failed to build SSL context", e);
			throw new RuntimeException(e);
		}
	}

	private KeyStore loadStoreWithFallback(String location, String password, String primaryType, String fallbackType) {
		try {
			return getKeyStore(location, password, primaryType);
		} catch (GeneralSecurityException e) {
			log.warn("Failed to load '{}' as {}: {}", location, primaryType, e.getMessage());
		}
		try {
			return getKeyStore(location, password, fallbackType);
		} catch (GeneralSecurityException ex) {
			throw new RuntimeException("Unable to load keystore as " + primaryType + " or " + fallbackType, ex);
		}
	}

	private KeyStore getKeyStore(String location, String password, String type) throws GeneralSecurityException {
		try {
			log.info("Loading keystore '{}' as type {}", location, type);
			KeyStore ks = KeyStore.getInstance(type);
			try (InputStream in = resolveResource(location)) {
				ks.load(in, password.toCharArray());
			}
			log.info(" → successfully loaded {} as {}", location, type);
			return ks;
		} catch (IOException ioe) {
			throw new GeneralSecurityException("I/O error loading keystore " + location, ioe);
		}
	}

	private InputStream resolveResource(String location) throws FileNotFoundException {
		if (location.startsWith("classpath:")) {
			String path = location.substring("classpath:".length());
			InputStream is = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(path);
			if (is == null) {
				throw new FileNotFoundException("Classpath resource not found: " + path);
			}
			return is;
		} else {
			return new FileInputStream(location);
		}
	}

	/**
	 * Decrypts an RSA/OAEP-SHA256 ciphertext with private key.
	 */
	public byte[] decrypt(byte[] ciphertext) throws GeneralSecurityException {
		Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		c.init(Cipher.DECRYPT_MODE, privateKey);
		return c.doFinal(ciphertext);
	}

	/**
	 * Encrypts plaintext with public key.
	 */
	public byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
		Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		c.init(Cipher.ENCRYPT_MODE, publicKey);
		return c.doFinal(plaintext);
	}

	/**
	 * Hybrid encryption: AES-GCM for payload + RSA-OAEP for AES key.
	 */
	private byte[] doHybridEncryption(byte[] plaintext, PublicKey rsaPublicKey) throws GeneralSecurityException {
		// Generate AES key
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey aesKey = keyGen.generateKey();

		// AES-GCM encrypt payload
		Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
		byte[] iv = new byte[12];
		new SecureRandom().nextBytes(iv);
		GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
		byte[] encryptedPayload = aesCipher.doFinal(plaintext);

		// RSA encrypt AES key
		byte[] encryptedAesKey = rsaEncrypt(aesKey.getEncoded(), rsaPublicKey);

		// Combine components
		ByteBuffer buffer = ByteBuffer.allocate(4 + encryptedAesKey.length + iv.length + encryptedPayload.length);
		buffer.putInt(encryptedAesKey.length)
				.put(encryptedAesKey)
				.put(iv)
				.put(encryptedPayload);
		return buffer.array();
	}

	private byte[] rsaEncrypt(byte[] data, PublicKey key) throws GeneralSecurityException {
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		rsaCipher.init(Cipher.ENCRYPT_MODE, key);
		return rsaCipher.doFinal(data);
	}

	public byte[] encryptHybrid(byte[] plaintext) throws GeneralSecurityException {
		return doHybridEncryption(plaintext, publicKey);
	}

	public byte[] encryptHybrid(byte[] plaintext, PublicKey externalKey) throws GeneralSecurityException {
		return doHybridEncryption(plaintext, externalKey);
	}

	public byte[] decryptHybrid(byte[] ciphertext) throws GeneralSecurityException {
		ByteBuffer buffer = ByteBuffer.wrap(ciphertext);
		int aesKeyLen = buffer.getInt();
		byte[] encryptedAesKey = new byte[aesKeyLen];
		buffer.get(encryptedAesKey);
		byte[] aesKeyBytes = decrypt(encryptedAesKey);
		SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
		byte[] iv = new byte[12];
		buffer.get(iv);
		byte[] encryptedPayload = new byte[buffer.remaining()];
		buffer.get(encryptedPayload);

		Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
		aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
		return aesCipher.doFinal(encryptedPayload);
	}
}
