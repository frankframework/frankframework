/*
 * $Log: AuthSSLProtocolSocketFactoryBase.java,v $
 * Revision 1.1  2004-10-14 15:35:10  L190409
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
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

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

public abstract class AuthSSLProtocolSocketFactoryBase implements SecureProtocolSocketFactory {

    /** Log object for this class. */
	protected static Logger log = Logger.getLogger(AuthSSLProtocolSocketFactoryBase.class);;

	protected URL keystoreUrl = null;
	protected String keystorePassword = null;
	protected String keystoreType = "null";
	protected URL truststoreUrl = null;
	protected String truststorePassword = null;
	protected Object sslContext = null;

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
    public AuthSSLProtocolSocketFactoryBase (
        final URL keystoreUrl, final String keystorePassword, final String keystoreType, 
        final URL truststoreUrl, final String truststorePassword)
    {
        super();
        this.keystoreUrl = keystoreUrl;
        this.keystorePassword = keystorePassword;
		this.keystoreType = keystoreType;
        this.truststoreUrl = truststoreUrl;
        this.truststorePassword = truststorePassword;
    }

	public abstract void initSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException;

	protected void initSSLContextNoExceptions(){
		if (this.sslContext == null) {
			try {
				initSSLContext();
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
	}

    protected static KeyStore createKeyStore(final URL url, final String password, String keyStoreType, String prefix) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        if (url == null) {
            throw new IllegalArgumentException("Keystore url may not be null");
        }
        log.debug("Initializing key store");
        KeyStore keystore  = KeyStore.getInstance(keyStoreType);
        keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
		if (log.isDebugEnabled()) {
			Enumeration aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				String alias = (String)aliases.nextElement();
				log.debug(prefix+" '" + alias + "':");
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
        return keystore;
    }
    
}

