/*
 * $Log: WebServiceSender.java,v $
 * Revision 1.4  2004-08-31 15:52:22  L190409
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
 * @version Id
 * 
 */

public class WebServiceSender extends HttpSender {
	public static final String version="$Id: WebServiceSender.java,v 1.4 2004-08-31 15:52:22 L190409 Exp $";
	

	private String soapActionURI = "";
	private String encodingStyleURI=null;
//	private String methodName = "";
	
	private Transformer extractBody;
	private final static String extractBodyNamespaces="xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"";
	private final static String extractBodyXPath="/soapenv:Envelope/soapenv:Body/*";
	
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
			extractBody = XmlUtils.createXPathEvaluator(extractBodyNamespaces,extractBodyXPath,"xml");
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create transformer from expression ["+extractBodyXPath+"]",e);
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
		String soapmsg= 
		"<soapenv:Envelope " + 
			"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
			"<soapenv:Body>" +	
				message +
			"</soapenv:Body>"+
		"</soapenv:Envelope>";

		HttpMethod method = super.getMethod(soapmsg);
		method.addRequestHeader("SOAPAction",getSoapActionURI());
		return method;
	}

	public String extractResult(HttpMethod httpmethod) throws SenderException {
		String httpResult = super.extractResult(httpmethod);
		
		String result;
		try {
			result = XmlUtils.transformXml(extractBody, httpResult);
		} catch (Exception e) {
			throw new SenderException("cannot retrieve result message",e);
		}
		return (result);
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