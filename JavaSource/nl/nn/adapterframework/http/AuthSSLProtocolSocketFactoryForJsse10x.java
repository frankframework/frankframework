/*
 * $Log: AuthSSLProtocolSocketFactoryForJsse10x.java,v $
 * Revision 1.1  2004-09-09 14:50:07  L190409
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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

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

public class AuthSSLProtocolSocketFactoryForJsse10x extends AuthSSLProtocolSocketFactory {

    private SSLContext sslcontext = null;

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
        final URL truststoreUrl, final String truststorePassword)
    {
        super(keystoreUrl, keystorePassword, keystoreType, truststoreUrl, truststorePassword);
    }

    private static KeyStore createKeyStore(final URL url, final String password, String keyStoreType) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        if (url == null) {
            throw new IllegalArgumentException("Keystore url may not be null");
        }
        log.debug("Initializing key store");
        KeyStore keystore  = KeyStore.getInstance(keyStoreType);
        keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
        return keystore;
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


	public void init() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
		this.sslcontext = createSSLContext();
	}

    private SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException {
            KeyManager[] keymanagers = null;
            TrustManager[] trustmanagers = null;
            if (this.keystoreUrl != null) {
                KeyStore keystore = createKeyStore(this.keystoreUrl, this.keystorePassword, this.keystoreType);
                if (log.isDebugEnabled()) {
                    Enumeration aliases = keystore.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = (String)aliases.nextElement();                        
                        Certificate[] certs = keystore.getCertificateChain(alias);
                        if (certs != null) {
                            log.debug("Certificate chain '" + alias + "':");
                            for (int c = 0; c < certs.length; c++) {
                                if (certs[c] instanceof X509Certificate) {
                                    X509Certificate cert = (X509Certificate)certs[c];
                                    log.debug(" Certificate " + (c + 1) + ":");
                                    log.debug("  Subject DN: " + cert.getSubjectDN());
                                    log.debug("  Signature Algorithm: " + cert.getSigAlgName());
                                    log.debug("  Valid from: " + cert.getNotBefore() );
                                    log.debug("  Valid until: " + cert.getNotAfter());
                                    log.debug("  Issuer: " + cert.getIssuerDN());
                                }
                            }
                        }
                    }
                }
                keymanagers = createKeyManagers(keystore, this.keystorePassword);
            }
            if (this.truststoreUrl != null) {
                KeyStore keystore = createKeyStore(this.truststoreUrl, this.truststorePassword, this.keystoreType);
                if (log.isDebugEnabled()) {
                    Enumeration aliases = keystore.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = (String)aliases.nextElement();
                        log.debug("Trusted certificate '" + alias + "':");
                        Certificate trustedcert = keystore.getCertificate(alias);
                        if (trustedcert != null && trustedcert instanceof X509Certificate) {
                            X509Certificate cert = (X509Certificate)trustedcert;
                            log.debug("  Subject DN: " + cert.getSubjectDN());
                            log.debug("  Signature Algorithm: " + cert.getSigAlgName());
                            log.debug("  Valid from: " + cert.getNotBefore() );
                            log.debug("  Valid until: " + cert.getNotAfter());
                            log.debug("  Issuer: " + cert.getIssuerDN());
                        }
                    }
                }
                trustmanagers = createTrustManagers(keystore);
            }
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(keymanagers, trustmanagers, null);
            return sslcontext;

    }

    private SSLContext getSSLContext() {
        if (this.sslcontext == null) {
			try {
				init();
			} catch (NoSuchAlgorithmException e) {
				log.error("Unsupported algorithm exception", e);
				throw new Error("Unsupported algorithm exception: " + e.getMessage());
			} catch (KeyStoreException e) {
				log.error("Keystore exception", e);
				throw new Error("Keystore exception: " + e.getMessage());
			} catch (GeneralSecurityException e) {
				log.error("Key management exception", e);
				throw new Error("Key management exception: " + e.getMessage());
			} catch (IOException e) {
				log.error("I/O error reading keystore/truststore file", e);
				throw new Error("I/O error reading keystore/truststore file: " + e.getMessage());
			}
        }
        return this.sslcontext;
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
       return getSSLContext().getSocketFactory().createSocket(
            host,
            port,
            clientHost,
            clientPort
        );
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port)
        throws IOException, UnknownHostException
    {
        return getSSLContext().getSocketFactory().createSocket(
            host,
            port
        );
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
        return getSSLContext().getSocketFactory().createSocket(
            socket,
            host,
            port,
            autoClose
        );
    }
}

