/*
   Copyright 2020, 2021 WeAreFrank!

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
package nl.nn.adapterframework.encryption;

import java.io.IOException;
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
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.KeyManagerUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

public class AuthSSLContextFactory {
	protected static Logger log = LogUtil.getLogger(MethodHandles.lookup().lookupClass());

	protected @Setter @Getter String protocol = "SSL";

	protected URL keystoreUrl = null;
	protected String keystorePassword = null;
	protected KeystoreType keystoreType = null;
	protected String keystoreAlias = null;
	protected String keystoreAliasPassword = null;
	protected String keyManagerAlgorithm = null;
	protected URL truststoreUrl = null;
	protected String truststorePassword = null;
	protected KeystoreType truststoreType = null;
	protected String trustManagerAlgorithm = null;
	protected boolean allowSelfSignedCertificates = false;
	protected boolean ignoreCertificateExpiredException=false;

	protected SSLContext sslContext = null;

	public static void verifyKeystoreConfiguration(HasKeystore keystoreRef, HasTruststore trustoreRef) throws ConfigurationException {
		URL keystoreUrl = null;
		URL truststoreUrl = null;

		if (keystoreRef!=null && !StringUtils.isEmpty(keystoreRef.getKeystore())) {
			keystoreUrl = ClassUtils.getResourceURL(keystoreRef, keystoreRef.getKeystore());
			if (keystoreUrl == null) {
				throw new ConfigurationException("cannot find URL for keystore resource ["+keystoreRef.getKeystore()+"]");
			}
			log.debug("resolved keystore-URL to ["+keystoreUrl.toString()+"]");
		}
		if (trustoreRef!=null && !StringUtils.isEmpty(trustoreRef.getTruststore())) {
			truststoreUrl = ClassUtils.getResourceURL(trustoreRef, trustoreRef.getTruststore());
			if (truststoreUrl == null) {
				throw new ConfigurationException("cannot find URL for truststore resource ["+trustoreRef.getTruststore()+"]");
			}
			log.debug("resolved truststore-URL to ["+truststoreUrl.toString()+"]");
		}
	}
	

	public static SSLContext createSSLContext(HasKeystore keystoreRef, HasTruststore trustoreRef, String protocol) throws GeneralSecurityException, IOException {
		URL keystoreUrl = null;
		URL truststoreUrl = null;

		if (!StringUtils.isEmpty(keystoreRef.getKeystore())) {
			keystoreUrl = ClassUtils.getResourceURL(keystoreRef, keystoreRef.getKeystore());
		}
		if (!StringUtils.isEmpty(trustoreRef.getTruststore())) {
			truststoreUrl = ClassUtils.getResourceURL(trustoreRef, trustoreRef.getTruststore());
		}
		if (keystoreUrl != null || truststoreUrl != null || trustoreRef.isAllowSelfSignedCertificates()) {
			CredentialFactory keystoreCf = new CredentialFactory(keystoreRef.getKeystoreAuthAlias(), null, keystoreRef.getKeystorePassword());
			CredentialFactory keystoreAliasCf = keystoreCf;
			if (StringUtils.isNotEmpty(keystoreRef.getKeystoreAliasAuthAlias()) || StringUtils.isNotEmpty(keystoreRef.getKeystoreAliasPassword())) {
				keystoreAliasCf = new CredentialFactory(keystoreRef.getKeystoreAliasAuthAlias(), null, keystoreRef.getKeystoreAliasPassword());
			}
			CredentialFactory truststoreCf  = new CredentialFactory(trustoreRef.getTruststoreAuthAlias(),  null, trustoreRef.getTruststorePassword());
			AuthSSLContextFactory socket = new AuthSSLContextFactory(keystoreUrl, keystoreCf.getPassword(), keystoreRef.getKeystoreType(), keystoreRef.getKeystoreAlias(), keystoreAliasCf.getPassword(), keystoreRef.getKeyManagerAlgorithm(), 
					truststoreUrl, truststoreCf.getPassword(), trustoreRef.getTruststoreType(), 
					trustoreRef.getTrustManagerAlgorithm(), trustoreRef.isAllowSelfSignedCertificates(), trustoreRef.isIgnoreCertificateExpiredException(), protocol);
			return socket.getSSLContext();
		}
		return SSLContext.getDefault();
	}

	public AuthSSLContextFactory(URL keystoreUrl, String keystorePassword, KeystoreType keystoreType, String keystoreAlias, String keystoreAliasPassword, String keyManagerAlgorithm,
			URL truststoreUrl, String truststorePassword, KeystoreType truststoreType, String trustManagerAlgorithm, 
			boolean allowSelfSignedCertificates, boolean ignoreCertificateExpiredException, String protocol) {

		this.keystoreUrl = keystoreUrl;
		this.keystorePassword = keystorePassword;
		this.keystoreType = keystoreType;
		this.keyManagerAlgorithm = keyManagerAlgorithm;
		this.keystoreAlias = keystoreAlias;
		this.keystoreAliasPassword = keystoreAliasPassword;

		this.truststoreUrl = truststoreUrl;
		this.truststorePassword = truststorePassword;
		this.truststoreType = truststoreType;
		this.trustManagerAlgorithm = trustManagerAlgorithm;

		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
		this.ignoreCertificateExpiredException = ignoreCertificateExpiredException;

		if(StringUtils.isNotEmpty(protocol))
			this.protocol = protocol;
	}


	private SSLContext createSSLContext() throws GeneralSecurityException, IOException {
		KeyManager[] keymanagers = null;
		TrustManager[] trustmanagers = null;
		if (keystoreUrl != null) {
			KeyStore keystore = PkiUtil.createKeyStore(keystoreUrl, keystorePassword, keystoreType, "Certificate chain");
			if(keystoreAlias != null) {
				keymanagers = new KeyManager[] { KeyManagerUtils.createClientKeyManager(keystore, keystoreAlias, keystoreAliasPassword)};
			} else {
				keymanagers = PkiUtil.createKeyManagers(keystore, keystoreAliasPassword, keyManagerAlgorithm);
			}
		}
		if (truststoreUrl != null) {
			KeyStore truststore = PkiUtil.createKeyStore(truststoreUrl, truststorePassword, truststoreType, "Trusted Certificate");
			trustmanagers = PkiUtil.createTrustManagers(truststore, trustManagerAlgorithm);
			if (allowSelfSignedCertificates) {
				trustmanagers = new TrustManager[] {
					new AuthSslTrustManager(truststore, trustmanagers)
				};
			}
		} else if (allowSelfSignedCertificates) {
			trustmanagers = new TrustManager[] {
				new AuthSslTrustManager(null, trustmanagers)
			};
		}
		SSLContext sslcontext = SSLContext.getInstance(protocol);
		sslcontext.init(keymanagers, trustmanagers, null);
		return sslcontext;
	}

	public SSLContext getSSLContext() throws GeneralSecurityException, IOException {
		if (sslContext == null) {
			sslContext = createSSLContext();
		}
		return sslContext;
	}


	/**
	 * Helper class for testing certificates that are not verified by an 
	 * authorized organisation
	 * 
	 * @author John Dekker
	 */
	class AuthSslTrustManager implements X509TrustManager {
		private X509TrustManager trustManager = null;

		AuthSslTrustManager(KeyStore keystore, TrustManager[] trustmanagers) throws NoSuchAlgorithmException, KeyStoreException {
			if (trustmanagers == null || trustmanagers.length == 0) {
				TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				factory.init(keystore);
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

		@Override
		public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			try {
				if (certificates != null) {
					for (int i = 0; i < certificates.length; i++) {
						X509Certificate certificate = certificates[i];
						certificate.checkValidity();
					}
				}
			} catch (CertificateException e) {
				if (ignoreCertificateExpiredException) {
					log.warn("error occurred during checking trusted server: " + e.getMessage());
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
