/*
 * $Log: HttpSender.java,v $
 * Revision 1.2  2004-08-24 11:41:27  a1909356#db2admin
 * Remove warnings
 *
 * Revision 1.1  2004/08/20 13:04:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.http;

import java.io.IOException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * Sender that gets information via a HTTP post or get
 * 
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class HttpSender implements ISender {
	public static final String version = "$Id: HttpSender.java,v 1.2 2004-08-24 11:41:27 a1909356#db2admin Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

	private String name;

	private String baseUrl;

	private String userName;
	private String Password;
	private String host;

	private String proxyHost;
	private int proxyPort=80;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm;
	
	private HttpClient httpclient;


	
	public void configure() throws ConfigurationException {
		httpclient = new HttpClient();
		
		if (!StringUtils.isEmpty(getUserName())) {
			httpclient.getState().setAuthenticationPreemptive(true);
			Credentials defaultcreds = new UsernamePasswordCredentials(getUserName(), getPassword());
			httpclient.getState().setCredentials(null, host, defaultcreds);
		}
		if (!StringUtils.isEmpty(getProxyHost())) {
			httpclient.getHostConfiguration().setProxy(getProxyHost(), getProxyPort());
			httpclient.getState().setProxyCredentials(getProxyRealm(), getProxyHost(),
			new UsernamePasswordCredentials(getProxyUserName(), getProxyPassword()));
		}
	}

	public void open() throws SenderException {
	
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		GetMethod httpget = new GetMethod(baseUrl+message); 
		
		  try {
			httpclient.executeMethod(httpget);
		} catch (HttpException e) {
			throw new SenderException(e);
		} catch (IOException e) {
			throw new SenderException(e);
		}
		  log.debug("status:"+httpget.getStatusLine().toString());	
		  return httpget.getResponseBodyAsString();	
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}


	public String getBaseUrl() {
		return baseUrl;
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

	public void setBaseUrl(String string) {
		baseUrl = string;
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

}
