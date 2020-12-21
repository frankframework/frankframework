/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.io.IOException;
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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.PkiUtil;

public class AuthSSLContextFactory {
	private static Logger log = LogUtil.getLogger(AuthSSLContextFactory.class);
	private String protocol = "SSL";

	private boolean allowSelfSignedCertificates = false;
	private URL keystoreUrl = null;
	private String keystorePassword = null;
	private String keystoreType = "null";
	private String keyManagerAlgorithm = null;
	private URL truststoreUrl = null;
	private String truststorePassword = null;
	private String truststoreType = "null";
	private String trustManagerAlgorithm = null;
	private SSLContext sslContext = null;
	private boolean ignoreCertificateExpiredException=false;

//	public static SSLContext createSSLContext(
//			URL keystoreUrl, 
//			String keystorePassword, 
//			String keystoreType, 
//			String keyManagerAlgorithm, 
//			URL truststoreUrl, 
//			String truststorePassword, 
//			String truststoreType, 
//			String trustManagerAlgorithm, 
//			boolean allowSelfSignedCertificates, 
//			boolean verifyHostname, 
//			boolean ignoreCertificateExpiredException) throws GeneralSecurityException, IOException {
//		AuthSSLContextFactory socket = new AuthSSLContextFactory(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
//				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, null);
//		return socket.getSSLContext();
//	}

	public static SSLContext createSSLContext(
			URL keystoreUrl, 
			String keystorePassword, 
			String keystoreType, 
			String keyManagerAlgorithm, 
			URL truststoreUrl, 
			String truststorePassword, 
			String truststoreType, 
			String trustManagerAlgorithm, 
			boolean allowSelfSignedCertificates, 
			boolean verifyHostname, 
			boolean ignoreCertificateExpiredException, 
			String protocol) throws GeneralSecurityException, IOException {
		AuthSSLContextFactory socket = new AuthSSLContextFactory(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, protocol);
		return socket.getSSLContext();
	}

	public AuthSSLContextFactory(URL keystoreUrl, String keystorePassword, String keystoreType, String keyManagerAlgorithm, 
			URL truststoreUrl, String truststorePassword, String truststoreType, String trustManagerAlgorithm, 
			boolean allowSelfSignedCertificates, boolean verifyHostname, boolean ignoreCertificateExpiredException) {
		this(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, null);
	}

	public AuthSSLContextFactory(URL keystoreUrl, String keystorePassword, String keystoreType, String keyManagerAlgorithm, 
			URL truststoreUrl, String truststorePassword, String truststoreType, String trustManagerAlgorithm, 
			boolean allowSelfSignedCertificates, boolean ignoreCertificateExpiredException, String protocol) {

		this.keystoreUrl = keystoreUrl;
		this.keystorePassword = keystorePassword;
		this.keystoreType = keystoreType;
		this.keyManagerAlgorithm = keyManagerAlgorithm;

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
			keymanagers = PkiUtil.createKeyManagers(keystore, keystorePassword, keyManagerAlgorithm);
		}
		if (truststoreUrl != null) {
			KeyStore keystore = PkiUtil.createKeyStore(truststoreUrl, truststorePassword, truststoreType, "Trusted Certificate");
			trustmanagers = PkiUtil.createTrustManagers(keystore, trustManagerAlgorithm);
			if (allowSelfSignedCertificates) {
				trustmanagers = new TrustManager[] {
					new AuthSslTrustManager(keystore, trustmanagers)
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
