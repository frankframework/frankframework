/*
 * $Log: AuthSSLProtocolSocketFactoryForJsse10x.java,v $
 * Revision 1.5  2005-02-24 12:13:14  L190409
 * added follow redirects and truststoretype
 *
 * Revision 1.4  2005/02/02 16:36:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hostname verification, default=false
 *
 * Revision 1.3  2004/12/23 12:06:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * explicit SSLContect provider
 *
 * Revision 1.2  2004/10/14 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * refactored AuthSSLProtocolSocketFactory group
 *
 * Revision 1.1  2004/09/09 14:50:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import javax.net.ssl.SSLSocket;


import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;

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
        final URL keystoreUrl, final String keystorePassword, final String keystoreType, 
        final URL truststoreUrl, final String truststorePassword, final String truststoreType, final boolean verifyHostname)
    {
        super(keystoreUrl, keystorePassword, keystoreType, truststoreUrl, truststorePassword, truststoreType, verifyHostname);
    }
    
    private static KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException 
    {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        log.debug("Initializing key manager");
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, password != null ? password.toCharArray(): null);
        return kmfactory.getKeyManagers(); 
    }

    private static TrustManager[] createTrustManagers(final KeyStore keystore)
        throws KeyStoreException, NoSuchAlgorithmException
    { 
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        log.debug("Initializing trust manager");
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers();
        return trustmanagers; 
    }


    private SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
            KeyManager[] keymanagers = null;
            TrustManager[] trustmanagers = null;
            if (this.keystoreUrl != null) {
                KeyStore keystore = createKeyStore(this.keystoreUrl, this.keystorePassword, this.keystoreType, "Certificate chain");
                keymanagers = createKeyManagers(keystore, this.keystorePassword);
            }
            if (this.truststoreUrl != null) {
                KeyStore keystore = createKeyStore(this.truststoreUrl, this.truststorePassword, this.truststoreType, "Trusted Certificate");
                trustmanagers = createTrustManagers(keystore);
            }
            SSLContext sslcontext = SSLContext.getInstance("SSL",new com.sun.net.ssl.internal.ssl.Provider());
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
    public Socket createSocket(
        String host,
        int port,
        InetAddress clientHost,
        int clientPort)
        throws IOException, UnknownHostException
   {
	SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host,port,clientHost,clientPort);
	verifyHostname(sslSocket);
	return sslSocket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host,port);
		verifyHostname(sslSocket);
		return sslSocket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    public Socket createSocket(
        Socket socket,
        String host,
        int port,
        boolean autoClose)
        throws IOException, UnknownHostException
    {
		SSLSocket sslSocket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(socket,host,port,autoClose);
		verifyHostname(sslSocket);
		return sslSocket;
    }



}

