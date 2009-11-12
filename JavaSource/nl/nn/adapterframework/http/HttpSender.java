/*
 * $Log: HttpSender.java,v $
 * Revision 1.38  2009-11-12 13:50:03  L190409
 * added extra timeout setting
 * abort method on IOException
 *
 * Revision 1.37  2009/08/26 11:47:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * upgrade to HttpClient 3.0.1 - including idle connection cleanup
 *
 * Revision 1.36  2009/03/31 08:21:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix in maxExecuteRetries and reduce the default maxRetries to 1
 *
 * Revision 1.35  2008/08/14 14:52:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased default maxConnections to 10
 *
 * Revision 1.34  2008/08/12 15:34:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * maxConnections must be positive
 *
 * Revision 1.33  2008/05/21 08:42:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * content-type configurable
 *
 * Revision 1.32  2008/03/20 12:00:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default path '/'
 *
 * Revision 1.31  2007/12/28 12:09:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added timeout exception detection
 *
 * Revision 1.30  2007/10/03 08:46:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added link to IBM site with JDK policy files
 *
 * Revision 1.29  2007/02/21 15:59:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove debug message
 *
 * Revision 1.28  2007/02/05 15:16:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made number of connection- and execution retries configurable
 *
 * Revision 1.27  2006/08/24 11:01:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retries instead of attempts
 *
 * Revision 1.26  2006/08/23 11:24:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retry when method fails
 *
 * Revision 1.25  2006/08/21 07:56:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return of the IbisMultiThreadedConnectionManager
 *
 * Revision 1.24  2006/07/17 09:02:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typos in documentation
 *
 * Revision 1.23  2006/06/14 09:40:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.22  2006/05/03 07:09:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed null pointer exception that occured when no statusline was found
 *
 * Revision 1.21  2006/01/23 12:57:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * determine port-default if not found from uri
 *
 * Revision 1.20  2006/01/19 12:14:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected logging output, improved javadoc
 *
 * Revision 1.19  2006/01/05 14:22:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * POST method now appends parameters to body instead of header
 *
 * Revision 1.18  2005/12/28 08:40:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.17  2005/12/19 16:42:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added authentication using authentication-alias
 *
 * Revision 1.16  2005/10/18 07:06:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging config, based now on ISenderWithParametersBase
 *
 * Revision 1.15  2005/10/03 13:19:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced IbisMultiThreadedConnectionManager with original MultiThreadedConnectionMananger
 *
 * Revision 1.14  2005/02/24 12:13:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added follow redirects and truststoretype
 *
 * Revision 1.13  2005/02/02 16:36:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hostname verification, default=false
 *
 * Revision 1.12  2004/12/23 16:11:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Explicit check for open connections
 *
 * Revision 1.11  2004/12/23 12:12:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * staleChecking optional
 *
 * Revision 1.10  2004/10/19 06:39:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.9  2004/10/14 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * refactored AuthSSLProtocolSocketFactory group
 *
 * Revision 1.8  2004/10/12 15:10:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made parameterized version
 *
 * Revision 1.7  2004/09/09 14:50:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added JDK1.3.x compatibility
 *
 * Revision 1.6  2004/09/08 14:18:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * early initialization of SocketFactory
 *
 * Revision 1.5  2004/09/01 12:24:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved fault handling
 *
 * Revision 1.4  2004/08/31 15:51:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added extractResult method
 *
 * Revision 1.3  2004/08/31 10:13:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added security handling
 *
 * Revision 1.2  2004/08/24 11:41:27  unknown <unknown@ibissource.org>
 * Remove warnings
 *
 * Revision 1.1  2004/08/20 13:04:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Security;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

/**
 * Sender that gets information via a HTTP using POST or GET.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.HttpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMethodType(String) methodType}</td><td>type of method to be executed, either 'GET' or 'POST'</td><td>GET</td></tr>
 * <tr><td>{@link #setContentType(String) contentType}</td><td>conent-type of the request, only for POST methods</td><td>text/html; charset=UTF-8</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout in ms of obtaining a connection/result. 0 means no timeout</td><td>10000</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>the maximum number of concurrent connections</td><td>2</td></tr>
 * <tr><td>{@link #setMaxConnectionRetries(int) maxConnectionRetries}</td><td>the maximum number of times it is retried to obtain a connection</td><td>1</td></tr>
 * <tr><td>{@link #setMaxExecuteRetries(int) maxExecuteRetries}</td><td>the maximum number of times it the execution is retried</td><td>1</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyRealm(String) proxyRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>&nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setFollowRedirects(boolean) followRedirects}</td><td>when true, a redirect request will be honoured, e.g. to switch to https</td><td>true</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setStaleChecking(boolean) staleChecking}</td><td>controls whether connections checked to be stale, i.e. appear open, but are not.</td><td>true</td></tr>
 * <tr><td>{@link #setEncodeMessages(boolean) encodeMessages}</td><td>specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Parameters:</b></p>
 * <p>Any parameters present are appended to the request as request-parameters</p>
 * 
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this</p>
 * <pre>
 *   param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST methods expect a message similar as GET, or looking like this</p>
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * <p>
 * Note 1:
 * Some certificates require the &lt;java_home&gt;/jre/lib/security/xxx_policy.jar files to be upgraded to unlimited strength. Typically, in such a case, an error message like 
 * <code>Error in loading the keystore: Private key decryption error: (java.lang.SecurityException: Unsupported keysize or algorithm parameters</code> is observed.
 * For IBM JDKs these files can be downloaded from http://www.ibm.com/developerworks/java/jdk/security/50/ (scroll down to 'IBM SDK Policy files')
 * </p>
 * <p>
 * Note 2:
 * To debug ssl-related problems, set the following system property:
 * <ul>
 * <li>IBM / WebSphere: <code>-Djavax.net.debug=true</code></li>
 * <li>SUN: <code>-Djavax.net.debug=all</code></li>
 * </ul>
 * </p>
 * <p>
 * Note 3:
 * In case <code>javax.net.ssl.SSLHandshakeException: unknown certificate</code>-exceptions are thrown, 
 * probably the certificate of the other party is not trusted. Try to use one of the certificates in the path as your truststore by doing the following:
 * <ul>
 *   <li>open the URL you are trying to reach in InternetExplorer</li>
 *   <li>click on the yellow padlock on the right in the bottom-bar. This opens the certificate information window</li>
 *   <li>click on tab 'Certificeringspad'</li>
 *   <li>double click on root certificate in the tree displayed. This opens the certificate information window for the root certificate</li>
 *   <li>click on tab 'Details'</li>
 *   <li>click on 'Kopieren naar bestand'</li>
 *   <li>click 'next', choose 'DER Encoded Binary X.509 (.CER)'</li>
 *   <li>click 'next', choose a filename</li>
 *   <li>click 'next' and 'finish'</li>
 * 	 <li>Start IBM key management tool ikeyman.bat, located in Program Files/IBM/WebSphere Studio/Application Developer/v5.1.2/runtimes/base_v51/bin (or similar)</li>
 *   <li>create a new key-database (Sleuteldatabase -> Nieuw...), or open the default key.jks (default password="changeit")</li>
 *   <li>add the generated certificate (Toevoegen...)</li>
 *   <li>store the key-database in JKS format</li>
 *   <li>if you didn't use the standard keydatabase, then reference the file in the truststore-attribute in Configuration.xml (include the file as a resource)</li>
 *   <li>use jks for the truststoreType-attribute</li>
 *   <li>restart your application</li>
 *   <li>instead of IBM ikeyman you can use the standard java tool <code>keytool</code> as follows: 
 *      <code>keytool -import -alias <i>yourAlias</i> -file <i>pathToSavedCertificate</i></code></li>
 * </ul>
 *  
 * </p>
 * @author Gerrit van Brakel
 * @since 4.2c
 */
public class HttpSender extends SenderWithParametersBase implements HasPhysicalDestination {
	public static final String version = "$RCSfile: HttpSender.java,v $ $Revision: 1.38 $ $Date: 2009-11-12 13:50:03 $";

	private String url;
	private String methodType="GET"; // GET or POST
	private String contentType="text/html; charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING;

	private int timeout=10000;
	private int maxConnections=10;
	
	private int maxConnectionRetries=1;
	private int maxExecuteRetries=1;

	private String authAlias;
	private String userName;
	private String password;

	private String proxyHost;
	private int    proxyPort=80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm=null;

	private String keystoreType="pkcs12";
	private String certificate;
	private String certificateAuthAlias;
	private String certificatePassword;
	private String truststore=null;
	private String truststorePassword=null;
	private String truststoreAuthAlias;
	private String truststoreType="jks";
	
	private boolean verifyHostname=true;
	private boolean followRedirects=true;
	private boolean jdk13Compatibility=false;
	private boolean staleChecking=true;
	private boolean encodeMessages=false;

	protected URI uri;
	private MultiThreadedHttpConnectionManager connectionManager;
	protected HttpClient httpclient;

//	/*
//	 * connection manager that checks connections to be open before handing them to HttpMethod.
//	 * Use of this connection manager prevents SocketExceptions to occur.
//	 * 
//	 */
//	private class IbisMultiThreadedHttpConnectionManager extends MultiThreadedHttpConnectionManager {
//		
//		protected boolean checkConnection(HttpConnection connection)  {
//			boolean status = connection.isOpen();
//			//log.debug(getLogPrefix()+"IbisMultiThreadedHttpConnectionManager connection open ["+status+"]");
//			if (status) {
//				try {
//					connection.setSoTimeout(connection.getSoTimeout());
//				} catch (SocketException e) {
//					log.warn(getLogPrefix()+"IbisMultiThreadedHttpConnectionManager SocketException while checking: "+ e.getMessage());
//					connection.close();
//					return false;
//				} catch (IllegalStateException e) {
//					log.warn(getLogPrefix()+"IbisMultiThreadedHttpConnectionManager IllegalStateException while checking: "+ e.getMessage());
//					connection.close();
//					return false;
//				}
//			}
//			return true;
//		}
//				
//		public HttpConnection getConnection(HostConfiguration hostConfiguration) {
//			//log.debug(getLogPrefix()+"IbisMultiThreadedHttpConnectionManager getConnection(HostConfiguration)");
//			HttpConnection result = super.getConnection(hostConfiguration);			
//			int count=getMaxConnectionRetries();
//			while (count-->0 && !checkConnection(result)) {
//				log.info("releasing failed connection, connectionRetries left ["+count+"]");
//				releaseConnection(result);
//				result= super.getConnection(hostConfiguration);
//			} 
//			return result;
//		}
//		public HttpConnection getConnection(HostConfiguration hostConfiguration, long timeout) throws HttpException {
//			//log.debug(getLogPrefix()+"IbisMultiThreadedHttpConnectionManager getConnection(HostConfiguration, timeout["+timeout+"])");
//			HttpConnection result = super.getConnection(hostConfiguration, timeout);
//			int count=getMaxConnectionRetries();
//			while (count-->0 && !checkConnection(result)) {
//				log.info("releasing failed connection, connectionRetries left ["+count+"]");
//				releaseConnection(result);
//				result= super.getConnection(hostConfiguration, timeout);
//			} 
//			return result;
//		}
//	}

	protected void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error(getLogPrefix()+"cannot add provider ["+name+"], "+t.getClass().getName()+": "+t.getMessage());
		}
	}

	
	public void configure() throws ConfigurationException {
		super.configure();
//		System.setProperty("javax.net.debug","all"); // normaal Java
//		System.setProperty("javax.net.debug","true"); // IBM java
		httpclient = new HttpClient();
		httpclient.setTimeout(getTimeout());
		httpclient.setConnectionTimeout(getTimeout());
		httpclient.setHttpConnectionFactoryTimeout(getTimeout());
		
		if (paramList!=null) {
			paramList.configure();
		}
		if (StringUtils.isEmpty(getUrl())) {
			throw new ConfigurationException(getLogPrefix()+"Url must be specified");
		}
		if (getMaxConnections()<=0) {
			throw new ConfigurationException(getLogPrefix()+"maxConnections is set to ["+getMaxConnections()+"], which is not enough for adequate operation");
		}
		try {
			uri = new URI(getUrl());

			int port = uri.getPort();
			if (port<1) {
				try {
					log.debug(getLogPrefix()+"looking up protocol for scheme ["+uri.getScheme()+"]");
					port = Protocol.getProtocol(uri.getScheme()).getDefaultPort();
				} catch (IllegalStateException e) {
					log.debug(getLogPrefix()+"protocol for scheme ["+uri.getScheme()+"] not found, setting port to 80",e);
					port=80; 
				}
			}
			if (uri.getPath()==null) {
				uri.setPath("/");
			}

			log.info(getLogPrefix()+"created uri: scheme=["+uri.getScheme()+"] host=["+uri.getHost()+"] port=["+port+"] path=["+uri.getPath()+"]");

			URL certificateUrl=null;
			URL truststoreUrl=null;
	
			if (!StringUtils.isEmpty(getCertificate())) {
				certificateUrl = ClassUtils.getResourceURL(this, getCertificate());
				if (certificateUrl==null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for certificate resource ["+getCertificate()+"]");
				}
				log.info(getLogPrefix()+"resolved certificate-URL to ["+certificateUrl.toString()+"]");
			}
			if (!StringUtils.isEmpty(getTruststore())) {
				truststoreUrl = ClassUtils.getResourceURL(this, getTruststore());
				if (truststoreUrl==null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for truststore resource ["+getTruststore()+"]");
				}
				log.info(getLogPrefix()+"resolved truststore-URL to ["+truststoreUrl.toString()+"]");
			}

			HostConfiguration hostconfiguration = httpclient.getHostConfiguration();		           
			
			if (certificateUrl!=null || truststoreUrl!=null) {
				AuthSSLProtocolSocketFactoryBase socketfactory ;
				try {
					CredentialFactory certificateCf = new CredentialFactory(getCertificateAuthAlias(), null, getCertificatePassword());
					CredentialFactory truststoreCf  = new CredentialFactory(getTruststoreAuthAlias(),  null, getTruststorePassword());
					if (isJdk13Compatibility()) {
						addProvider("sun.security.provider.Sun");
						addProvider("com.sun.net.ssl.internal.ssl.Provider");
						System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
						socketfactory = new AuthSSLProtocolSocketFactoryForJsse10x(
							certificateUrl, certificateCf.getPassword(), getKeystoreType(),
							truststoreUrl,  truststoreCf.getPassword(),  getTruststoreType(), isVerifyHostname());
					} else {
						socketfactory = new AuthSSLProtocolSocketFactory(
							certificateUrl, certificateCf.getPassword(), getKeystoreType(),
							truststoreUrl,  truststoreCf.getPassword(),  getTruststoreType(),isVerifyHostname());
					}
					socketfactory.initSSLContext();	
				} catch (Throwable t) {
					throw new ConfigurationException(getLogPrefix()+"cannot create or initialize SocketFactory",t);
				}
				Protocol authhttps = new Protocol(uri.getScheme(), socketfactory, port);
				hostconfiguration.setHost(uri.getHost(),port,authhttps);
			} else {
				hostconfiguration.setHost(uri.getHost(),port,uri.getScheme());
			}
			log.info(getLogPrefix()+"configured httpclient for host ["+hostconfiguration.getHostURL()+"]");
			
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
			if (!StringUtils.isEmpty(cf.getUsername())) {
				httpclient.getState().setAuthenticationPreemptive(true);
				Credentials defaultcreds = new UsernamePasswordCredentials(cf.getUsername(), cf.getPassword());
				httpclient.getState().setCredentials(null, uri.getHost(), defaultcreds);
			}
			if (!StringUtils.isEmpty(getProxyHost())) {
				CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), getProxyUserName(), getProxyPassword());
				httpclient.getHostConfiguration().setProxy(getProxyHost(), getProxyPort());
				httpclient.getState().setProxyCredentials(getProxyRealm(), getProxyHost(),
				new UsernamePasswordCredentials(pcf.getUsername(), pcf.getPassword()));
			}
	

		} catch (URIException e) {
			throw new ConfigurationException(getLogPrefix()+"cannot interprete uri ["+getUrl()+"]");
		}

	}

	public void open() {
//		connectionManager = new IbisMultiThreadedHttpConnectionManager();
		connectionManager = new HttpConnectionManager(0,getName());
		connectionManager.setMaxConnectionsPerHost(getMaxConnections());
		log.debug(getLogPrefix()+"set up connectionManager, stale checking ["+connectionManager.isConnectionStaleCheckingEnabled()+"]");
		if (connectionManager.isConnectionStaleCheckingEnabled() != isStaleChecking()) {
			log.info(getLogPrefix()+"set up connectionManager, setting stale checking ["+isStaleChecking()+"]");
			connectionManager.setConnectionStaleCheckingEnabled(isStaleChecking());
		}
		httpclient.setHttpConnectionManager(connectionManager);
	}

	public void close() {
		connectionManager.shutdown();
		connectionManager=null;
	}

	public boolean isSynchronous() {
		return true;
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuffer path, ParameterValueList parameters) {
		if (parameters!=null) {
			log.debug(getLogPrefix()+"appending ["+parameters.size()+"] parameters");
		}
		for(int i=0; i<parameters.size(); i++) {
			if (parametersAppended) {
				path.append("&");
			} else {
				path.append("?");
				parametersAppended = true;
			}
			ParameterValue pv = parameters.getParameterValue(i);
			String parameterToAppend=pv.getDefinition().getName()+"="+URLEncoder.encode(pv.asStringValue(""));
			log.debug(getLogPrefix()+"appending parameter ["+parameterToAppend+"]");
			path.append(parameterToAppend);
		}
		return parametersAppended;
	}

	protected HttpMethod getMethod(String message, ParameterValueList parameters) throws SenderException {
		try { 
			boolean queryParametersAppended = false;
			if (isEncodeMessages()) {
				message = URLEncoder.encode(message);
			}
			StringBuffer path = new StringBuffer(uri.getPath());
			if (!StringUtils.isEmpty(uri.getQuery())) {
				path.append("?"+uri.getQuery());
				queryParametersAppended = true;
			}
			
			if (getMethodType().equals("GET")) {
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,path,parameters);
					log.debug(getLogPrefix()+"path after appending of parameters ["+path.toString()+"]");
				}
				GetMethod result = new GetMethod(path+(parameters==null? message:""));
				log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+result.getQueryString()+"]");
				return result;
			} else {
				if (getMethodType().equals("POST")) {
					PostMethod postMethod = new PostMethod(path.toString());
					if (StringUtils.isNotEmpty(getContentType())) {
						postMethod.setRequestHeader("Content-Type",getContentType());
					}
					if (parameters!=null) {
						StringBuffer msg = new StringBuffer(message);
						appendParameters(true,msg,parameters);
						if (StringUtils.isEmpty(message) && msg.length()>1) {
							message=msg.substring(1);
						} else {
							message=msg.toString();
						}
					}
					postMethod.setRequestBody(message);
				
					return postMethod;
				} else {
					throw new SenderException("unknown methodtype ["+getMethodType()+"], must be either POST or GET");
				}
			}
		} catch (URIException e) {
			throw new SenderException(getLogPrefix()+"cannot find path from url ["+getUrl()+"]", e);
		}

	}
	
	public String extractResult(HttpMethod httpmethod) throws SenderException, IOException {
		int statusCode = httpmethod.getStatusCode();
		if (statusCode!=200) {
			throw new SenderException(getLogPrefix()+"httpstatus "+statusCode+": "+httpmethod.getStatusText());
		}
		return httpmethod.getResponseBodyAsString();
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		try {
			if (prc !=null && paramList !=null) {
				pvl=prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}
		HttpMethod httpmethod=getMethod(message, pvl);
		if (!"POST".equals(getMethodType())) {
			httpmethod.setFollowRedirects(isFollowRedirects());
		}
		
		String result = null;
		int statusCode = -1;
		int count=getMaxExecuteRetries();
		String msg = null;
		try {
			while (count-->=0 && statusCode==-1) {
				try {
					log.debug(getLogPrefix()+"executing method");
					statusCode = httpclient.executeMethod(httpmethod);
					log.debug(getLogPrefix()+"executed method");
					if (log.isDebugEnabled()) {
						StatusLine statusline = httpmethod.getStatusLine();
						if (statusline!=null) { 
							log.debug(getLogPrefix()+"status:"+statusline.toString());
						} else {
							log.debug(getLogPrefix()+"no statusline found");
						}
					}
					result = extractResult(httpmethod);	
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix()+"retrieved result ["+result+"]");
					}
				} catch (HttpException e) {
					Throwable throwable = e.getCause();
					String cause = null;
					if (throwable!=null) {
						cause = throwable.toString();
					}
					if (e!=null) {
						msg = e.getMessage();
					}
					log.warn("httpException with message [" + msg + "] and cause [" + cause + "], executeRetries left [" + count + "]");
				} catch (IOException e) {
					httpmethod.abort();
					throw new SenderException(e);
				}
			}
		} finally {
			httpmethod.releaseConnection();
		}

		if (statusCode==-1){
			if (StringUtils.contains(msg.toUpperCase(), "TIMEOUTEXCEPTION")) {
				//java.net.SocketTimeoutException: Read timed out
				throw new TimeOutException("Failed to recover from timeout exception");
			} else {
				throw new SenderException("Failed to recover from exception");
			}
		}

		return result;	
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}



	public String getPhysicalDestinationName() {
		return getUrl();
	}



	public String getUrl() {
		return url;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public String getProxyUserName() {
		return proxyUserName;
	}

	public void setUrl(String string) {
		url = string;
	}

	public void setProxyHost(String string) {
		proxyHost = string;
	}

	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	public void setProxyPort(int i) {
		proxyPort = i;
	}

	public void setProxyUserName(String string) {
		proxyUserName = string;
	}

	public String getProxyRealm() {
		return proxyRealm;
	}

	public void setProxyRealm(String string) {
		proxyRealm = string;
	}

	public String getPassword() {
		return password;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String string) {
		password = string;
	}

	public void setUserName(String string) {
		userName = string;
	}

	public String getMethodType() {
		return methodType;
	}

	public void setMethodType(String string) {
		methodType = string;
	}
	public String getCertificate() {
		return certificate;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}

	public String getKeystoreType() {
		return keystoreType;
	}

	public void setCertificate(String string) {
		certificate = string;
	}

	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}

	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	public String getTruststore() {
		return truststore;
	}

	public String getTruststorePassword() {
		return truststorePassword;
	}

	public void setTruststore(String string) {
		truststore = string;
	}

	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int i) {
		timeout = i;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	public boolean isJdk13Compatibility() {
		return jdk13Compatibility;
	}

	public void setJdk13Compatibility(boolean b) {
		jdk13Compatibility = b;
	}

	public boolean isEncodeMessages() {
		return encodeMessages;
	}

	public void setEncodeMessages(boolean b) {
		encodeMessages = b;
	}

	public boolean isStaleChecking() {
		return staleChecking;
	}

	public void setStaleChecking(boolean b) {
		staleChecking = b;
	}

	public boolean isVerifyHostname() {
		return verifyHostname;
	}

	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	public boolean isFollowRedirects() {
		return followRedirects;
	}

	public void setFollowRedirects(boolean b) {
		followRedirects = b;
	}

	public String getTruststoreType() {
		return truststoreType;
	}
	public void setTruststoreType(String string) {
		truststoreType = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public String getCertificateAuthAlias() {
		return certificateAuthAlias;
	}

	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public void setCertificateAuthAlias(String string) {
		certificateAuthAlias = string;
	}

	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}

	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}

	public void setMaxConnectionRetries(int i) {
		maxConnectionRetries = i;
	}
	public int getMaxConnectionRetries() {
		return maxConnectionRetries;
	}

	public void setMaxExecuteRetries(int i) {
		maxExecuteRetries = i;
	}
	public int getMaxExecuteRetries() {
		return maxExecuteRetries;
	}

	public void setContentType(String string) {
		contentType = string;
	}
	public String getContentType() {
		return contentType;
	}

}
