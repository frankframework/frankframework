/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

//import com.sun.net.ssl.KeyManager;
//import com.sun.net.ssl.KeyManagerFactory;
//import com.sun.net.ssl.SSLContext;
//import com.sun.net.ssl.TrustManager;
//import com.sun.net.ssl.TrustManagerFactory;

import java.util.Enumeration;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
// javax.net.ssl is jdk 1.4.x ...
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import nl.nn.adapterframework.http.AuthSSLProtocolSocketFactoryBase;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.ReflectionSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * <p>
 * AuthSSLProtocolSocketFactory can be used to validate the identity of the
 * HTTPS server against a list of trusted certificates and to authenticate to
 * the HTTPS server using a private key.
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable server authentication when supplied
 * with a {@link KeyStore truststore} file containg one or several trusted
 * certificates. The client secure socket will reject the connection during the
 * SSL session handshake if the target HTTPS server attempts to authenticate
 * itself with a non-trusted certificate.
 * </p>
 * 
 * <p>
 * Use JDK keytool utility to import a trusted certificate and generate a
 * truststore file:
 * 
 * <pre>
 *     keytool -import -alias "my server cert" -file server.crt -keystore my.truststore
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable client authentication when supplied
 * with a {@link KeyStore keystore} file containg a private key/public
 * certificate pair. The client secure socket will use the private key to
 * authenticate itself to the target HTTPS server during the SSL session
 * handshake if requested to do so by the server. The target HTTPS server will
 * in its turn verify the certificate presented by the client in order to
 * establish client's authenticity
 * </p>
 * 
 * <p>
 * Use the following sequence of actions to generate a keystore file
 * </p>
 * <ul>
 * <li>
 * <p>
 * Use JDK keytool utility to generate a new key
 * 
 * <pre>
 * keytool -genkey -v -alias "my client key" -validity 365 -keystore my.keystore
 * </pre>
 * 
 * For simplicity use the same password for the key as that of the keystore
 * </p>
 * </li>
 * <li>
 * <p>
 * Issue a certificate signing request (CSR)
 * 
 * <pre>
 * keytool -certreq -alias "my client key" -file mycertreq.csr -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Send the certificate request to the trusted Certificate Authority for
 * signature. One may choose to act as her own CA and sign the certificate
 * request using a PKI tool, such as OpenSSL.
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the trusted CA root certificate
 * 
 * <pre>
 * keytool -import -alias "my trusted ca" -file caroot.crt -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the PKCS#7 file containg the complete certificate chain
 * 
 * <pre>
 * keytool -import -alias "my client key" -file mycert.p7 -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Verify the content the resultant keystore file
 * 
 * <pre>
 * keytool -list -v -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * </ul>
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 * 
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(
 * 		new URL(&quot;file:my.keystore&quot;), &quot;mypassword&quot;,
 * 		new URL(&quot;file:my.truststore&quot;), &quot;mypassword&quot;), 443);
 * 
 * HttpClient client = new HttpClient();
 * client.getHostConfiguration().setHost(&quot;localhost&quot;, 443, authhttps);
 * // use relative url only
 * GetMethod httpget = new GetMethod(&quot;/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 * 
 * </p>
 * <p>
 * Example of using custom protocol socket factory per default instead of the
 * standard one:
 * 
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(
 * 		new URL(&quot;file:my.keystore&quot;), &quot;mypassword&quot;,
 * 		new URL(&quot;file:my.truststore&quot;), &quot;mypassword&quot;), 443);
 * Protocol.registerProtocol(&quot;https&quot;, authhttps);
 * 
 * HttpClient client = new HttpClient();
 * GetMethod httpget = new GetMethod(&quot;https://localhost/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 * 
 * </p>
 */

public class IbisSSLSocketFactory extends SSLSocketFactory {
	protected static Logger log = LogUtil.getLogger(IbisSSLSocketFactory.class);

	protected String protocol = "SSL";

	protected boolean allowSelfSignedCertificates = false;
	protected URL keystoreUrl = null;
	protected String keystorePassword = null;
	protected String keystoreType = "null";
	protected String keyManagerAlgorithm = null;
	protected URL truststoreUrl = null;
	protected String truststorePassword = null;
	protected String truststoreType = "null";
	protected String trustManagerAlgorithm = null;
	protected Object sslContext = null;
	protected boolean verifyHostname = true;
	protected boolean ignoreCertificateExpiredException = false;

	/**
	 * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or
	 * truststore file must be given. Otherwise SSL context initialization error
	 * will result.
	 * 
	 * @param keystoreUrl
	 *            URL of the keystore file. May be <tt>null</tt> if HTTPS client
	 *            authentication is not to be used.
	 * @param keystorePassword
	 *            Password to unlock the keystore. IMPORTANT: this
	 *            implementation assumes that the same password is used to
	 *            protect the key and the keystore itself.
	 * @param truststoreUrl
	 *            URL of the truststore file. May be <tt>null</tt> if HTTPS
	 *            server authentication is not to be used.
	 * @param truststorePassword
	 *            Password to unlock the truststore.
	 */
	public IbisSSLSocketFactory(final URL keystoreUrl,
			final String keystorePassword, final String keystoreType,
			final String keyManagerAlgorithm, final URL truststoreUrl,
			final String truststorePassword, final String truststoreType,
			final String trustManagerAlgorithm,
			final boolean allowSelfSignedCertificates,
			final boolean verifyHostname,
			final boolean ignoreCertificateExpiredException) {
		super();
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
		this.keystoreUrl = keystoreUrl;
		this.keystorePassword = keystorePassword;
		this.keystoreType = keystoreType;
		this.keyManagerAlgorithm = keyManagerAlgorithm;
		this.truststoreUrl = truststoreUrl;
		this.truststorePassword = truststorePassword;
		this.truststoreType = truststoreType;
		this.trustManagerAlgorithm = trustManagerAlgorithm;
		this.verifyHostname = verifyHostname;
		this.ignoreCertificateExpiredException = ignoreCertificateExpiredException;
	}

	private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password, String algorithm) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing key manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm = KeyManagerFactory.getDefaultAlgorithm();
			log.debug("using default KeyManager algorithm [" + algorithm + "]");
		} else {
			log.debug("using configured KeyManager algorithm [" + algorithm + "]");
		}
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(algorithm);
		kmfactory.init(keystore, password != null ? password.toCharArray() : null);
		return kmfactory.getKeyManagers();
	}

	private static TrustManager[] createTrustManagers(final KeyStore keystore, String algorithm) throws KeyStoreException, NoSuchAlgorithmException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		log.debug("Initializing trust manager");
		if (StringUtils.isEmpty(algorithm)) {
			algorithm = TrustManagerFactory.getDefaultAlgorithm();
			log.debug("using default TrustManager algorithm [" + algorithm + "]");
		} else {
			log.debug("using configured TrustManager algorithm [" + algorithm + "]");
		}
		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(algorithm);
		tmfactory.init(keystore);
		TrustManager[] trustmanagers = tmfactory.getTrustManagers();
		return trustmanagers;
	}

	private SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		KeyManager[] keymanagers = null;
		TrustManager[] trustmanagers = null;
		if (this.keystoreUrl != null) {
			KeyStore keystore = createKeyStore(this.keystoreUrl, this.keystorePassword, this.keystoreType, "Certificate chain");
			keymanagers = createKeyManagers(keystore, this.keystorePassword, this.keyManagerAlgorithm);
		}
		if (this.truststoreUrl != null) {
			KeyStore keystore = createKeyStore(this.truststoreUrl, this.truststorePassword, this.truststoreType, "Trusted Certificate");
			trustmanagers = createTrustManagers(keystore, this.trustManagerAlgorithm);
			if (allowSelfSignedCertificates) {
				trustmanagers = new TrustManager[] { new AuthSslTrustManager( keystore, trustmanagers) };
			}
		} else if (allowSelfSignedCertificates) {
			trustmanagers = new TrustManager[] { new AuthSslTrustManager(null, trustmanagers) };
		}
		SSLContext sslcontext = SSLContext.getInstance(protocol);
		sslcontext.init(keymanagers, trustmanagers, null);
		return sslcontext;
	}

	public void initSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		if (this.sslContext == null) {
			sslContext = createSSLContext();
		}
	}

	private SSLContext getSSLContext() {
		if (this.sslContext == null) {
			initSSLContextNoExceptions();
		}
		return (SSLContext) this.sslContext;
	}

	protected void initSSLContextNoExceptions() {
		if (this.sslContext == null) {
			try {
				initSSLContext();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Unsupported algorithm exception", e);
			} catch (KeyStoreException e) {
				throw new RuntimeException("Keystore exception", e);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException("Key management exception", e);
			} catch (IOException e) {
				throw new RuntimeException(
						"I/O error reading keystore/truststore file", e);
			}
		}
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	 */
	public Socket createSocket(String host, int port, InetAddress clientHost,
			int clientPort) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory()
				.createSocket(host, port, clientHost, clientPort);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	 */
	public Socket createSocket(String host, int port) throws IOException,
	UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory()
				.createSocket(host, port);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	 */
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory()
				.createSocket(socket, host, port, autoClose);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * @see AuthSSLProtocolSocketFactoryBase#createSocket(InetAddress, int)
	 */
	public Socket createSocket(InetAddress adress, int port) throws IOException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory()
				.createSocket(adress, port);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * @see AuthSSLProtocolSocketFactoryBase#createSocket(InetAddress, int,
	 *      InetAddress, int)
	 */
	public Socket createSocket(InetAddress adress, int port,
			InetAddress localAdress, int localPort) throws IOException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory()
				.createSocket(adress, port, localAdress, localPort);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * Attempts to get a new socket connection to the given host within the
	 * given time limit.
	 * <p>
	 * This method employs several techniques to circumvent the limitations of
	 * older JREs that do not support connect timeout. When running in JRE 1.4
	 * or above reflection is used to call Socket#connect(SocketAddress
	 * endpoint, int timeout) method. When executing in older JREs a controller
	 * thread is executed. The controller thread attempts to create a new socket
	 * within the given limit of time. If socket constructor does not return
	 * until the timeout expires, the controller terminates and throws an
	 * {@link ConnectTimeoutException}
	 * </p>
	 * 
	 * @param host
	 *            the host name/IP
	 * @param port
	 *            the port on the host
	 * @param localAddress
	 *            the local host name/IP to bind the socket to
	 * @param localPort
	 *            the port on the local machine
	 * @param params
	 *            {@link HttpConnectionParams Http connection parameters}
	 * 
	 * @return Socket a new socket
	 * 
	 * @throws IOException
	 *             if an I/O error occurs while creating the socket
	 * @throws UnknownHostException
	 *             if the IP address of the host cannot be determined
	 * 
	 * @author Copied from HttpClient 3.0.1 SSLProtocolSocketFactory
	 * @since 3.0
	 */
	public Socket createSocket(final String host, final int port,
			final InetAddress localAddress, final int localPort,
			final HttpConnectionParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		int timeout = params.getConnectionTimeout();
		if (timeout == 0) {
			return createSocket(host, port, localAddress, localPort);
		}
		// To be eventually deprecated when migrated to Java 1.4 or above
		Socket socket = ReflectionSocketFactory.createSocket(
				"javax.net.ssl.SSLSocketFactory", host, port, localAddress,
				localPort, timeout);
		return socket;
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
			trustManager = (X509TrustManager) trustmanagers[0];
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

	/**
	 * Describe <code>verifyHostname</code> method here.
	 * 
	 * @param socket
	 *            a <code>SSLSocket</code> value
	 * @exception SSLPeerUnverifiedException
	 *                If there are problems obtaining the server certificates
	 *                from the SSL session, or the server host name does not
	 *                match with the "Common Name" in the server certificates
	 *                SubjectDN.
	 * @exception UnknownHostException
	 *                If we are not able to resolve the SSL sessions returned
	 *                server host name.
	 */
	protected void verifyHostname(SSLSocket socket)
			throws SSLPeerUnverifiedException, UnknownHostException {
		if (!verifyHostname)
			return;

		SSLSession session = socket.getSession();
		if (session == null) {
			throw new UnknownHostException(
					"could not obtain session from socket");
		}
		String hostname = session.getPeerHost();
		try {
			InetAddress.getByName(hostname);
		} catch (UnknownHostException uhe) {
			String msg = "Could not resolve SSL sessions server hostname: "
					+ hostname;
			// Under WebSphere, hostname can be equal to proxy-hostname
			log.warn(msg, uhe);
			// throw new UnknownHostException(msg);
		}

		javax.security.cert.X509Certificate[] certs = session
				.getPeerCertificateChain();
		if (certs == null || certs.length == 0)
			throw new SSLPeerUnverifiedException(
					"No server certificates found!");

		// get the servers DN in its string representation
		String dn = certs[0].getSubjectDN().getName();

		// might be useful to print out all certificates we receive from the
		// server, in case one has to debug a problem with the installed certs.
		if (log.isInfoEnabled()) {
			log.info("Server certificate chain:");
			for (int i = 0; i < certs.length; i++) {
				log.info("X509Certificate[" + i + "]=" + certs[i]);
			}
		}
		// get the common name from the first cert
		String cn = getCN(dn);
		if (hostname.equalsIgnoreCase(cn)) {
			if (log.isInfoEnabled()) {
				log.info("Target hostname valid: " + cn);
			}
		} else {
			throw new SSLPeerUnverifiedException(
					"HTTPS hostname invalid: expected '" + hostname
					+ "', received '" + cn + "'");
		}
	}

	/**
	 * Parses a X.500 distinguished name for the value of the "Common Name"
	 * field. This is done a bit sloppy right now and should probably be done a
	 * bit more according to <code>RFC 2253</code>.
	 * 
	 * @param dn
	 *            a X.500 distinguished name.
	 * @return the value of the "Common Name" field.
	 */
	protected String getCN(String dn) {
		int i = 0;
		i = dn.indexOf("CN=");
		if (i == -1) {
			return null;
		}
		// get the remaining DN without CN=
		dn = dn.substring(i + 3);
		// System.out.println("dn=" + dn);
		char[] dncs = dn.toCharArray();
		for (i = 0; i < dncs.length; i++) {
			if (dncs[i] == ',' && i > 0 && dncs[i - 1] != '\\') {
				break;
			}
		}
		return dn.substring(0, i);
	}

	protected static KeyStore createKeyStore(final URL url,
			final String password, String keyStoreType, String prefix)
					throws KeyStoreException, NoSuchAlgorithmException,
					CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url for " + prefix
					+ " may not be null");
		}
		log.info("Initializing keystore for " + prefix + " from "
				+ url.toString());
		KeyStore keystore = KeyStore.getInstance(keyStoreType);
		keystore.load(url.openStream(),
				password != null ? password.toCharArray() : null);
		if (log.isInfoEnabled()) {
			Enumeration aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = (String) aliases.nextElement();
				log.info(prefix + " '" + alias + "':");
				Certificate trustedcert = keystore.getCertificate(alias);
				if (trustedcert != null
						&& trustedcert instanceof X509Certificate) {
					X509Certificate cert = (X509Certificate) trustedcert;
					log.info("  Subject DN: " + cert.getSubjectDN());
					log.info("  Signature Algorithm: " + cert.getSigAlgName());
					log.info("  Valid from: " + cert.getNotBefore());
					log.info("  Valid until: " + cert.getNotAfter());
					log.info("  Issuer: " + cert.getIssuerDN());
				}
			}
		}
		return keystore;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String[] getDefaultCipherSuites() {
		return null;
	}

	public String[] getSupportedCipherSuites() {
		return null;
	}
}
