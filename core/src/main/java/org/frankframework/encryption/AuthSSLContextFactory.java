/*
   Copyright 2020-2022 WeAreFrank!

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

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.KeyManagerUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

public class AuthSSLContextFactory {
	protected static Logger log = LogUtil.getLogger(MethodHandles.lookup().lookupClass());

	protected @Setter @Getter String protocol = "TLSv1.2";

	private final HasKeystore keystoreOwner;
	private final HasTruststore truststoreOwner;

	protected SSLContext sslContext = null;

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

	public static SSLSocketFactory createSSLSocketFactory(HasKeystore keystoreOwner, HasTruststore truststoreOwner, String protocol) throws GeneralSecurityException {
		AuthSSLContextFactory sslContextFactory = new AuthSSLContextFactory(keystoreOwner, truststoreOwner, protocol);
		return sslContextFactory.getSSLSocketFactory();
	}

	public static SSLContext createSSLContext(HasKeystore keystoreOwner, HasTruststore truststoreOwner, String protocol) throws GeneralSecurityException {
		AuthSSLContextFactory sslContextFactory = new AuthSSLContextFactory(keystoreOwner, truststoreOwner, protocol);
		return sslContextFactory.getSSLContext();
	}

	private AuthSSLContextFactory(HasKeystore keystoreOwner, HasTruststore truststoreOwner, String protocol) {

		this.keystoreOwner = keystoreOwner;
		this.truststoreOwner = truststoreOwner;

		if (StringUtils.isNotEmpty(protocol)) {
			this.protocol = protocol;
		}
	}

	private SSLContext createSSLContext() throws GeneralSecurityException, EncryptionException {
		KeyStore keystore = null;
		KeyStore truststore = null;
		SSLContext sslcontext;

		// Has to be explicitly defined.
		if (keystoreOwner != null && StringUtils.isNotEmpty(keystoreOwner.getKeystore())) {
			keystore = CorePkiUtil.createKeyStore(keystoreOwner);
		}
		// Has to be explicitly defined.
		if (truststoreOwner != null) {
			// Should never be null...
			truststore = CorePkiUtil.createKeyStore(truststoreOwner);
		}
		boolean allowSelfSignedCertificates = truststoreOwner != null && truststoreOwner.isAllowSelfSignedCertificates();

		if (keystore == null && truststore == null && !allowSelfSignedCertificates) {
			sslcontext = SSLContext.getDefault();
		} else {
			KeyManager[] keymanagers = null;
			if (keystore != null) {
				final String password;
				if (StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasAuthAlias()) || StringUtils.isNotEmpty(keystoreOwner.getKeystoreAliasPassword())) {
					CredentialFactory keystoreAliasCf = new CredentialFactory(keystoreOwner.getKeystoreAliasAuthAlias(), null, keystoreOwner.getKeystoreAliasPassword());
					password = keystoreAliasCf.getPassword();
				} else {
					log.warn("no password provided for the certificate (alias) [{}] in keystore [{}]", keystoreOwner.getKeystoreAlias(), keystoreOwner.getKeystore());
					password = null;
					// Do we want the old behavior of using the keystore password for the key? It is not recommended to use the same password for both the keystore and the key.
					// keystoreAliasCf = new CredentialFactory(keystoreOwner.getKeystoreAuthAlias(), null, keystoreOwner.getKeystorePassword());
				}
				if (StringUtils.isNotEmpty(keystoreOwner.getKeystoreAlias())) {
					keymanagers = new KeyManager[] {KeyManagerUtils.createClientKeyManager(keystore, keystoreOwner.getKeystoreAlias(), password)};
				} else {
					keymanagers = CommonsPkiUtil.createKeyManagers(keystore, password, keystoreOwner.getKeyManagerAlgorithm());
				}
			}

			TrustManager[] trustmanagers = null;
			if (truststore != null) {
				String algorithm = truststoreOwner.getTrustManagerAlgorithm();
				trustmanagers = CommonsPkiUtil.createTrustManagers(truststore, algorithm);
			}

			if (allowSelfSignedCertificates) {
				trustmanagers = new TrustManager[] {
					new SelfSignedCertificateAcceptingTrustManagerWrapper(truststore, trustmanagers)
				};
			}

			sslcontext = SSLContext.getInstance(protocol);
			sslcontext.init(keymanagers, trustmanagers, null);
		}
		return sslcontext;
	}

	private SSLContext getSSLContext() throws GeneralSecurityException {
		if (sslContext == null) {
			try {
				sslContext = createSSLContext();
			} catch (EncryptionException e) {
				throw new GeneralSecurityException("unable to create keystore", e);
			}
		}
		return sslContext;
	}

	public SSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException {
		URL keystoreUrl = null;
		URL truststoreUrl = null;
		if (keystoreOwner != null && StringUtils.isNotEmpty(keystoreOwner.getKeystore())) {
			keystoreUrl = ClassLoaderUtils.getResourceURL(keystoreOwner, keystoreOwner.getKeystore());
		}
		if (truststoreOwner != null && StringUtils.isNotEmpty(truststoreOwner.getTruststore())) {
			truststoreUrl = ClassLoaderUtils.getResourceURL(truststoreOwner, truststoreOwner.getTruststore());
		}
		if (keystoreUrl == null && truststoreUrl == null && (truststoreOwner == null || !truststoreOwner.isAllowSelfSignedCertificates())) {
			// Add javax.net.ssl.SSLSocketFactory.getDefault() SSLSocketFactory if none has been set.
			// See: http://httpcomponents.10934.n7.nabble.com/Upgrading-commons-httpclient-3-x-to-HttpClient4-x-td19333.html
			//
			// The first time this method is called, the security property "ssl.SocketFactory.provider" is examined.
			// If it is non-null, a class by that name is loaded and instantiated. If that is successful and the
			// object is an instance of SSLSocketFactory, it is made the default SSL socket factory.
			// Otherwise, this method returns SSLContext.getDefault().getSocketFactory(). If that call fails, an inoperative factory is returned.
			return (SSLSocketFactory) SSLSocketFactory.getDefault();
		}
		return getSSLContext().getSocketFactory();
	}


	/**
	 * Helper class for testing certificates that are not verified by an
	 * authorized organisation
	 *
	 * @author John Dekker
	 */
	class SelfSignedCertificateAcceptingTrustManagerWrapper implements X509TrustManager {
		private X509TrustManager trustManager;

		SelfSignedCertificateAcceptingTrustManagerWrapper(KeyStore truststore, TrustManager[] trustmanagers) throws NoSuchAlgorithmException, KeyStoreException {
			if (trustmanagers == null || trustmanagers.length == 0) {
				TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				factory.init(truststore);
				trustmanagers = factory.getTrustManagers();
			}
			if (trustmanagers.length != 1) {
				throw new NoSuchAlgorithmException("Only works with X509 trustmanagers");
			}
			trustManager = (X509TrustManager)trustmanagers[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			trustManager.checkClientTrusted(certificates, authType);
		}

		/**
		 * checkServerTrusted() trusts the server if each of the certificates in the chain is valid.
		 * It does not compare the root certificate to a list of trusted certificates.
		 */
		@Override
		public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			try {
				if (certificates != null) {
					for (X509Certificate certificate : certificates) {
						certificate.checkValidity();
					}
				}
			} catch (CertificateException e) {
				if (truststoreOwner != null && truststoreOwner.isIgnoreCertificateExpiredException()) {
					log.warn("error occurred during checking trusted server: {}", e.getMessage());
				} else {
					throw e;
				}
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return trustManager.getAcceptedIssuers();
		}
	}

}
