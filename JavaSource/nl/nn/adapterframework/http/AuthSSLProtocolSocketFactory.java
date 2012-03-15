/*
 * $Log: AuthSSLProtocolSocketFactory.java,v $
 * Revision 1.15  2012-03-15 16:53:59  m00f069
 * Made allowSelfSignedCertificates work without truststore and made it usable from the Ibis configuration.
 *
 * Revision 1.14  2011/11/30 13:52:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.12  2011/06/27 15:52:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set keyManagerAlgorithm and trustManagerAlgorithm
 *
 * Revision 1.11  2009/08/26 11:47:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * upgrade to HttpClient 3.0.1 - including idle connection cleanup
 *
 * Revision 1.10  2005/10/10 14:07:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Add allowSelfSignedCertificates, to easy up testing
 *
 * Revision 1.9  2005/10/07 14:12:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Add a protocol propery, to support TLS besides SSL
 *
 * Revision 1.8  2005/10/04 11:25:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Added two additional createSocket methods to comply with the 
 * SocketFactory createSocket methods
 *
 * Revision 1.7  2005/10/04 11:25:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Added two additional createSocket methods to comply with the 
 * SocketFactory createSocket methods
 *
 * Revision 1.6  2005/02/24 12:13:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added follow redirects and truststoretype
 *
 * Revision 1.5  2005/02/02 16:36:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hostname verification, default=false
 *
 * Revision 1.4  2004/10/14 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * refactored AuthSSLProtocolSocketFactory group
 *
 * Revision 1.3  2004/09/09 14:50:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added JDK1.3.x compatibility
 *
 * Revision 1.2  2004/09/08 14:18:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * early initialization of SocketFactory
 *
 * Revision 1.1  2004/08/31 10:12:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version, based on code from Apache HttpClient contributions
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

//import com.sun.net.ssl.KeyManager;
//import com.sun.net.ssl.KeyManagerFactory;
//import com.sun.net.ssl.SSLContext;
//import com.sun.net.ssl.TrustManager;
//import com.sun.net.ssl.TrustManagerFactory;

// javax.net.ssl is jdk 1.4.x ...
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.ReflectionSocketFactory;
import org.apache.commons.lang.StringUtils;

/**
 * <p>
 * AuthSSLProtocolSocketFactory can be used to validate the identity of the HTTPS 
 * server against a list of trusted certificates and to authenticate to the HTTPS 
 * server using a private key. 
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable server authentication when supplied with
 * a {@link KeyStore truststore} file containg one or several trusted certificates. 
 * The client secure socket will reject the connection during the SSL session handshake 
 * if the target HTTPS server attempts to authenticate itself with a non-trusted 
 * certificate.
 * </p>
 * 
 * <p>
 * Use JDK keytool utility to import a trusted certificate and generate a truststore file:    
 *    <pre>
 *     keytool -import -alias "my server cert" -file server.crt -keystore my.truststore
 *    </pre>
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable client authentication when supplied with
 * a {@link KeyStore keystore} file containg a private key/public certificate pair. 
 * The client secure socket will use the private key to authenticate itself to the target 
 * HTTPS server during the SSL session handshake if requested to do so by the server. 
 * The target HTTPS server will in its turn verify the certificate presented by the client
 * in order to establish client's authenticity
 * </p>
 * 
 * <p>
 * Use the following sequence of actions to generate a keystore file
 * </p>
 *   <ul>
 *     <li>
 *      <p>
 *      Use JDK keytool utility to generate a new key
 *      <pre>keytool -genkey -v -alias "my client key" -validity 365 -keystore my.keystore</pre>
 *      For simplicity use the same password for the key as that of the keystore
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *      Issue a certificate signing request (CSR)
 *      <pre>keytool -certreq -alias "my client key" -file mycertreq.csr -keystore my.keystore</pre>
 *     </p>
 *     </li>
 *     <li>
 *      <p>
 *      Send the certificate request to the trusted Certificate Authority for signature. 
 *      One may choose to act as her own CA and sign the certificate request using a PKI 
 *      tool, such as OpenSSL.
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Import the trusted CA root certificate
 *       <pre>keytool -import -alias "my trusted ca" -file caroot.crt -keystore my.keystore</pre> 
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Import the PKCS#7 file containg the complete certificate chain
 *       <pre>keytool -import -alias "my client key" -file mycert.p7 -keystore my.keystore</pre> 
 *      </p>
 *     </li>
 *     <li>
 *      <p>
 *       Verify the content the resultant keystore file
 *       <pre>keytool -list -v -keystore my.keystore</pre> 
 *      </p>
 *     </li>
 *   </ul>
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 *     <pre>
 *     Protocol authhttps = new Protocol("https",  
 *          new AuthSSLProtocolSocketFactory(
 *              new URL("file:my.keystore"), "mypassword",
 *              new URL("file:my.truststore"), "mypassword"), 443); 
 *
 *     HttpClient client = new HttpClient();
 *     client.getHostConfiguration().setHost("localhost", 443, authhttps);
 *     // use relative url only
 *     GetMethod httpget = new GetMethod("/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 * <p>
 * Example of using custom protocol socket factory per default instead of the standard one:
 *     <pre>
 *     Protocol authhttps = new Protocol("https",  
 *          new AuthSSLProtocolSocketFactory(
 *              new URL("file:my.keystore"), "mypassword",
 *              new URL("file:my.truststore"), "mypassword"), 443); 
 *     Protocol.registerProtocol("https", authhttps);
 *
 *     HttpClient client = new HttpClient();
 *     GetMethod httpget = new GetMethod("https://localhost/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 */

public class AuthSSLProtocolSocketFactory extends AuthSSLProtocolSocketFactoryBase {

    /**
     * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or truststore file
     * must be given. Otherwise SSL context initialization error will result.
     * 
     * @param keystoreUrl URL of the keystore file. May be <tt>null</tt> if HTTPS client
     *        authentication is not to be used.
     * @param keystorePassword Password to unlock the keystore. IMPORTANT: this implementation
     *        assumes that the same password is used to protect the key and the keystore itself.
     * @param truststoreUrl URL of the truststore file. May be <tt>null</tt> if HTTPS server
     *        authentication is not to be used.
     * @param truststorePassword Password to unlock the truststore.
     */
	public AuthSSLProtocolSocketFactory(
			final boolean allowSelfSignedCertificates,
    		final URL keystoreUrl, final String keystorePassword, final String keystoreType, final String keyManagerAlgorithm,
    		final URL truststoreUrl, final String truststorePassword, final String truststoreType, final String trustManagerAlgorithm, final boolean verifyHostname) {
		super(allowSelfSignedCertificates, keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, trustManagerAlgorithm, verifyHostname);
	}
    
    private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password, String algorithm)
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException
    {
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

    private static TrustManager[] createTrustManagers(final KeyStore keystore, String algorithm)
        throws KeyStoreException, NoSuchAlgorithmException
    { 
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
				trustmanagers = new TrustManager[] {
					new AuthSslTrustManager(keystore, trustmanagers)
				};
			}
		} else if (allowSelfSignedCertificates) {
			trustmanagers = new TrustManager[] {
				new AuthSslTrustManager(null, trustmanagers)
			};
		}
		SSLContext sslcontext = SSLContext.getInstance(getProtocol());
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
		return (SSLContext)this.sslContext;
	}

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host,port,clientHost,clientPort);
		verifyHostname(sslSocket);
		return sslSocket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host,port);
		verifyHostname(sslSocket);
		return sslSocket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(socket,host,port,autoClose);
		verifyHostname(sslSocket);
		return sslSocket;
    }

	/**
	 * @see AuthSSLProtocolSocketFactoryBase#createSocket(InetAddress, int)
	 */
	public Socket createSocket(InetAddress adress, int port) throws IOException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(adress, port);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * @see AuthSSLProtocolSocketFactoryBase#createSocket(InetAddress, int, InetAddress, int)
	 */
	public Socket createSocket(InetAddress adress, int port, InetAddress localAdress, int localPort) throws IOException {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(adress, port, localAdress, localPort);
		verifyHostname(sslSocket);
		return sslSocket;
	}

	/**
	 * Attempts to get a new socket connection to the given host within the given time limit.
	 * <p>
	 * This method employs several techniques to circumvent the limitations of older JREs that 
	 * do not support connect timeout. When running in JRE 1.4 or above reflection is used to 
	 * call Socket#connect(SocketAddress endpoint, int timeout) method. When executing in older 
	 * JREs a controller thread is executed. The controller thread attempts to create a new socket
	 * within the given limit of time. If socket constructor does not return until the timeout 
	 * expires, the controller terminates and throws an {@link ConnectTimeoutException}
	 * </p>
	 *  
	 * @param host the host name/IP
	 * @param port the port on the host
	 * @param localAddress the local host name/IP to bind the socket to
	 * @param localPort the port on the local machine
	 * @param params {@link HttpConnectionParams Http connection parameters}
	 * 
	 * @return Socket a new socket
	 * 
	 * @throws IOException if an I/O error occurs while creating the socket
	 * @throws UnknownHostException if the IP address of the host cannot be
	 * determined
	 * 
	 * @author Copied from HttpClient 3.0.1 SSLProtocolSocketFactory
	 * @since 3.0
	 */
	public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		int timeout = params.getConnectionTimeout();
		if (timeout == 0) {
			return createSocket(host, port, localAddress, localPort);
		}
		// To be eventually deprecated when migrated to Java 1.4 or above
		Socket socket = ReflectionSocketFactory.createSocket(
			"javax.net.ssl.SSLSocketFactory", host, port, localAddress, localPort, timeout);
		if (socket == null) {
			socket = ControllerThreadSocketFactory.createSocket(
				this, host, port, localAddress, localPort, timeout);
		}
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
			trustManager = (X509TrustManager)trustmanagers[0];
		}

		public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			trustManager.checkClientTrusted(certificates, authType);
		}

		public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
			if (certificates != null) {
				for (int i = 0; i < certificates.length; i++) {
					X509Certificate certificate = certificates[i];
					certificate.checkValidity();
				}
			}
		}

		public X509Certificate[] getAcceptedIssuers() {
			return trustManager.getAcceptedIssuers();
		}
	}

}

