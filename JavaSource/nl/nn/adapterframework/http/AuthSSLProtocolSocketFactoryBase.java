/*
 * $Log: AuthSSLProtocolSocketFactoryBase.java,v $
 * Revision 1.13  2011-12-20 12:19:34  l190409
 * improved error handling
 *
 * Revision 1.12  2011/11/30 13:52:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.10  2011/06/27 15:52:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set keyManagerAlgorithm and trustManagerAlgorithm
 *
 * Revision 1.9  2007/02/12 13:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.8  2006/08/23 11:23:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * more log-info
 *
 * Revision 1.7  2005/12/19 16:43:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added static method createSocketFactory
 * added createServerSocket-methods
 *
 * Revision 1.6  2005/10/10 14:07:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Add allowSelfSignedCertificates, to easy up testing
 *
 * Revision 1.5  2005/10/07 14:12:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Add a protocol propery, to support TLS besides SSL
 *
 * Revision 1.4  2005/10/04 11:25:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Added two additional createSocket methods to comply with the 
 * SocketFactory createSocket methods
 *
 * Revision 1.3  2005/02/24 12:13:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added follow redirects and truststoretype
 *
 * Revision 1.2  2005/02/02 16:36:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hostname verification, default=false
 *
 * Revision 1.1  2004/10/14 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.net.SocketFactory;
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

public abstract class AuthSSLProtocolSocketFactoryBase implements SocketFactory, SecureProtocolSocketFactory {
	protected static Logger log = LogUtil.getLogger(AuthSSLProtocolSocketFactoryBase.class);;

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
	protected boolean verifyHostname=true;

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
     *
     * @param verifyHostname  The host name verification flag. If set to 
     * 		  <code>true</code> the SSL sessions server host name will be compared
     * 		  to the host name returned in the server certificates "Common Name" 
     * 		  field of the "SubjectDN" entry.  If these names do not match a
     * 		  Exception is thrown to indicate this.  Enabling host name verification 
     * 		  will help to prevent man-in-the-middle attacks.  If set to 
     * 		  <code>false</code> host name verification is turned off.
     */
    public AuthSSLProtocolSocketFactoryBase (
    		final URL keystoreUrl, final String keystorePassword, final String keystoreType, final String keyManagerAlgorithm,
    		final URL truststoreUrl, final String truststorePassword, final String truststoreType, final String trustManagerAlgorithm, final boolean verifyHostname) {
        super();
        this.keystoreUrl = keystoreUrl;
        this.keystorePassword = keystorePassword;
		this.keystoreType = keystoreType;
		this.keyManagerAlgorithm = keyManagerAlgorithm;
        this.truststoreUrl = truststoreUrl;
        this.truststorePassword = truststorePassword;
		this.truststoreType = truststoreType;
		this.trustManagerAlgorithm = trustManagerAlgorithm;
        this.verifyHostname = verifyHostname;
    }

	public static AuthSSLProtocolSocketFactoryBase createSocketFactory(
		final URL certificateUrl, final String certificateAuthAlias, final String certificatePassword, final String certificateType, final String keyManagerAlgorithm, 
		final URL truststoreUrl, final String truststoreAuthAlias, final String truststorePassword, final String truststoreType, final String trustManagerAlgorithm, 
		final boolean verifyHostname, boolean jdk13Compatible)
 			throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException 
 	{

		CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
		CredentialFactory truststoreCf  = new CredentialFactory(truststoreAuthAlias,  null, truststorePassword);

		AuthSSLProtocolSocketFactoryBase factory;
		if (jdk13Compatible) {
			addProvider("sun.security.provider.Sun");
			addProvider("com.sun.net.ssl.internal.ssl.Provider");
			System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
			factory =
				new AuthSSLProtocolSocketFactoryForJsse10x(
					certificateUrl,
					certificatePassword,
					certificateType,
					keyManagerAlgorithm,
					truststoreUrl,
					certificateCf.getPassword(),
					truststoreType,
					trustManagerAlgorithm,
					verifyHostname);
		}
		else {
			factory =
				new AuthSSLProtocolSocketFactory(
					certificateUrl,
					certificatePassword,
					certificateType,
					keyManagerAlgorithm,
					truststoreUrl,
					truststoreCf.getPassword(),
					truststoreType,
					trustManagerAlgorithm,
					verifyHostname);
		}

		return factory;
	}

	protected static void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			java.security.Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error("cannot add provider ["+name+"], "+t.getClass().getName(),t);
		}
	}


	public abstract void initSSLContext() throws NoSuchAlgorithmException, KeyStoreException, GeneralSecurityException, IOException;

	protected void initSSLContextNoExceptions(){
		if (this.sslContext == null) {
			try {
				initSSLContext();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("Unsupported algorithm exception",e);
			} catch (KeyStoreException e) {
				throw new RuntimeException("Keystore exception",e);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException("Key management exception",e);
			} catch (IOException e) {
				throw new RuntimeException("I/O error reading keystore/truststore file",e);
			}
		}
	}

	// http://publib.boulder.ibm.com/infocenter/wasinfo/v6r1/index.jsp?topic=/com.ibm.websphere.express.doc/info/exp/ae/csec_sslkeystoreconfs.html
//	private static KeyStore getWebsphereKeyStore() {
//		 com.ibm.websphere.ssl.JSSEHelper jsseHelper = com.ibm.websphere.ssl.JSSEHelper.getInstance();
//		try {
//			String alias = "NodeAServer1SSLSettings";   // As specified in the WebSphere SSL configuration
//			Properties sslProps = jsseHelper.getProperties(alias); 
//		 } catch (SSLException e) {
//			 e.printStackTrace();
//		 }
//	}

    protected static KeyStore createKeyStore(final URL url, final String password, String keyStoreType, String prefix) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        if (url == null) {
            throw new IllegalArgumentException("Keystore url for "+prefix+" may not be null");
        }
        log.info("Initializing keystore for "+prefix+" from "+url.toString());
        KeyStore keystore  = KeyStore.getInstance(keyStoreType);
        keystore.load(url.openStream(), password != null ? password.toCharArray(): null);
		if (log.isInfoEnabled()) {
			Enumeration aliases = keystore.aliases();
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
	 * Describe <code>verifyHostname</code> method here.
	 *
	 * @param socket a <code>SSLSocket</code> value
	 * @exception SSLPeerUnverifiedException  If there are problems obtaining
	 * the server certificates from the SSL session, or the server host name 
	 * does not match with the "Common Name" in the server certificates 
	 * SubjectDN.
	 * @exception UnknownHostException  If we are not able to resolve
	 * the SSL sessions returned server host name. 
	 */
	protected void verifyHostname(SSLSocket socket) 
		throws SSLPeerUnverifiedException, UnknownHostException {
		if (! verifyHostname) 
			return;

		SSLSession session = socket.getSession();
		if (session==null) {
			throw new UnknownHostException("could not obtain session from socket");
		}
		String hostname = session.getPeerHost();
		try {
			InetAddress.getByName(hostname);
		} catch (UnknownHostException uhe) {
			String msg = "Could not resolve SSL sessions server hostname: " + hostname;
			// Under WebSphere, hostname can be equal to proxy-hostname
			log.warn(msg,uhe);
//			throw new UnknownHostException(msg);
		}

		javax.security.cert.X509Certificate[] certs = session.getPeerCertificateChain();
		if (certs == null || certs.length == 0) 
			throw new SSLPeerUnverifiedException("No server certificates found!");
        
		//get the servers DN in its string representation
		String dn = certs[0].getSubjectDN().getName();

		//might be useful to print out all certificates we receive from the
		//server, in case one has to debug a problem with the installed certs.
		if (log.isInfoEnabled()) {
			log.info("Server certificate chain:");
			for (int i = 0; i < certs.length; i++) {
				log.info("X509Certificate[" + i + "]=" + certs[i]);
			}
		}
		//get the common name from the first cert
		String cn = getCN(dn);
		if (hostname.equalsIgnoreCase(cn)) {
			if (log.isInfoEnabled()) {
				log.info("Target hostname valid: " + cn);
			}
		} else {
			throw new SSLPeerUnverifiedException(
				"HTTPS hostname invalid: expected '" + hostname + "', received '" + cn + "'");
		}
	}

    
	/**
	 * Parses a X.500 distinguished name for the value of the 
	 * "Common Name" field.
	 * This is done a bit sloppy right now and should probably be done a bit
	 * more according to <code>RFC 2253</code>.
	 *
	 * @param dn  a X.500 distinguished name.
	 * @return the value of the "Common Name" field.
	 */
	protected String getCN(String dn) {
		int i = 0;
		i = dn.indexOf("CN=");
		if (i == -1) {
			return null;
		}
		//get the remaining DN without CN=
		dn = dn.substring(i + 3);  
		// System.out.println("dn=" + dn);
		char[] dncs = dn.toCharArray();
		for (i = 0; i < dncs.length; i++) {
			if (dncs[i] == ','  && i > 0 && dncs[i - 1] != '\\') {
				break;
			}
		}
		return dn.substring(0, i);
	}

	/**
	 * Added to completely implement the SocketFactory interface 
	 * @param adress
	 * @param port
	 * @return
	 * @throws IOException
	 */
	public abstract Socket createSocket(InetAddress adress, int port) throws IOException;

	public abstract Socket createSocket(InetAddress adress, int port, InetAddress localAdress, int localPort) throws IOException;

	/* (non-Javadoc)
	 * These three methods are included to provide compatibility with org.apache.commons.net.SocketFactory.
	 * The created sockets are probably not secured
	 * //TODO: find out if these serversockets need to be secured.
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int)
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int, int)
	 * @see org.apache.commons.net.SocketFactory#createServerSocket(int, int, java.net.InetAddress)
	 */
	public ServerSocket createServerSocket(int port) throws IOException {
		return new ServerSocket(port);
	}
	public ServerSocket createServerSocket(int port, int backlog) throws IOException {
		return new ServerSocket(port, backlog);
	}
	public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
		return new ServerSocket(port, backlog, bindAddr);
	}


	/**
	 * Properties
	 */	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String string) {
		protocol = string;
	}
	
	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	public void setAllowSelfSignedCertificates(boolean b) {
		allowSelfSignedCertificates = b;
	}

}

