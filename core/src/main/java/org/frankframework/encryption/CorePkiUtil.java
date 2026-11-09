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
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
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

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.TimeProvider;

@Log4j2
public class CorePkiUtil {

	private static final Pattern PEM_PATTERN = Pattern.compile(
			"(?m)^-----BEGIN ([A-Z0-9 ]+)-----\\s*$" +
					"([A-Za-z0-9+/=\\r\\n]++)" +
					"^-----END \\1-----\\s*$"
	);

	private CorePkiUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static void verifyKeystoreConfiguration(HasKeystore keystoreOwner, HasTruststore truststoreOwner) throws ConfigurationException {
		if (keystoreOwner != null && StringUtils.isNotEmpty(keystoreOwner.getKeystore())) {
			URL keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
			if (keystoreUrl == null) {
				throw new ConfigurationException("cannot find URL for keystore resource [" + keystoreOwner.getKeystore() + "]");
			}
			log.debug("resolved keystore-URL to [{}]", keystoreUrl);

			if (keystoreOwner.getKeystoreType() == KeystoreType.PKCS12 && (StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasPassword()) || StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasAuthAlias()))) {
				ConfigurationWarnings.add(keystoreOwner, log, "KeystoreType [" + KeystoreType.PKCS12 + "] does not guarantee to support using different passwords for the keys and the keystore.", SuppressKeys.MULTIPASSWORD_KEYSTORE_SUPPRESS_KEY);
			}
		}
		if (truststoreOwner != null && StringUtils.isNotEmpty(truststoreOwner.getTruststore())) {
			URL truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
			if (truststoreUrl == null) {
				throw new ConfigurationException("cannot find URL for truststore resource [" + truststoreOwner.getTruststore() + "]");
			}
			log.debug("resolved truststore-URL to [{}]", truststoreUrl);
		}
	}

	public static HasTruststore keyStoreAsTrustStore(HasKeystore keystoreOwner) {
		TruststoreConfiguration truststoreConfiguration = new TruststoreConfiguration();
		truststoreConfiguration.setResource(keystoreOwner.getKeystore());
		truststoreConfiguration.setType(keystoreOwner.getKeystoreType());
		truststoreConfiguration.setAuthAlias(keystoreOwner.getKeystoreAuthAlias());
		truststoreConfiguration.setPassword(keystoreOwner.getKeystorePassword());
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

	@SuppressWarnings("deprecation")
	public static KeyStore createKeyStore(HasKeystore keystoreOwner) throws EncryptionException {
		return createKeyStore(keystoreOwner.getKeystoreConfiguration());
	}

	public static KeyStore createKeyStore(KeystoreConfiguration keystoreOwner) throws EncryptionException {
		if (StringUtils.isBlank(keystoreOwner.getResource())) {
			// If no keystore is specified, the JVMs default keystore will be used.
			try {
				log.info("no Keystore resource specified, using default keystore");
				return CommonsPkiUtil.getDefaultKeyStore();
			} catch (IllegalStateException e) {
				throw new EncryptionException("unable to open default keystore", e);
			}
		}

		URL keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getResource());
		CredentialFactory credentialFactory = new CredentialFactory(keystoreOwner.getAuthAlias(), null, keystoreOwner.getPassword());
		try {
			return CommonsPkiUtil.createKeyStore(keystoreUrl, credentialFactory.getPassword(), keystoreOwner.getType());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("unable to open keystore [" + keystoreUrl + "]", e);
		}
	}

	public static KeyStore createKeyStore(HasTruststore truststoreOwner) throws EncryptionException {
		if (StringUtils.isBlank(truststoreOwner.getTruststore())) {
			// If no truststore is specified, the JVMs default truststore will be used.
			try {
				log.info("no TrustStore resource specified, using default truststore");
				return CommonsPkiUtil.getDefaultTrustStore();
			} catch (IllegalStateException e) {
				throw new EncryptionException("unable to open default truststore", e);
			}
		}

		URL truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
		CredentialFactory truststoreCredentialFactory = new CredentialFactory(truststoreOwner.getTruststoreAuthAlias(), null, truststoreOwner.getTruststorePassword());
		try {
			return CommonsPkiUtil.createKeyStore(truststoreUrl, truststoreCredentialFactory.getPassword(), truststoreOwner.getTruststoreType());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("unable to open keystore [" + truststoreUrl + "]", e);
		}
	}

	public static PrivateKey getPrivateKey(HasKeystore keystoreOwner) throws EncryptionException {
		try {
			if (keystoreOwner.getKeystoreType() == KeystoreType.PEM) {
				// PEM is a single certificate.
				URL keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
				return CorePkiUtil.getPrivateKeyFromPem(keystoreUrl);
			} else {
				KeyStore keystore = createKeyStore(keystoreOwner);

				CredentialFactory keystoreCredentialFactory;
				CredentialFactory keystoreAliasCredentialFactory;
				keystoreCredentialFactory = new CredentialFactory(keystoreOwner.getKeystoreAuthAlias(), null, keystoreOwner.getKeystorePassword());
				if (StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasAuthAlias()) || StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasPassword())) {
					keystoreAliasCredentialFactory = new CredentialFactory(keystoreOwner.getKeystoreAliasAuthAlias(), null, keystoreOwner.getKeystoreAliasPassword());
				} else {
					keystoreAliasCredentialFactory = keystoreCredentialFactory;
				}
				String password = keystoreAliasCredentialFactory.getPassword() != null ? keystoreAliasCredentialFactory.getPassword() : "";

				PrivateKey privateKey = (PrivateKey) keystore.getKey(keystoreOwner.getKeystoreAlias(), password.toCharArray());
				if (privateKey == null) {
					throw new EncryptionException("no Private Key found with name [" + keystoreOwner.getKeystoreAlias() + "] in keystore [" + keystoreOwner.getKeystore() + "]");
				}
				return privateKey;
			}
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException | KeyStoreException | UnrecoverableKeyException e) {
			throw new EncryptionException("cannot obtain Private Key with name [" + keystoreOwner.getKeystoreAlias() + "] in keystore [" + keystoreOwner.getKeystore() + "]", e);
		}
	}

	public static Certificate getCertificate(HasKeystore keystoreOwner) throws EncryptionException {
		try {
			if (keystoreOwner.getKeystoreType() == KeystoreType.PEM) {
				// PEM is a single certificate.
				URL keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
				return CorePkiUtil.getCertificateFromPem(keystoreUrl);
			} else {
				KeyStore keystore = createKeyStore(keystoreOwner);

				Certificate certificate = keystore.getCertificate(keystoreOwner.getKeystoreAlias());
				if (certificate == null) {
					throw new EncryptionException("no Certificate found with name [" + keystoreOwner.getKeystoreAlias() + "] in keystore [" + keystoreOwner.getKeystore() + "]");
				}
				return certificate;
			}
		} catch (GeneralSecurityException | IOException e) {
			throw new EncryptionException("cannot obtain Private Key with name [" + keystoreOwner.getKeystoreAlias() + "] in keystore [" + keystoreOwner.getKeystore() + "]", e);
		}
	}

	public static Certificate getFirstCertificateFromTrustStore(HasTruststore truststoreOwner) throws EncryptionException {
		String truststoreName = truststoreOwner.getTruststore();
		try {
			if (truststoreOwner.getTruststoreType() == KeystoreType.PEM) {
				// PEM is a single certificate.
				URL truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
				if (truststoreUrl == null) {
					throw new EncryptionException("cannot find URL for truststore resource [" + truststoreName + "]");
				}
				return CorePkiUtil.getCertificateFromPem(truststoreUrl);
			} else {
				KeyStore keystore = createKeyStore(truststoreOwner);

				TrustManager[] trustManagers = CommonsPkiUtil.createTrustManagers(keystore, truststoreOwner.getTrustManagerAlgorithm());
				if (trustManagers == null || trustManagers.length == 0) {
					throw new EncryptionException("no TrustManager for keystore [" + truststoreName + "]");
				}
				X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
				X509Certificate[] certificates = trustManager.getAcceptedIssuers();
				if (certificates == null || certificates.length == 0) {
					throw new EncryptionException("no Verification Key found in keystore [" + truststoreName + "]");
				}
				return certificates[0];
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new EncryptionException("cannot get Public Key for verification in keystore [" + truststoreName + "]", e);
		}
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
			String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
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
		try (InputStream in = resource.openStream()) {
			return fact.generateCertificate(in);
		}
	}
}
