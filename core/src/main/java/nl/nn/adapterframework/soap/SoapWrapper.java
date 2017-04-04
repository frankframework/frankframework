/*
   Copyright 2013, 2015 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.soap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

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
 */
public class SoapWrapper {
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
	
	private TransformerPool extractBody_1_2;
	private TransformerPool extractHeader_1_2;
	private TransformerPool extractFaultCount_1_2;
	private TransformerPool extractFaultCode_1_2;
	private TransformerPool extractFaultString_1_2;	
	private final static String extractNamespaceDefs_1_2="soapenv=http://www.w3.org/2003/05/soap-envelope";	
	private final static String extractBodyXPath_1_2="/soapenv:Envelope/soapenv:Body/*";
	private final static String extractHeaderXPath_1_2="/soapenv:Envelope/soapenv:Header/*";
	private final static String extractFaultCountXPath_1_2="count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private final static String extractFaultCodeXPath_1_2="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private final static String extractFaultStringXPath_1_2="/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";


	private static SoapWrapper self=null;
	
	private SoapWrapper() {
		super();
	}
	
	private void init() throws ConfigurationException {
		try {
			//Soap ver1.1
			extractBody        = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractBodyXPath,"xml",false,null,false));
			extractHeader      = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractHeaderXPath,"xml"));
			extractFaultCount  = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultCountXPath,"text"));
			extractFaultCode   = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultCodeXPath,"text"));
			extractFaultString = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs,extractFaultStringXPath,"text"));
			//Soap ver _1_2
			extractBody_1_2        = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs_1_2,extractBodyXPath_1_2,"xml",false,null,false));
			extractHeader_1_2      = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs_1_2,extractHeaderXPath_1_2,"xml"));
			extractFaultCount_1_2  = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs_1_2,extractFaultCountXPath_1_2,"text"));
			extractFaultCode_1_2   = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs_1_2,extractFaultCodeXPath_1_2,"text"));
			extractFaultString_1_2 = new TransformerPool(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs_1_2,extractFaultStringXPath_1_2,"text"));

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

	public String getBody_1_2(String message) throws DomBuilderException, TransformerException, IOException  {
		return extractBody_1_2.transform(message,null,true);
	}
	public String getBody_1_2(InputStream request) throws TransformerException, IOException {
		return extractBody_1_2.transform(new StreamSource(request),null);
	}

	public String getHeader_1_2(String message) throws DomBuilderException, TransformerException, IOException {
		return extractHeader_1_2.transform(message,null,true);
	}
	public String getHeader_1_2(InputStream request) throws TransformerException, IOException {
		return extractHeader_1_2.transform(new StreamSource(request),null);
	}
	
	public String getBody(String message) throws DomBuilderException, TransformerException, IOException  {
		if (StringUtils.containsIgnoreCase(message, "http://www.w3.org/2003/05/soap-envelope")) {
			return extractBody_1_2.transform(message,null,true);
		}else{
			return extractBody.transform(message,null,true);
		}
	}
	public String getBody(InputStream request) throws TransformerException, IOException {
		return extractBody.transform(new StreamSource(request),null);
	}

	public String getHeader(String message) throws DomBuilderException, TransformerException, IOException {
		if (StringUtils.containsIgnoreCase(message, "http://www.w3.org/2003/05/soap-envelope")) {
			return extractHeader_1_2.transform(message,null,true);
		}else{
			return extractHeader.transform(message,null,true);
		}
	}
	public String getHeader(InputStream request) throws TransformerException, IOException {
		return extractHeader.transform(new StreamSource(request),null);
	}

	public int getFaultCount(String message) throws NumberFormatException, DomBuilderException, TransformerException, IOException {
		if (StringUtils.isEmpty(message)) {
			log.warn("getFaultCount(): message is empty");
			return 0;
		}
		String faultCount = null;
		if (StringUtils.containsIgnoreCase(message, "http://www.w3.org/2003/05/soap-envelope")) {
			faultCount=extractFaultCount_1_2.transform(message,null,true);
		}else{
			faultCount=extractFaultCount.transform(message,null,true);
		}
		if (StringUtils.isEmpty(faultCount)) {
			log.warn("getFaultCount(): could not extract fault count, result is empty");
			return 0;
		}
		if (log.isDebugEnabled()) log.debug("getFaultCount(): transformation result ["+faultCount+"]");
		return Integer.parseInt(faultCount);
	}
	public String getFaultCode(String message) throws DomBuilderException, TransformerException, IOException {
		if (StringUtils.containsIgnoreCase(message, "http://www.w3.org/2003/05/soap-envelope")) {
			return extractFaultCode_1_2.transform(message,null,true);
		}else{
			return extractFaultCode.transform(message,null,true);
		}
	}
	public String getFaultString(String message) throws DomBuilderException, TransformerException, IOException {
		if (StringUtils.containsIgnoreCase(message, "http://www.w3.org/2003/05/soap-envelope")) {
			return extractFaultString_1_2.transform(message,null,true);
		}else{
			return extractFaultString.transform(message,null,true);
		}
	}
	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace, String soapHeader) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace, String soapHeader, String namespaceDefs) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, namespaceDefs, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace, String soapHeader, String namespaceDefs, String soapNamespace) {
		String encodingStyle="";
		String targetObjectNamespaceClause="";
		if (!StringUtils.isEmpty(encodingStyleUri)) {
			encodingStyle=" soapenv:encodingStyle=\""+ encodingStyleUri+"\"";
		}
		if (!StringUtils.isEmpty(targetObjectNamespace)) {
			targetObjectNamespaceClause=" xmlns=\""+ targetObjectNamespace+"\"";
		}
		if (StringUtils.isNotEmpty(soapHeader)) {
			soapHeader="<soapenv:Header>"+XmlUtils.skipXmlDeclaration(soapHeader)+"</soapenv:Header>";
		} else {
			soapHeader="";
		}
		String namespaceClause="";
		if (StringUtils.isNotEmpty(namespaceDefs)) {
			StringTokenizer st1 = new StringTokenizer(namespaceDefs,", \t\r\n\f");
			while (st1.hasMoreTokens()) {
				String namespaceDef=st1.nextToken();
				log.debug("namespaceDef ["+namespaceDef+"]");
				int separatorPos=namespaceDef.indexOf('=');
				if (separatorPos<1) {
					namespaceClause+=" xmlns=\""+namespaceDef+"\"";
				} else {
					namespaceClause+=" xmlns:"+namespaceDef.substring(0,separatorPos)+"=\""+namespaceDef.substring(separatorPos+1)+"\"";
				}
			}
			log.debug("namespaceClause ["+namespaceClause+"]");
		}
		String soapns = "http://schemas.xmlsoap.org/soap/envelope/";
		if (StringUtils.isNotEmpty(soapNamespace)) {
			soapns = soapNamespace;
		}
		String soapmsg= 
		"<soapenv:Envelope xmlns:soapenv=\"" + soapns + "\"" +
			encodingStyle +
			targetObjectNamespaceClause +
//			"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " + 
//			"xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" " +
			namespaceClause +
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

	public String createSoapFaultMessage(String faultcode, String faultstring) {
		String faultCdataString = "<![CDATA[" + faultstring + "]]>";
		String fault= 
		"<soapenv:Fault>" + 	
			"<faultcode>" + faultcode + "</faultcode>" +
			"<faultstring>" + faultCdataString + "</faultstring>" +
		"</soapenv:Fault>";
		return putInEnvelope(fault, null, null, null);
	}

	public String createSoapFaultMessage(String faultstring) {
		return createSoapFaultMessage("soapenv:Server", faultstring);
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
