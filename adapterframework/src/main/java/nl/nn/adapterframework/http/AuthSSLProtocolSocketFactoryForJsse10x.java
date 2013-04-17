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
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocket;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.ReflectionSocketFactory;
import org.apache.commons.lang.StringUtils;

import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.X509TrustManager;

// javax.net.ssl is jdk 1.4.x ...
//import javax.net.ssl.KeyManager;
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.TrustManagerFactory;

/**
 * <p>
 * JDK 1.3 / JSSE 1.0.x compatible implementation of {@link AuthSSLProtocolSocketFactory}.
 * 
 */

public class AuthSSLProtocolSocketFactoryForJsse10x extends AuthSSLProtocolSocketFactoryBase {

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
	public AuthSSLProtocolSocketFactoryForJsse10x(
			final URL keystoreUrl, final String keystorePassword, final String keystoreType, final String keyManagerAlgorithm,
			final URL truststoreUrl, final String truststorePassword, final String truststoreType, final String trustManagerAlgorithm,
			final boolean allowSelfSignedCertificates, final boolean verifyHostname) {
		super(keystoreUrl, keystorePassword, keystoreType, keyManagerAlgorithm, truststoreUrl, truststorePassword, truststoreType, trustManagerAlgorithm, allowSelfSignedCertificates, verifyHostname);
		addProvider("sun.security.provider.Sun");
		addProvider("com.sun.net.ssl.internal.ssl.Provider");
		System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
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
		}
		SSLContext sslcontext = SSLContext.getInstance(getProtocol(), new com.sun.net.ssl.internal.ssl.Provider());
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

		public boolean isClientTrusted(X509Certificate[] certs) {
			return trustManager.isClientTrusted(certs);
		}

		public boolean isServerTrusted(X509Certificate[] certs) {
			if (certs != null) {
				for (int i = 0; i < certs.length; i++) {
					X509Certificate certificate = certs[i];
					try {
						certificate.checkValidity();
					}
					catch(Exception e) {
						log.debug("Exception checking certificate validity, assuming server not trusted",e);
						return false;
					}
				}
			}
			return true;
		}

		public X509Certificate[] getAcceptedIssuers() {
			return trustManager.getAcceptedIssuers();
		}
	}

}

