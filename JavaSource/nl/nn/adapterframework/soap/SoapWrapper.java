/*
 * $Log: SoapWrapper.java,v $
 * Revision 1.11  2010-07-12 12:49:45  L190409
 * use modified way of specifying namespace definitions
 *
 * Revision 1.10  2009/08/04 11:33:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for header extraction
 *
 * Revision 1.9  2008/10/31 15:02:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * WS-Security made possible
 *
 * Revision 1.8  2008/09/01 13:00:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed xsd and xsi namespace prefix definitions in putInEnvelope
 *
 * Revision 1.7  2008/08/07 07:58:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * skip xml declaration from soap header
 *
 * Revision 1.6  2008/06/09 09:57:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extract fault count always namespace aware
 *
 * Revision 1.5  2008/05/14 11:50:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * strip xml declaration from payload message
 *
 * Revision 1.4  2007/02/12 14:06:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.3  2006/06/21 08:54:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added xsd namespace
 *
 * Revision 1.2  2006/03/15 13:59:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * force to be namespaceAware
 *
 * Revision 1.1  2005/10/18 08:14:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created separate soap-package
 *
 * Revision 1.4  2005/07/05 12:55:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set targetObjectNamespace for putInEnvelope
 *
 * Revision 1.3  2005/05/31 09:16:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.2  2005/05/31 09:15:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added catch for DomBuilderException
 *
 * Revision 1.1  2005/02/24 12:15:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved SOAP conversion coding to SOAP-wrapper
 *
 */
package nl.nn.adapterframework.soap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.AxisClient;
import org.apache.axis.configuration.NullProvider;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.util.DOM2Writer;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Document;

/**
 * Utility class that wraps and unwraps messages from (and into) a SOAP Envelope.
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class SoapWrapper {
	public static final String version="$RCSfile: SoapWrapper.java,v $ $Revision: 1.11 $ $Date: 2010-07-12 12:49:45 $";
	protected Logger log = LogUtil.getLogger(this);

	private TransformerPool extractBody;
	private TransformerPool extractHeader;
	private TransformerPool extractFaultCount;
	private TransformerPool extractFaultCode;
	private TransformerPool extractFaultString;
	private final static String extractNamespaceDefs="soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private final static String extractBodyXPath="/soapenv:Envelope/soapenv:Body/*";
	private final static String extractHeaderXPath="/soapenv:Envelope/soapenv:Header/*";
	private final static String extractFaultCountXPath="count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private final static String extractFaultCodeXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private final static String extractFaultStringXPath="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";

	private static SoapWrapper self=null;
	
	private SoapWrapper() {
		super();
	}
	
	private void init() throws ConfigurationException {
		try {
			extractBody        = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractBodyXPath,"xml"));
			extractHeader      = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractHeaderXPath,"xml"));
			extractFaultCount  = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultCountXPath,"text"));
			extractFaultCode   = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultCodeXPath,"text"));
			extractFaultString = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultStringXPath,"text"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create SOAP transformer",e);
		}
	}
	
	public static SoapWrapper getInstance() throws ConfigurationException {
		if (self==null) {
			self=new SoapWrapper();
			self.init();
		}
		return self;
	}

	public void checkForSoapFault(String responseBody, Throwable nested) throws SenderException {
		String faultString=null;
		String faultCode=null;
		int faultCount=0;
		try {
			faultCount = getFaultCount(responseBody);
			log.debug("fault count="+faultCount);
			if (faultCount > 0) {
				faultCode = getFaultCode(responseBody);
				faultString = getFaultString(responseBody);
				log.debug("faultCode="+faultCode+", faultString="+faultString);
			}
		} catch (DomBuilderException e) {
			log.debug("IOException extracting fault message",e);
		} catch (IOException e) {
			log.debug("IOException extracting fault message",e);
		} catch (TransformerException e) {
			log.debug("TransformerException extracting fault message:"+e.getMessageAndLocation());
		}
		if (faultCount > 0) {
			String msg = "SOAP fault ["+faultCode+"]: "+faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	
	public String getBody(String message) throws DomBuilderException, TransformerException, IOException {
		return extractBody.transform(message,null,true);
	}
	public String getBody(InputStream request) throws TransformerException, IOException {
		return extractBody.transform(new StreamSource(request),null);
	}

	public String getHeader(String message) throws DomBuilderException, TransformerException, IOException {
		return extractHeader.transform(message,null,true);
	}
	public String getHeader(InputStream request) throws TransformerException, IOException {
		return extractHeader.transform(new StreamSource(request),null);
	}

	public int getFaultCount(String message) throws NumberFormatException, DomBuilderException, TransformerException, IOException {
		return Integer.parseInt(extractFaultCount.transform(message,null,true));
	}
	public String getFaultCode(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultCode.transform(message,null,true);
	}
	public String getFaultString(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultString.transform(message,null,true);
	}
	
	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace, String soapHeader) {
		
		String encodingStyle="";
		String targetObjectNamespaceClause="";
		if (!StringUtils.isEmpty(encodingStyleUri)) {
			encodingStyle="soapenv:encodingStyle=\""+ encodingStyleUri+"\" ";
		}
		if (!StringUtils.isEmpty(targetObjectNamespace)) {
			targetObjectNamespaceClause=" xmlns=\""+ targetObjectNamespace+"\" ";
		}
		if (StringUtils.isNotEmpty(soapHeader)) {
			soapHeader="<soapenv:Header>"+XmlUtils.skipXmlDeclaration(soapHeader)+"</soapenv:Header>";
		} else {
			soapHeader="";
		}
		String soapmsg= 
		"<soapenv:Envelope " + 
			"xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "+encodingStyle +
			targetObjectNamespaceClause +
//			"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " + 
//			"xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" " +
			">" + 
			soapHeader+
			"<soapenv:Body>" + 	
				XmlUtils.skipXmlDeclaration(message) +
			"</soapenv:Body>"+
		"</soapenv:Envelope>";
		return soapmsg;
	}

	public String putInEnvelope(String message, String encodingStyleUri) {
		return putInEnvelope(message, encodingStyleUri, null);
	}



	public String signMessage(String soapMessage, String user, String password) throws SenderException {
		try {
			WSSecurityEngine secEngine = WSSecurityEngine.getInstance();
			WSSConfig config = secEngine.getWssConfig();
			config.setPrecisionInMilliSeconds(false);

			// create context
			AxisClient tmpEngine = new AxisClient(new NullProvider());
			MessageContext msgContext = new MessageContext(tmpEngine);

			InputStream in = new ByteArrayInputStream(soapMessage.getBytes());
			Message msg = new Message(in);
			msg.setMessageContext(msgContext);

			// create unsigned envelope
			SOAPEnvelope unsignedEnvelope = msg.getSOAPEnvelope();
			Document doc = unsignedEnvelope.getAsDocument();

			// create security header and insert it into unsigned envelope
			WSSecHeader secHeader = new WSSecHeader();
			secHeader.insertSecurityHeader(doc);

			// add a UsernameToken
			WSSecUsernameToken tokenBuilder = new WSSecUsernameToken();
			tokenBuilder.setPasswordType(WSConstants.PASSWORD_DIGEST);
			tokenBuilder.setUserInfo(user, password);
			tokenBuilder.addNonce();
			tokenBuilder.addCreated();
			tokenBuilder.prepare(doc);

		
			WSSecSignature sign = new WSSecSignature();
			sign.setUsernameToken(tokenBuilder);
			sign.setKeyIdentifierType(WSConstants.UT_SIGNING);
			sign.setSignatureAlgorithm(XMLSignature.ALGO_ID_MAC_HMAC_SHA1);
			sign.build(doc, null, secHeader);

			tokenBuilder.prependToHeader(secHeader);

			// add a Timestamp
			WSSecTimestamp timestampBuilder = new WSSecTimestamp(); 
			timestampBuilder.setTimeToLive(300);
			timestampBuilder.prepare(doc);
			timestampBuilder.prependToHeader(secHeader);

			Document signedDoc = doc;

			String result=DOM2Writer.nodeToString(signedDoc);
		
			return result;
		} catch (Throwable t) {
			throw new SenderException(t);
		}
	}
}
