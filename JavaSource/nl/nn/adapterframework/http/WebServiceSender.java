/*
 * $Log: WebServiceSender.java,v $
 * Revision 1.2  2004-08-17 15:22:15  L190409
 * first version using Axis
 *
 */
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPEnvelope;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import org.apache.soap.SOAPException;
/*
import org.apache.soap.Constants;
import org.apache.soap.Fault;
import org.apache.soap.encoding.SOAPMappingRegistry;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.Response;
import org.apache.soap.transport.http.SOAPHTTPConnection;
*/

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;


import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import javax.net.ssl.SSLSocketFactory;

import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;


/**
 * Sender that sends a message via a WebService
 * @version Id
 * 
 */

public class WebServiceSender implements ISender, HasPhysicalDestination {
	public static final String version="$Id: WebServiceSender.java,v 1.2 2004-08-17 15:22:15 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());
	

	private String name="";
	private String endPointUrl= "";
	private String soapActionURI = "";
	private String encodingStyleURI=null;
	private String methodName = "";
	
	private String proxyHost;
	private int proxyPort=80;
	private String proxyUserName;
	private String proxyPassword;

	private String userName; 
	private String password; 

	private String keystoreType="PKCS12";
	private String certificate;
	private String certificatePassword;

	
	private URL url = null;

	private Service service;
	private Call call;
//	private SOAPMappingRegistry smr = call.getSOAPMappingRegistry();
	
	public String getLogPrefix() {
		return "WebServiceSender ["+getName()+"] to ["+getPhysicalDestinationName()+"] ";
	}
  
	public static void main(String args[]){
		String kenteken="38JBRX";
		// String kenteken="9494DA";
		// String kenteken="BE2738";
		String xmlmsg="<Abz-Audascan-M9><VT><VT_KENTEKEN>"+kenteken+"</VT_KENTEKEN></VT></Abz-Audascan-M9>";
		try {
			WebServiceSender ws = new WebServiceSender();
			ws.name="testmans";
			ws.endPointUrl="https://demo.abzportal.nl:443/demo2/isa-ws/services/isa3";
			ws.soapActionURI="urn:isa3/geefAntwoord";
			ws.methodName="geefAntwoord";
			ws.proxyHost="proxy.rsc1.ing-int";
			ws.proxyPort=8080;
			ws.proxyUserName="190409";
			ws.proxyPassword="BzZYeMP";
			ws.certificate="E:/Gerrit/KeyStoring/999023.0000000002.abc";
			ws.certificatePassword="isa2demo";
			ws.keystoreType="pkcs12";
		
			System.out.println("configuring...");
			ws.configure();
			ws.open();
			System.out.println("sending msg ["+xmlmsg+"]...");
			String result = ws.sendMessage("Abz-Audascan-M9",xmlmsg);
			System.out.println("retrieved result:");
			System.out.println(result);
			System.out.println("ready.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	protected void addProvider(String name) {
		try {
			Class clazz = Class.forName(name);
			java.security.Security.addProvider((java.security.Provider)clazz.newInstance());
		} catch (Throwable t) {
			log.error("cannot add provider ["+name+"]");
		}
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getEndPointUrl())) {
			throw new ConfigurationException(getLogPrefix()+" endPointUrl must be specified");
		}
		System.setProperty("http.proxyHost",getProxyHost());
		System.setProperty("http.proxyPort",new Integer(getProxyPort()).toString());
		System.setProperty("http.proxyUser",getProxyUserName());
		System.setProperty("http.proxyPassword",getProxyPassword());
		System.setProperty("javax.net.ssl.keyStore",getCertificate());
		System.setProperty("javax.net.ssl.keyStorePassword",getCertificatePassword());
		System.setProperty("javax.net.ssl.keyStoreType",getKeystoreType());
//		System.setProperty("javax.net.ssl.trustStore","E:/Gerrit/KeyStoring/cacerts");
//		System.setProperty("javax.net.ssl.trustStorePassword","atricom");
		System.setProperty("java.protocol.handler.pkgs","com.sun.net.ssl.internal.www.protocol");

		addProvider("sun.security.provider.Sun");
		addProvider("com.sun.net.ssl.internal.ssl.Provider");
//		addProvider("com.sun.rsajca.Provider");

		try {
			setEndPoint(new URL(getEndPointUrl()));
		} catch (MalformedURLException e) {
			throw new ConfigurationException(getLogPrefix()+"could not set endPointUrl ["+getEndPointUrl()+"]", e);
		}

		try {
			service = new Service();
			call = (Call) service.createCall();

//			call.setOperation(_operations[0]);
			call.setUseSOAPAction(true);
			call.setSOAPActionURI(getSoapActionURI());
			call.setEncodingStyle(getEncodingStyleURI());
			call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
			call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
			call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
			call.setTargetEndpointAddress( getEndPoint() );
			call.setOperationName(new QName("http://soapinterop.org/", getMethodName()));

		} catch (Exception e) {
			throw new ConfigurationException("exception in configuring webservice Call",e);
		}
		
		
		try {
/*			SOAPHTTPConnection connection = new SOAPHTTPConnection();
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
*/	  	} catch (Exception e) {
			throw new ConfigurationException("exception in configuring proxy or user settings",e);
		}
/*
		try {
	  		
			keystore = KeyStore.getInstance(keystoreType);
			URL url = ClassUtils.getResourceURL(this,certificate);
			InputStream stream = url.openStream();
			keystore.load(stream,certificatePassword.toCharArray());
			stream.close();
			Enumeration aliases = keystore.aliases();
			while (aliases.hasMoreElements()) {
				log.info("keystore alias:"+aliases.nextElement());
			}
		} catch (Exception e) {
			throw new ConfigurationException("exception in configuring keystore",e);
		}
*/	  		
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public synchronized String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
	
		SOAPEnvelope resultmsg; 

		String soapmsg= 
			"<soapenv:Envelope " + 
				"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
				"<soapenv:Body>" +						message +
				"</soapenv:Body>"+
			"</soapenv:Envelope>";
			
		Message msg = new Message(soapmsg);
		String result;

		try {
			log.debug(getLogPrefix()+"invoking call ["+call+"] with msg ["+msg.getSOAPPartAsString()+"]");
			log.debug(getLogPrefix()+"invoking call to endpoint ["+getEndPoint()+"] action ["+getSoapActionURI()+"]");
			resultmsg = call.invoke( msg );
			log.debug(getLogPrefix()+"retrieved result: "+resultmsg.getAsString());
		} catch (Throwable t) {
			throw new SenderException(getLogPrefix()+"cannot send message", t);
		}
		
		// Check the response.
/*		
		if (resultmsg. generatedFault()) {
	    	
		  Fault fault = resp.getFault();
	
		  throw new SenderException(getLogPrefix()+" problems processing message, faultcode ["+fault.getFaultCode()+"]: "+ fault.getFaultString());
		} else {
		  Parameter retValue = resp.getReturnValue();
		  return (String)retValue.getValue();
		}
*/
		try { 		
			result = resultmsg.getAsString();
			return result;
		} catch (Throwable t) {
			throw new SenderException(getLogPrefix()+"cannot convert response", t);
		}
	}


	public String getPhysicalDestinationName() {
		return (url==null) ? (">"+getEndPointUrl()+"<") : getEndPoint().toString();
//		return "unkown";
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

	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
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
	/**
	 * @return
	 */
	public String getCertificate() {
		return certificate;
	}

	/**
	 * @return
	 */
	public String getCertificatePassword() {
		return certificatePassword;
	}

	/**
	 * @return
	 */
	public String getKeystoreType() {
		return keystoreType;
	}

	/**
	 * @param string
	 */
	public void setCertificate(String string) {
		certificate = string;
	}

	/**
	 * @param string
	 */
	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}

	/**
	 * @param string
	 */
	public void setKeystoreType(String string) {
		keystoreType = string;
	}

}