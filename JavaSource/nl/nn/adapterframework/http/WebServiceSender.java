package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

import org.apache.commons.digester.SetTopRule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.soap.Constants;
import org.apache.soap.Fault;
import org.apache.soap.SOAPException;
import org.apache.soap.encoding.SOAPMappingRegistry;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.Response;
import org.apache.soap.transport.http.SOAPHTTPConnection;

import sun.security.action.GetLongAction;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.SSLSocketFactory;
import java.security.Security;


/**
 * Sender that sends a message via a WebService
 * @version Id
 * 
 */

public class WebServiceSender implements ISender, HasPhysicalDestination {
	public static final String version="$Id: WebServiceSender.java,v 1.1 2004-07-15 07:40:43 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String name="";
	private String endPointUrl= "";
	private String soapActionURI = "";
	private String targetObjectURI = "";
	private String encodingStyleURI = "http://schemas.xmlsoap.org/soap/encoding/";
	private String methodName = "";
	
	private String proxyHost;
	private int proxyPort=80;
	private String proxyUserName;
	private String proxyPassword;

	private String userName; 
	private String password; 
	
	private URL url = null;
	private Call call = new Call();
	private SOAPMappingRegistry smr = call.getSOAPMappingRegistry();
  
    public String getLogPrefix() {
    	return "WebServiceSender ["+getName()+"] to ["+getPhysicalDestinationName()+"] ";
    }
  
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getEndPointUrl())) {
			throw new ConfigurationException(getLogPrefix()+" endPointUrl must be specified");
		}
		try {
	  		call.setEncodingStyleURI(getEncodingStyleURI());
			call.setTargetObjectURI(getTargetObjectURI());
			call.setMethodName(getMethodName());
		} catch (Exception e) {
			throw new ConfigurationException("exception in configuring webservice Call",e);
		}
		
		try {
			setEndPoint(new URL(getEndPointUrl()));
	  	} catch (MalformedURLException e) {
	  		throw new ConfigurationException(getLogPrefix()+"could not set endPointUrl ["+getEndPointUrl()+"]", e);
	  	}
	  	try {
			SOAPHTTPConnection connection = new SOAPHTTPConnection();
			call.setSOAPTransport(connection);
			
			if (!StringUtils.isEmpty(getProxyHost())) {
				log.info(getLogPrefix()+" setting proxy ["+getProxyHost()+":"+getProxyPort()+"] proxy user ["+getProxyUserName()+"]");
				connection.setProxyHost(getProxyHost());
				connection.setProxyPort(getProxyPort());
				connection.setProxyUserName(getProxyUserName());
				connection.setProxyPassword(getProxyPassword());
			}
			if (!StringUtils.isEmpty(getUserName())) {
				connection.setUserName(getUserName());
				connection.setPassword(getPassword());
			}
	  	} catch (Exception e) {
	  		throw new ConfigurationException("exception in configuring proxy or user settings",e);
	  	}
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public synchronized String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
	
	    Vector params = new Vector();
	    Parameter msg = new Parameter("message", String.class, message, null);
	    params.addElement(msg);
	    call.setParams(params);
		Response resp;
	    try {
	    	 resp = call.invoke(getEndPoint(), getSoapActionURI());
		} catch (SOAPException e) {
			throw new SenderException(getLogPrefix()+"cannot send message", e);
	    }	    	
	    // Check the response.
	    if (resp.generatedFault()) {
	    	
	      Fault fault = resp.getFault();
	
	      throw new SenderException(getLogPrefix()+" problems processing message, faultcode ["+fault.getFaultCode()+"]: "+ fault.getFaultString());
	    } else {
	      Parameter retValue = resp.getReturnValue();
	      return (String)retValue.getValue();
	    }
	}

	public String getPhysicalDestinationName() {
		return getEndPoint().toString();
	}

	public synchronized URL getEndPoint() {
    	return url;
  	}
  	
  	protected synchronized void setEndPoint(URL url) {
    	this.url = url;
  	}


	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}



	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getSoapActionURI() {
		return soapActionURI;
	}
	public void setSoapActionURI(String soapActionURI) {
		this.soapActionURI = soapActionURI;
	}

	public String getEndPointUrl() {
		return endPointUrl;
	}
	public void setEndPointUrl(String endPointUrl) {
		this.endPointUrl = endPointUrl;
	}

	public String getEncodingStyleURI() {
		return encodingStyleURI;
	}

	public String getTargetObjectURI() {
		return targetObjectURI;
	}

	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
	}

	public void setTargetObjectURI(String string) {
		targetObjectURI = string;
	}

	public String getPassword() {
		return password;
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

	public String getUserName() {
		return userName;
	}
	public void setPassword(String string) {
		password = string;
	}

	public void setProxyHost(String string) {
		proxyHost = string;
	}
	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setProxyUserName(String string) {
		proxyUserName = string;
	}
	public void setUserName(String string) {
		userName = string;
	}

}
