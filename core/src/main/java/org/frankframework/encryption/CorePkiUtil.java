/*
   Copyright 2020, 2021, 2025 WeAreFrank!

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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TimeProvider;

@Log4j2
public class CorePkiUtil {

	private CorePkiUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static HasTruststore keyStoreAsTrustStore(HasKeystore keystoreOwner) {
		return new HasTruststore() {

			@Override
			public ClassLoader getConfigurationClassLoader() {
				return keystoreOwner.getConfigurationClassLoader();
			}

			@Override
			public String getTruststore() {
				return keystoreOwner.getKeystore();
			}

			@Override
			public KeystoreType getTruststoreType() {
				return keystoreOwner.getKeystoreType();
			}

			@Override
			public String getTruststoreAuthAlias() {
				return keystoreOwner.getKeystoreAuthAlias();
			}

			@Override
			public String getTruststorePassword() {
				return keystoreOwner.getKeystorePassword();
			}

			@Override
			public String getTrustManagerAlgorithm() {
				return keystoreOwner.getKeyManagerAlgorithm();
			}

			@Override
			public boolean isVerifyHostname() {
				return true;
			}

			@Override
			public boolean isAllowSelfSignedCertificates() {
				return false;
			}

			@Override
			public boolean isIgnoreCertificateExpiredException() {
				return false;
			}

			@Override
			public void setTruststore(String truststore) {
				keystoreOwner.setKeystore(truststore);
			}

			@Override
			public void setTruststoreType(KeystoreType truststoreType) {
				keystoreOwner.setKeystoreType(truststoreType);
			}

			@Override
			public void setTruststoreAuthAlias(String truststoreAuthAlias) {
				keystoreOwner.setKeystoreAuthAlias(truststoreAuthAlias);
			}

			@Override
			public void setTruststorePassword(String truststorePassword) {
				keystoreOwner.setKeystorePassword(truststorePassword);
			}

			@Override
			public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
				keystoreOwner.setKeyManagerAlgorithm(trustManagerAlgorithm);
			}

			@Override
			public void setVerifyHostname(boolean verifyHostname) {
				throw new NotImplementedException();
			}

			@Override
			public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
				throw new NotImplementedException();
			}

			@Override
			public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
				throw new NotImplementedException();
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
	@Nonnull
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
		InputStream in = resource.openStream();
		String pem = StreamUtil.streamToString(in, null, "ISO_8859_1");
		Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
		String encoded = parse.matcher(pem).replaceFirst("$1");
		return Base64.decodeBase64(encoded);
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
