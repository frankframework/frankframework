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
	private TransformerPool extractBody2;
	private TransformerPool extractHeader;
	private TransformerPool extractFaultCount;
	private TransformerPool extractFaultCode;
	private TransformerPool extractFaultString;
	private static final String EXTRACT_NAMESPACE_DEFS  = "soapenv=http://schemas.xmlsoap.org/soap/envelope/";
	private static final String EXTRACT_NAMESPACE_DEFS2 = "soapenv=http://www.w3.org/2003/05/soap-envelope";
	private static final String EXTRACT_BODY_XPATH = "/soapenv:Envelope/soapenv:Body/*";
	private static final String EXTRACT_HEADER_XPATH = "/soapenv:Envelope/soapenv:Header/*";
	private static final String EXTRACT_FAULTCOUNTER_XPATH = "count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private static final String EXTRACT_FAULTCODE_XPATH = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private static final String EXTRACT_FAULTSTRING_XPATH = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";

	private static SoapWrapper self = null;

	private SoapWrapper() {
		super();
	}

	private void init() throws ConfigurationException {
		try {
			extractBody = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS, EXTRACT_BODY_XPATH, "xml", false, null, false));
			extractBody2 = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS2, EXTRACT_BODY_XPATH,"xml",false,null,false));
			extractHeader = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS, EXTRACT_HEADER_XPATH, "xml"));
			extractFaultCount = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS, EXTRACT_FAULTCOUNTER_XPATH, "text"));
			extractFaultCode = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS, EXTRACT_FAULTCODE_XPATH, "text"));
			extractFaultString = new TransformerPool(XmlUtils.createXPathEvaluatorSource(EXTRACT_NAMESPACE_DEFS, EXTRACT_FAULTSTRING_XPATH, "text"));
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("cannot create SOAP transformer", e);
		}
	}

	public static SoapWrapper getInstance() throws ConfigurationException {
		if (self == null) {
			self = new SoapWrapper();
			self.init();
		}
		return self;
	}

	public void checkForSoapFault(String responseBody, Throwable nested) throws SenderException {
		String faultString = null;
		String faultCode = null;
		int faultCount = 0;
		try {
			faultCount = getFaultCount(responseBody);
			log.debug("fault count=" + faultCount);
			if (faultCount > 0) {
				faultCode = getFaultCode(responseBody);
				faultString = getFaultString(responseBody);
				log.debug("faultCode=" + faultCode + ", faultString=" + faultString);
			}
		} catch (DomBuilderException e) {
			log.debug("IOException extracting fault message", e);
		} catch (IOException e) {
			log.debug("IOException extracting fault message", e);
		} catch (TransformerException e) {
			log.debug("TransformerException extracting fault message:" + e.getMessageAndLocation());
		}
		if (faultCount > 0) {
			String msg = "SOAP fault [" + faultCode + "]: " + faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	public String getBody(String message) throws DomBuilderException, TransformerException, IOException  {
		String result = extractBody.transform(message,null,true);
		if (StringUtils.isNotEmpty(result))
			return result;
		return extractBody2.transform(message,null,true);
	}

	public String getBody(InputStream request) throws TransformerException, IOException {
		String result = extractBody.transform(new StreamSource(request),null);
		if (StringUtils.isNotEmpty(result))
			return result;
		return extractBody2.transform(new StreamSource(request),null);
	}

	public String getHeader(String message) throws DomBuilderException, TransformerException, IOException {
		return extractHeader.transform(message, null, true);
	}

	public String getHeader(InputStream request) throws TransformerException, IOException {
		return extractHeader.transform(new StreamSource(request), null);
	}

	public int getFaultCount(String message) throws DomBuilderException, TransformerException, IOException {
		if (StringUtils.isEmpty(message)) {
			log.warn("getFaultCount(): message is empty");
			return 0;
		}
		String faultCount = extractFaultCount.transform(message, null, true);
		if (StringUtils.isEmpty(faultCount)) {
			log.warn("getFaultCount(): could not extract fault count, result is empty");
			return 0;
		}
		if (log.isDebugEnabled()) {
			log.debug("getFaultCount(): transformation result [" + faultCount + "]");
		}
		return Integer.parseInt(faultCount);
	}

	public String getFaultCode(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultCode.transform(message, null, true);
	}

	public String getFaultString(String message) throws DomBuilderException, TransformerException, IOException {
		return extractFaultString.transform(message, null, true);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace,
			String soapHeader) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace,
			String soapHeader, String namespaceDefs) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, namespaceDefs, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri, String targetObjectNamespace,
			String soapHeaderInitial, String namespaceDefs, String soapNamespace) {
		String soapHeader = "";
		String encodingStyle = "";
		String targetObjectNamespaceClause = "";
		if (!StringUtils.isEmpty(encodingStyleUri)) {
			encodingStyle = " soapenv:encodingStyle=\"" + encodingStyleUri + "\"";
		}
		if (!StringUtils.isEmpty(targetObjectNamespace)) {
			targetObjectNamespaceClause = " xmlns=\"" + targetObjectNamespace + "\"";
		}
		if (StringUtils.isNotEmpty(soapHeaderInitial)) {
			soapHeader = "<soapenv:Header>" + XmlUtils.skipXmlDeclaration(soapHeaderInitial) + "</soapenv:Header>";
		} 
		StringBuilder namespaceClause = new StringBuilder();
		if (StringUtils.isNotEmpty(namespaceDefs)) {
			StringTokenizer st1 = new StringTokenizer(namespaceDefs, ", \t\r\n\f");
			while (st1.hasMoreTokens()) {
				String namespaceDef = st1.nextToken();
				log.debug("namespaceDef [" + namespaceDef + "]");
				int separatorPos = namespaceDef.indexOf('=');
				if (separatorPos < 1) {
					namespaceClause.append(" xmlns=\"" + namespaceDef + "\"");
				} else {
					namespaceClause.append(" xmlns:" + namespaceDef.substring(0, separatorPos) + "=\""
							+ namespaceDef.substring(separatorPos + 1) + "\"");
				}
			}
			log.debug("namespaceClause [" + namespaceClause + "]");
		}
		String soapns = "http://schemas.xmlsoap.org/soap/envelope/";
		if (StringUtils.isNotEmpty(soapNamespace)) {
			soapns = soapNamespace;
		}
		return "<soapenv:Envelope xmlns:soapenv=\"" + soapns + "\"" + encodingStyle + targetObjectNamespaceClause
				+ namespaceClause + ">" + soapHeader + "<soapenv:Body>" + XmlUtils.skipXmlDeclaration(message)
				+ "</soapenv:Body>" + "</soapenv:Envelope>";

	}

	public String putInEnvelope(String message, String encodingStyleUri) {
		return putInEnvelope(message, encodingStyleUri, null);
	}

	public String createSoapFaultMessage(String faultcode, String faultstring) {
		String faultCdataString = "<![CDATA[" + faultstring + "]]>";
		String fault = "<soapenv:Fault>" + "<faultcode>" + faultcode + "</faultcode>" + "<faultstring>"
				+ faultCdataString + "</faultstring>" + "</soapenv:Fault>";
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

			return DOM2Writer.nodeToString(signedDoc);

		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
}
