/*
 * $Log: WebServiceSender.java,v $
 * Revision 1.8  2004-09-09 14:48:47  L190409
 * removed unused imports
 *
 * Revision 1.7  2004/09/08 14:17:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of SOAP faults
 *
 * Revision 1.6  2004/09/02 13:25:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.XmlUtils;


import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;


import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;


/**
 * Sender that sends a message via a WebService.
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
	public static final String version="$Id: WebServiceSender.java,v 1.8 2004-09-09 14:48:47 L190409 Exp $";
	

	private String soapActionURI = "";
	private String encodingStyleURI=null;
//	private String methodName = "";
	
	private Transformer extractBody;
	private Transformer extractFaultCount;
	private Transformer extractFaultCode;
	private Transformer extractFaultString;
	private final static String extractNamespaces="xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"";
	private final static String extractBodyXPath="/soapenv:Envelope/soapenv:Body/*";
	private final static String extractFaultCountXPath="count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private final static String extractFaultCodeXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private final static String extractFaultStringXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";
	
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
			extractFaultCount = XmlUtils.createXPathEvaluator(extractNamespaces,extractFaultCountXPath,"text");
			extractFaultCode = XmlUtils.createXPathEvaluator(extractNamespaces,extractFaultCodeXPath,"text");
			extractFaultString = XmlUtils.createXPathEvaluator(extractNamespaces,extractFaultStringXPath,"text");
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create transformer from expression ["+extractBodyXPath+"] or ["+extractFaultStringXPath+"]",e);
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

	protected void checkForSoapFault(String responseBody, Throwable nested) throws SenderException {
		String faultString=null;
		String faultCode=null;
		int faultCount=0;
		try {
			String faultCountString = XmlUtils.transformXml(extractFaultCount, responseBody);
			faultCount = Integer.parseInt(faultCountString);
			log.debug("fault count="+faultCount);
			if (faultCount > 0) {
				faultCode = XmlUtils.transformXml(extractFaultCode, responseBody);
				faultString = XmlUtils.transformXml(extractFaultString, responseBody);
				log.debug("faultCode="+faultCode);
				log.debug("faultString="+faultString);
			}
		} catch (IOException e) {
			log.debug("IOException extracting fault message",e);
		} catch (TransformerException e) {
			log.debug("TransformerException extracting fault message:"+e.getMessageAndLocation());
		}
		if (faultCount > 0) {
			String msg = "WebServiceSender ["+getName()+"] caught SOAP fault ["+faultCode+"]: "+faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	public String extractResult(HttpMethod httpmethod) throws SenderException {
		String httpResult;
		try {
			httpResult = super.extractResult(httpmethod);
		} catch (SenderException e) {
			checkForSoapFault(httpmethod.getResponseBodyAsString(), e);
			throw e;
		}
		
		checkForSoapFault(httpmethod.getResponseBodyAsString(), null);
		try {
			String result = XmlUtils.transformXml(extractBody, httpResult);
			return (result);
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
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