/*
 * $Log: WebServiceSender.java,v $
 * Revision 1.6  2004-09-02 13:25:39  L190409
 * improved fault-handling
 * added handling of encodingStyleUri
 *
 * Revision 1.5  2004/09/01 12:24:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved fault handling
 *
 * Revision 1.4  2004/08/31 15:52:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new version based on HttpSender
 *
 * Revision 1.3  2004/08/23 13:08:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/08/17 15:22:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.util.XmlUtils;


import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;



/*
import org.apache.soap.Constants;
import org.apache.soap.Fault;
import org.apache.soap.encoding.SOAPMappingRegistry;
import org.apache.soap.rpc.Call;
import org.apache.soap.rpc.Parameter;
import org.apache.soap.rpc.Response;
import org.apache.soap.transport.http.SOAPHTTPConnection;
*/


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

/*
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
*/
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;


/**
 * Sender that sends a message via a WebService
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.HttpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapActionURI(String) soapActionURI}</td><td>the SOAPActionUri to be set in the requestheader</td><td>&nbsp;</td></tr>
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
 * 
 */

public class WebServiceSender extends HttpSender {
	public static final String version="$Id: WebServiceSender.java,v 1.6 2004-09-02 13:25:39 L190409 Exp $";
	

	private String soapActionURI = "";
	private String encodingStyleURI=null;
//	private String methodName = "";
	
	private Transformer extractBody;
	private Transformer extractFault;
	private final static String extractNamespaces="xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"";
	private final static String extractBodyXPath="/soapenv:Envelope/soapenv:Body/*";
	private final static String extractFaultXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/*";
	
//	private MessageFactory soapMessageFactory;
	
	public String getLogPrefix() {
		return "WebServiceSender ["+getName()+"] to ["+getPhysicalDestinationName()+"] ";
	}
 
 	public WebServiceSender() {
 		super();
 		setMethodType("POST");
 	}
 

	public void configure() throws ConfigurationException {
		super.configure();
		try {
			extractBody = XmlUtils.createXPathEvaluator(extractNamespaces,extractBodyXPath,"xml");
			extractFault = XmlUtils.createXPathEvaluator(extractNamespaces,extractFaultXPath,"xml");
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create transformer from expression ["+extractBodyXPath+"] or ["+extractFaultXPath+"]",e);
		}
/*		
		try {
			soapMessageFactory = MessageFactory.newInstance();
		} catch (SOAPException e) {
			throw new ConfigurationException("cannot create SoapMessageFactory",e);
		}
*/		
	}

	protected HttpMethod getMethod(String message) throws SenderException {
		
		String encodingStyle="";
		if (!StringUtils.isEmpty(getEncodingStyleURI())) {
			encodingStyle="soapenv:encodingStyle=\""+ getEncodingStyleURI()+"\" ";
		}
		String soapmsg= 
		"<soapenv:Envelope " + 
			"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "+encodingStyle +
			"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" >" + 
			"<soapenv:Body>" +	
				message +
			"</soapenv:Body>"+
		"</soapenv:Envelope>";

		HttpMethod method = super.getMethod(soapmsg);
		method.addRequestHeader("SOAPAction",getSoapActionURI());
		return method;
	}

	public String extractResult(HttpMethod httpmethod) throws SenderException {
		String httpResult;
		try {
			httpResult = super.extractResult(httpmethod);
		} catch (SenderException e) {
			String fault=null;
			try {
				fault = XmlUtils.transformXml(extractFault, httpmethod.getResponseBodyAsString());
			} catch (Exception ee) {
				log.debug("exception extracting fault message",ee);
			}
			if (!StringUtils.isEmpty(fault) && !fault.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")) {
				throw new SenderException("SOAP Fault caught ["+fault+"]",e);
			}
			throw e;
		}
		
		String fault;
		String result;
		try {
			fault = XmlUtils.transformXml(extractFault, httpResult);
			result = XmlUtils.transformXml(extractBody, httpResult);
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
		if (!StringUtils.isEmpty(fault) && !fault.equals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")) {
			throw new SenderException("SOAP Fault caught ["+fault+"]");
		}
		return (result);
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
			return super.sendMessage(correlationID,message);
	}

/*
	public String extractResult_1(HttpMethod httpmethod) throws SenderException {
		SOAPMessage message;
		SOAPBody body;
		try {
			message = soapMessageFactory.createMessage(null,httpmethod.getResponseBodyAsStream());
			body = message.getSOAPBody();
		} catch (Exception e) {
			throw new SenderException("cannot create result message object",e);
		}
		if (body.hasFault()) {
			SOAPFault fault = body.getFault();
			throw new SenderException("SOAP Fault, code["+fault.getFaultCode()+"] actor["+fault.getFaultActor()+"]  string ["+fault.getFaultString()+"] string ["+ToStringBuilder.reflectionToString(fault.getDetail())+"]");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		try {
			message.writeTo(out);
		} catch (Exception e) {
			throw new SenderException("cannot write result message object to stream",e);
		}
		return (out.toString());
	}
*/	





	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}


	public String getSoapActionURI() {
		return soapActionURI;
	}
	public void setSoapActionURI(String soapActionURI) {
		this.soapActionURI = soapActionURI;
	}


	public String getEncodingStyleURI() {
		return encodingStyleURI;
	}

	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
	}


}