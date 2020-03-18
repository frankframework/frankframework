package nl.nn.adapterframework.http;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

public class AuthSSLConnectionSocket {
	private static Logger log = LogUtil.getLogger(AuthSSLConnectionSocket.class);
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
			boolean ignoreCertificateExpiredException) throws GeneralSecurityException, IOException {
		AuthSSLConnectionSocket socket = new AuthSSLConnectionSocket(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, null);
		return socket.getSSLContext();
	}

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
		AuthSSLConnectionSocket socket = new AuthSSLConnectionSocket(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, protocol);
		return socket.getSSLContext();
	}

	public AuthSSLConnectionSocket(URL keystoreUrl, String keystorePassword, String keystoreType, String keyManagerAlgorithm, 
			URL truststoreUrl, String truststorePassword, String truststoreType, String trustManagerAlgorithm, 
			boolean allowSelfSignedCertificates, boolean verifyHostname, boolean ignoreCertificateExpiredException) {
		this(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, 
				trustManagerAlgorithm, allowSelfSignedCertificates, ignoreCertificateExpiredException, null);
	}

	public AuthSSLConnectionSocket(URL keystoreUrl, String keystorePassword, String keystoreType, String keyManagerAlgorithm, 
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

	private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password, String algorithm) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing key manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm=KeyManagerFactory.getDefaultAlgorithm();
			log.debug("using default KeyManager algorithm ["+algorithm+"]");
		} else {
			log.debug("using configured KeyManager algorithm ["+algorithm+"]");
		}
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(algorithm);
		kmfactory.init(keystore, password != null ? password.toCharArray(): null);
		return kmfactory.getKeyManagers(); 
	}

	private static TrustManager[] createTrustManagers(final KeyStore keystore, String algorithm) throws KeyStoreException, NoSuchAlgorithmException { 
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing trust manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm=TrustManagerFactory.getDefaultAlgorithm();
			log.debug("using default TrustManager algorithm ["+algorithm+"]");
		} else {
			log.debug("using configured TrustManager algorithm ["+algorithm+"]");
		}
		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(algorithm);
		tmfactory.init(keystore);
		TrustManager[] trustmanagers = tmfactory.getTrustManagers();
		return trustmanagers; 
	}

	private SSLContext createSSLContext() throws GeneralSecurityException, IOException {
		KeyManager[] keymanagers = null;
		TrustManager[] trustmanagers = null;
		if (keystoreUrl != null) {
			KeyStore keystore = createKeyStore(keystoreUrl, keystorePassword, keystoreType, "Certificate chain");
			keymanagers = createKeyManagers(keystore, keystorePassword, keyManagerAlgorithm);
		}
		if (truststoreUrl != null) {
			KeyStore keystore = createKeyStore(truststoreUrl, truststorePassword, truststoreType, "Trusted Certificate");
			trustmanagers = createTrustManagers(keystore, trustManagerAlgorithm);
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

	private static KeyStore createKeyStore(final URL url, final String password, String keyStoreType, String prefix) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url for "+prefix+" may not be null");
		}
		log.info("Initializing keystore for "+prefix+" from "+url.toString());
		KeyStore keystore  = KeyStore.getInstance(keyStoreType);
		keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
		if (log.isInfoEnabled()) {
			Enumeration<String> aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = (String)aliases.nextElement();
				log.info(prefix+" '" + alias + "':");
				Certificate trustedcert = keystore.getCertificate(alias);
				if (trustedcert != null && trustedcert instanceof X509Certificate) {
					X509Certificate cert = (X509Certificate)trustedcert;
					log.info("  Subject DN: " + cert.getSubjectDN());
					log.info("  Signature Algorithm: " + cert.getSigAlgName());
					log.info("  Valid from: " + cert.getNotBefore() );
					log.info("  Valid until: " + cert.getNotAfter());
					log.info("  Issuer: " + cert.getIssuerDN());
				}
			}
		}
		return keystore;
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

		public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			trustManager.checkClientTrusted(certificates, authType);
		}

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

		public X509Certificate[] getAcceptedIssuers() {
			return trustManager.getAcceptedIssuers();
		}
	}

}
