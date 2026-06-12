/*
   Copyright 2020-2026 WeAreFrank!

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
package org.frankframework.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.TimeProvider;

public class CorePkiUtil {

	private static final Pattern PEM_PATTERN = Pattern.compile(
			"(?m)^-----BEGIN ([A-Z0-9 ]+)-----\\s*$" +
					"([A-Za-z0-9+/=\\r\\n]++)" +
					"^-----END \\1-----\\s*$"
	);

	private CorePkiUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static HasTruststore keyStoreAsTrustStore(HasKeystore keystoreOwner) {
		TruststoreConfiguration truststoreConfiguration = new TruststoreConfiguration();
		truststoreConfiguration.setTruststoreResource(keystoreOwner.getKeystore());
		truststoreConfiguration.setTruststoreType(keystoreOwner.getKeystoreType());
		truststoreConfiguration.setTruststoreAuthAlias(keystoreOwner.getKeystoreAuthAlias());
		truststoreConfiguration.setTruststorePassword(keystoreOwner.getKeystorePassword());
		truststoreConfiguration.setTrustManagerAlgorithm(keystoreOwner.getKeyManagerAlgorithm());

		return new HasTruststore() {

			@Override
			public void setTruststoreConfiguration(TruststoreConfiguration truststoreConfiguration) {
				throw new NotImplementedException("This method is not implemented because the truststore configuration is derived from the keystore " +
						"configuration and cannot be set independently.");
			}

			@Override
			public TruststoreConfiguration getTruststoreConfiguration() {
				return truststoreConfiguration;
			}

			@Override
			public ClassLoader getConfigurationClassLoader() {
				return keystoreOwner.getConfigurationClassLoader();
			}
		};
	}

	public static KeyStore createKeyStore(HasKeystore keystoreOwner) throws EncryptionException {
		URL truststoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
		CredentialFactory truststoreCredentialFactory = new CredentialFactory(keystoreOwner.getKeystoreAuthAlias(), null, keystoreOwner.getKeystorePassword());
		try {
			return CommonsPkiUtil.createKeyStore(truststoreUrl, truststoreCredentialFactory.getPassword(), keystoreOwner.getKeystoreType());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("unable to open keystore [" + truststoreUrl + "]", e);
		}
	}

	public static KeyStore createKeyStore(HasTruststore truststoreOwner) throws EncryptionException {
		URL truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
		CredentialFactory truststoreCredentialFactory = new CredentialFactory(truststoreOwner.getTruststoreAuthAlias(), null, truststoreOwner.getTruststorePassword());
		try {
			return CommonsPkiUtil.createKeyStore(truststoreUrl, truststoreCredentialFactory.getPassword(), truststoreOwner.getTruststoreType());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("unable to open keystore [" + truststoreUrl + "]", e);
		}
	}

	public static PrivateKey getPrivateKey(HasKeystore keystoreOwner) throws EncryptionException {
		PrivateKey privateKey;
		URL keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
		try {
			if (keystoreOwner.getKeystoreType() == KeystoreType.PEM) {
				privateKey = CorePkiUtil.getPrivateKeyFromPem(keystoreUrl);
			} else {
				CredentialFactory keystoreCredentialFactory;
				CredentialFactory keystoreAliasCredentialFactory;
				keystoreCredentialFactory = new CredentialFactory(keystoreOwner.getKeystoreAuthAlias(), null, keystoreOwner.getKeystorePassword());
				if (StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasAuthAlias()) || StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasPassword())) {
					keystoreAliasCredentialFactory = new CredentialFactory(keystoreOwner.getKeystoreAliasAuthAlias(), null, keystoreOwner.getKeystoreAliasPassword());
				} else {
					keystoreAliasCredentialFactory = keystoreCredentialFactory;
				}
				KeyStore keystore = CommonsPkiUtil.createKeyStore(keystoreUrl, keystoreCredentialFactory.getPassword(), keystoreOwner.getKeystoreType());
				String password = keystoreAliasCredentialFactory.getPassword() != null ? keystoreAliasCredentialFactory.getPassword() : "";
				privateKey = (PrivateKey) keystore.getKey(keystoreOwner.getKeystoreAlias(), password.toCharArray());
			}
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException | UnrecoverableKeyException e) {
			throw new EncryptionException("cannot obtain Private Key in alias [" + keystoreOwner.getKeystoreAlias() + "] of keystore [" + keystoreOwner.getKeystore() + "]", e);
		}
		if (privateKey == null) {
			throw new EncryptionException("no Signing Key found in alias [" + keystoreOwner.getKeystoreAlias() + "] of keystore [" + keystoreOwner.getKeystore() + "]");
		}
		return privateKey;
	}

	public static PublicKey getPublicKey(HasTruststore truststoreOwner) throws EncryptionException {
		return getCertificate(truststoreOwner).getPublicKey();
	}

	public static Certificate getCertificate(HasTruststore truststoreOwner) throws EncryptionException {
		Certificate certificate;
		URL truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
		try {
			if (truststoreOwner.getTruststoreType() == KeystoreType.PEM) {
				certificate = CorePkiUtil.getCertificateFromPem(truststoreUrl);
			} else {
				CredentialFactory truststoreCredentialFactory = new CredentialFactory(truststoreOwner.getTruststoreAuthAlias(), null, truststoreOwner.getTruststorePassword());
				KeyStore keystore = CommonsPkiUtil.createKeyStore(truststoreUrl, truststoreCredentialFactory.getPassword(), truststoreOwner.getTruststoreType());
				TrustManager[] trustmanagers = CommonsPkiUtil.createTrustManagers(keystore, truststoreOwner.getTrustManagerAlgorithm());
				if (trustmanagers == null || trustmanagers.length == 0) {
					throw new EncryptionException("no trustmanager for keystore [" + truststoreUrl + "]");
				}
				X509TrustManager trustManager = (X509TrustManager) trustmanagers[0];
				X509Certificate[] certificates = trustManager.getAcceptedIssuers();
				if (certificates == null || certificates.length == 0) {
					throw new EncryptionException("no Verfication Key found in keystore [" + truststoreUrl + "]");
				}
				certificate = certificates[0];
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("cannot get Public Key for verification in keystore [" + truststoreUrl + "]", e);
		}
		return certificate;
	}

	/**
	 * Returns a list of certificate aliases which are due to expire.
	 *
	 * @param keystore A {@link KeyStore}.
	 * @param duration Date after which Certificates should be classified as 'due to expire'.
	 * @return A list with aliases of {@link Certificate Certificates}.
	 */
	@NonNull
	public static List<String> getExpiringCertificates(KeyStore keystore, TemporalAmount duration) throws EncryptionException {
		List<String> certificates = new ArrayList<>();
		Instant dateAfterWhichCertsAreExpired = TimeProvider.now().plus(duration);
		try {
			for (String certAlias : Collections.list(keystore.aliases())) {
				Certificate cert = keystore.getCertificate(certAlias);
				if (isDueToExpire(cert, dateAfterWhichCertsAreExpired)) {
					certificates.add(certAlias);
				}
			}
		} catch (KeyStoreException e) {
			throw new EncryptionException("unable to read certificate from keystore", e);
		}
		return certificates;
	}

	private static boolean isDueToExpire(Certificate cert, Instant dateAfterWhichCertsAreExpired) {
		if (cert instanceof X509Certificate x509Cert) {
			Instant notAfter = x509Cert.getNotAfter().toInstant();
			return dateAfterWhichCertsAreExpired.isAfter(notAfter);
		}

		return false;
	}

	private static byte[] loadPEM(URL resource) throws IOException {
		try (InputStream in = resource.openStream()) {
			String pem = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
			Matcher m = PEM_PATTERN.matcher(pem);
			if (!m.find()) throw new IOException("Invalid PEM format");
			return Base64.getMimeDecoder().decode(m.group(2));
		}
	}

	private static PrivateKey getPrivateKeyFromPem(URL resource) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		KeyFactory kf = KeyFactory.getInstance("RSA");
		byte[] pkcs8EncodedKeySpec = loadPEM(resource);
		return kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKeySpec));
	}

	private static Certificate getCertificateFromPem(URL resource) throws CertificateException, IOException {
		CertificateFactory fact = CertificateFactory.getInstance("X.509");
		return fact.generateCertificate(resource.openStream());
	}
}
