/*
 * $Log: HttpSender.java,v $
 * Revision 1.6  2004-09-08 14:18:34  L190409
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
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Sender that gets information via a HTTP post or get
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
 * <tr><td>{@link #setAddSecurityProviders(boolean) addSecurityProviders}</td><td>if true, basic SUN security providers are added to the list of providers</td><td>false</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 * @since 4.2c
 */
public class HttpSender implements ISender, HasPhysicalDestination {
	public static final String version = "$Id: HttpSender.java,v 1.6 2004-09-08 14:18:34 L190409 Exp $";
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
	
	private boolean addSecurityProviders=false;

	protected URI uri;
	private MultiThreadedHttpConnectionManager connectionManager;
	protected HttpClient httpclient;

	protected void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			java.security.Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error("cannot add provider ["+name+"]");
		}
	}
	
	public void configure() throws ConfigurationException {
//		System.setProperty("javax.net.debug","all");
		httpclient = new HttpClient();
		httpclient.setTimeout(getTimeout());
		httpclient.setConnectionTimeout(getTimeout());
		
		if (StringUtils.isEmpty(getUrl())) {
			throw new ConfigurationException("Url must be specified");
		}
		if (isAddSecurityProviders()) {
			addProvider("sun.security.provider.Sun");
			addProvider("com.sun.net.ssl.internal.ssl.Provider");
			System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");
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
					throw new ConfigurationException("cannot find URL for certificate resource ["+getTruststore()+"]");
				}
				log.debug("resolved truststore-URL to ["+certificateUrl.toString()+"]");
			}

			HostConfiguration hostconfiguration = httpclient.getHostConfiguration();		           
			
			if (certificateUrl!=null || truststoreUrl!=null) {
				AuthSSLProtocolSocketFactory socketfactory ;
				try {
					socketfactory = new AuthSSLProtocolSocketFactory(
						certificateUrl, getCertificatePassword(), getKeystoreType(),
						truststoreUrl, getTruststorePassword());
					socketfactory.init();	
				} catch (Throwable t) {
					throw new ConfigurationException("cannot create or initialize SocketFactory",t);
				}
				Protocol authhttps = new Protocol(uri.getScheme(), socketfactory, uri.getPort());
				hostconfiguration.setHost(uri.getHost(),uri.getPort(),authhttps);
			} else {
				hostconfiguration.setHost(uri.getHost(),uri.getPort(),uri.getScheme());
			}
			log.debug("configured for httpclient for host: "+hostconfiguration.getHostURL());
			
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
		connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.setMaxConnectionsPerHost(getMaxConnections());
		httpclient.setHttpConnectionManager(connectionManager);
	}

	public void close() throws SenderException {
		connectionManager.shutdown();
		connectionManager=null;
	}

	public boolean isSynchronous() {
		return true;
	}

	protected HttpMethod getMethod(String message) throws SenderException {
		try { 
			String path=uri.getPath();
			if (!StringUtils.isEmpty(uri.getQuery())) {
				path += "?"+uri.getQuery();
			}
			if (getMethodType().equals("GET")) {
				return new GetMethod(path+message);
			} else {
				if (getMethodType().equals("POST")) {
					PostMethod postMethod = new PostMethod(path);
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

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		HttpMethod httpmethod=getMethod(message);
		
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

	public boolean isAddSecurityProviders() {
		return addSecurityProviders;
	}

	public void setAddSecurityProviders(boolean b) {
		addSecurityProviders = b;
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

}
