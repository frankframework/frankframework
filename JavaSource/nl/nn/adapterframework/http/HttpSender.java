/*
 * $Log: HttpSender.java,v $
 * Revision 1.13  2005-02-02 16:36:26  L190409
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
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;


import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;

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
 * <tr><td>{@link #setTimeout(int) Timeout}</td><td>timeout ih ms of obtaining a connection/result. 0 means no timeout</td><td>60000</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>the maximum number of concurrent connections</td><td>2</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyRealm(String) proxyRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setSetStaleChecking(boolean) staleChecking}</td><td>controls whether connections checked to be stale, i.e. appear open, but are not.</td><td>true</td></tr>
 * <tr><td>{@link #setEncodeMessages(boolean) encodeMessages}</td><td>specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.</td><td>false</td></tr>
 * </table>
 * </p>
 * Note:
 * Some certificates require the &lt;java_home&gt;/jre/lib/security/xxx_policy.jar files to be upgraded to unlimited strength. Typically, in such a case, an error message like 
 * <code>Error in loading the keystore: Private key decryption error: (java.lang.SecurityException: Unsupported keysize or algorithm parameters</code> is observed.
 * @author Gerrit van Brakel
 * @since 4.2c
 */
public class HttpSender implements ISenderWithParameters, HasPhysicalDestination {
	public static final String version = "$Id: HttpSender.java,v 1.13 2005-02-02 16:36:26 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

	private String name;

	private String url;
	private String methodType="GET"; // GET or POST

	private int timeout=60000;
	private int maxConnections=2;

	private String userName;
	private String Password;

	private String proxyHost;
	private int proxyPort=80;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm=null;

	private String keystoreType="pkcs12";
	private String certificate;
	private String certificatePassword;
	private String truststore=null;
	private String truststorePassword=null;
	
	private boolean verifyHostname=true;
	private boolean jdk13Compatibility=false;
	private boolean staleChecking=true;
	private boolean encodeMessages=false;

	protected URI uri;
	private MultiThreadedHttpConnectionManager connectionManager;
	protected HttpClient httpclient;

	protected ParameterList paramList = null;

	private class IbisMultiThreadedHttpConnectionManager extends MultiThreadedHttpConnectionManager {
		
		protected boolean checkConnection(HttpConnection connection)  {
			boolean status = connection.isOpen();
			log.debug("IbisMultiThreadedHttpConnectionManager["+name+"] connection open ["+status+"]");
			if (status) {
				try {
					connection.setSoTimeout(connection.getSoTimeout());
				} catch (SocketException e) {
					log.warn("IbisMultiThreadedHttpConnectionManager["+name+"] SocketException while checking", e);
					connection.close();
					return false;
				} catch (IllegalStateException e) {
					log.warn("IbisMultiThreadedHttpConnectionManager["+name+"] IllegalStateException while checking", e);
					connection.close();
					return false;
				}
			}
			return true;
		}
		
//		public void releaseConnection(HttpConnection connection) {
//			log.debug("IbisMultiThreadedHttpConnectionManager["+name+"] closing connection before release");
//			connection.close();
//			super.releaseConnection(connection);
//		}
		
		public HttpConnection getConnection(HostConfiguration hostConfiguration) {
			log.debug("IbisMultiThreadedHttpConnectionManager["+name+"] getConnection(HostConfiguration)");
			HttpConnection result = super.getConnection(hostConfiguration);			
			checkConnection(result);
			return result;
		}
		public HttpConnection getConnection(HostConfiguration hostConfiguration, long timeout) throws HttpException {
			log.debug("IbisMultiThreadedHttpConnectionManager["+name+"] getConnection(HostConfiguration, timeout["+timeout+"])");
			HttpConnection result = super.getConnection(hostConfiguration, timeout);
			int count=10;
			while (count-->0 && !checkConnection(result)) {
				releaseConnection(result);
				result= super.getConnection(hostConfiguration, timeout);
			} 
			return result;
		}
	}

	protected void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			java.security.Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error("cannot add provider ["+name+"], "+t.getClass().getName()+": "+t.getMessage());
		}
	}


	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	
	public void configure() throws ConfigurationException {
//		System.setProperty("javax.net.debug","all");
		httpclient = new HttpClient();
		httpclient.setTimeout(getTimeout());
		httpclient.setConnectionTimeout(getTimeout());
		
		if (paramList!=null) {
			paramList.configure();
		}
		if (StringUtils.isEmpty(getUrl())) {
			throw new ConfigurationException("Url must be specified");
		}
		try {
			uri = new URI(getUrl());

			log.debug("created uri: scheme=["+uri.getScheme()+"] host=["+uri.getHost()+"] port=["+uri.getPort()+"] path=["+uri.getPath()+"]");

			URL certificateUrl=null;
			URL truststoreUrl=null;
	
			if (!StringUtils.isEmpty(getCertificate())) {
				certificateUrl = ClassUtils.getResourceURL(this, getCertificate());
				if (certificateUrl==null) {
					throw new ConfigurationException("cannot find URL for certificate resource ["+getCertificate()+"]");
				}
				log.debug("resolved certificate-URL to ["+certificateUrl.toString()+"]");
			}
			if (!StringUtils.isEmpty(getTruststore())) {
				truststoreUrl = ClassUtils.getResourceURL(this, getTruststore());
				if (truststoreUrl==null) {
					throw new ConfigurationException("cannot find URL for truststore resource ["+getTruststore()+"]");
				}
				log.debug("resolved truststore-URL to ["+certificateUrl.toString()+"]");
			}

			HostConfiguration hostconfiguration = httpclient.getHostConfiguration();		           
			
			if (certificateUrl!=null || truststoreUrl!=null) {
				AuthSSLProtocolSocketFactoryBase socketfactory ;
				try {
					if (isJdk13Compatibility()) {
						addProvider("sun.security.provider.Sun");
						addProvider("com.sun.net.ssl.internal.ssl.Provider");
						System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
						socketfactory = new AuthSSLProtocolSocketFactoryForJsse10x(
							certificateUrl, getCertificatePassword(), getKeystoreType(),
							truststoreUrl, getTruststorePassword(),isVerifyHostname());
					} else {
						socketfactory = new AuthSSLProtocolSocketFactory(
							certificateUrl, getCertificatePassword(), getKeystoreType(),
							truststoreUrl, getTruststorePassword(),isVerifyHostname());
					}
					socketfactory.initSSLContext();	
				} catch (Throwable t) {
					throw new ConfigurationException("cannot create or initialize SocketFactory",t);
				}
				Protocol authhttps = new Protocol(uri.getScheme(), socketfactory, uri.getPort());
				hostconfiguration.setHost(uri.getHost(),uri.getPort(),authhttps);
			} else {
				hostconfiguration.setHost(uri.getHost(),uri.getPort(),uri.getScheme());
			}
			log.debug("configured httpclient for host ["+hostconfiguration.getHostURL()+"]");
			
			if (!StringUtils.isEmpty(getUserName())) {
				httpclient.getState().setAuthenticationPreemptive(true);
				Credentials defaultcreds = new UsernamePasswordCredentials(getUserName(), getPassword());
				httpclient.getState().setCredentials(null, uri.getHost(), defaultcreds);
			}
			if (!StringUtils.isEmpty(getProxyHost())) {
				httpclient.getHostConfiguration().setProxy(getProxyHost(), getProxyPort());
				httpclient.getState().setProxyCredentials(getProxyRealm(), getProxyHost(),
				new UsernamePasswordCredentials(getProxyUserName(), getProxyPassword()));
			}
	

		} catch (URIException e) {
			throw new ConfigurationException("cannot interprete uri ["+getUrl()+"]");
		}

	}

	public void open() throws SenderException {
		connectionManager = new IbisMultiThreadedHttpConnectionManager();
//		connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.setMaxConnectionsPerHost(getMaxConnections());
		log.debug("set up connectionManager, stale checking ["+connectionManager.isConnectionStaleCheckingEnabled()+"]");
		if (connectionManager.isConnectionStaleCheckingEnabled() != isStaleChecking()) {
			log.info("set up connectionManager, setting stale checking ["+isStaleChecking()+"]");
			connectionManager.setConnectionStaleCheckingEnabled(isStaleChecking());
		}
		httpclient.setHttpConnectionManager(connectionManager);
	}

	public void close() throws SenderException {
		connectionManager.shutdown();
		connectionManager=null;
	}

	public boolean isSynchronous() {
		return true;
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuffer path, ParameterValueList parameters) {
		if (parameters!=null) {
			log.debug("appending ["+parameters.size()+"] parameters");
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
			log.debug("appending parameter ["+parameterToAppend+"]");
			path.append(parameterToAppend);
		}
		return parametersAppended;
	}

	protected HttpMethod getMethod(String message, ParameterValueList parameters) throws SenderException {
		try { 
			boolean parametersAppended = false;
			if (isEncodeMessages()) {
				message = URLEncoder.encode(message);
			}
			StringBuffer path = new StringBuffer(uri.getPath());
			if (!StringUtils.isEmpty(uri.getQuery())) {
				path.append("?"+uri.getQuery());
				parametersAppended = true;
			}
			if (parameters!=null) {
				parametersAppended = appendParameters(parametersAppended,path,parameters);
				log.debug("path after appending of parameters ["+path.toString()+"]");
			}
			
			if (getMethodType().equals("GET")) {
				GetMethod result = new GetMethod(path+(parameters==null? message:""));
				log.debug("HttpSender constructed GET-method ["+result.getQueryString()+"]");
				return result;
			} else {
				if (getMethodType().equals("POST")) {
					PostMethod postMethod = new PostMethod(path.toString());
					postMethod.setRequestBody(message);
				
					return postMethod;
				} else {
					throw new SenderException("unknown methodtype ["+getMethodType()+"], must be either POST or GET");
				}
			}
		} catch (URIException e) {
			throw new SenderException("cannot find path from url ["+getUrl()+"]", e);
		}

	}
	
	public String extractResult(HttpMethod httpmethod) throws SenderException {
		int statusCode = httpmethod.getStatusCode();
		if (statusCode!=200) {
			throw new SenderException("httpstatus "+statusCode+": "+httpmethod.getStatusText());
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
			throw new SenderException("Sender ["+getName()+"] caught exception evaluating parameters",e);
		}
		HttpMethod httpmethod=getMethod(message, pvl);
		
		try {
			httpclient.executeMethod(httpmethod);
			log.debug("status:"+httpmethod.getStatusLine().toString());	
			return extractResult(httpmethod);	
		} catch (HttpException e) {
			throw new SenderException(e);
		} catch (IOException e) {
			throw new SenderException(e);
		} finally {
			httpmethod.releaseConnection();
		}
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
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
		return Password;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String string) {
		Password = string;
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

}
