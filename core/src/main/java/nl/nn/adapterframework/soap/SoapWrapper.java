/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import java.io.IOException;
import java.util.StringTokenizer;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.common.util.WSTimeSource;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WsuIdAllocator;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.xml.security.algorithms.JCEMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Utility class that wraps and unwraps messages from (and into) a SOAP Envelope.
 *
 * @author Gerrit van Brakel
 */
public class SoapWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private TransformerPool extractBodySoap11;
	private TransformerPool extractBodySoap12;
	private TransformerPool extractHeader;
	private TransformerPool extractFaultCount;
	private TransformerPool extractFaultCode;
	private TransformerPool extractFaultString;
	private static final String NAMESPACE_DEFS_SOAP11 = "soapenv="+SoapVersion.SOAP11.namespace;
	private static final String NAMESPACE_DEFS_SOAP12 = "soapenv="+SoapVersion.SOAP12.namespace;
	private static final String EXTRACT_BODY_XPATH = "/soapenv:Envelope/soapenv:Body/*";
	private static final String EXTRACT_HEADER_XPATH = "/soapenv:Envelope/soapenv:Header/*";
	private static final String EXTRACT_FAULTCOUNTER_XPATH = "count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private static final String EXTRACT_FAULTCODE_XPATH = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private static final String EXTRACT_FAULTSTRING_XPATH = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";

	private static SoapWrapper self = null;
	private @Setter WsuIdAllocator idAllocator = null; //Only used for testing purposes

	private SoapWrapper() {
		super();

		JCEMapper.registerDefaultAlgorithms();
	}

	private void init() throws ConfigurationException {
		try {
			extractBodySoap11  = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_BODY_XPATH, OutputType.XML, false, null, false));
			extractBodySoap12  = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_BODY_XPATH, OutputType.XML, false, null, false));
			extractHeader      = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_HEADER_XPATH, OutputType.XML));
			extractFaultCount  = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTCOUNTER_XPATH, OutputType.TEXT));
			extractFaultCode   = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTCODE_XPATH, OutputType.TEXT));
			extractFaultString = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTSTRING_XPATH, OutputType.TEXT));
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

	public void checkForSoapFault(Message responseBody, Throwable nested) throws SenderException {
		String faultString = null;
		String faultCode = null;
		int faultCount = 0;
		try {
			responseBody.preserve();
			faultCount = getFaultCount(responseBody);
			log.debug("fault count={}", faultCount);
			if (faultCount > 0) {
				faultCode = getFaultCode(responseBody);
				faultString = getFaultString(responseBody);
				log.debug("faultCode={}, faultString={}", faultCode, faultString);
			}
		} catch (SAXException|IOException e) {
			log.debug("IOException extracting fault message", e);
		} catch (TransformerException e) {
			log.debug("TransformerException extracting fault message: {}", e.getMessageAndLocation());
		}
		if (faultCount > 0) {
			String msg = "SOAP fault [" + faultCode + "]: " + faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	public Message getBody(Message message) throws SAXException, TransformerException, IOException  {
		return getBody(message, false, null, null);
	}

	public Message getBody(Message message, boolean allowPlainXml, PipeLineSession session, String soapNamespaceSessionKey) throws SAXException, TransformerException, IOException  {
		message.preserve();
		Message result = new Message(extractBodySoap11.transform(message.asSource()));
		if (!Message.isEmpty(result)) {
			if (session!=null && StringUtils.isNotEmpty(soapNamespaceSessionKey)) {
				session.put(soapNamespaceSessionKey, SoapVersion.SOAP11.namespace);
			}
			return result;
		}
		result = new Message(extractBodySoap12.transform(message.asSource()));
		if (!Message.isEmpty(result)) {
			if (session!=null && StringUtils.isNotEmpty(soapNamespaceSessionKey)) {
				session.put(soapNamespaceSessionKey, SoapVersion.SOAP12.namespace);
			}
			return result;
		}
		if (session!=null && StringUtils.isNotEmpty(soapNamespaceSessionKey)) {
			session.put(soapNamespaceSessionKey, SoapVersion.NONE.namespace);
		}
		return allowPlainXml ? message : new Message(""); // could replace "" with nullMessage(), but then tests fail.
	}


	public String getHeader(Message message) throws SAXException, TransformerException, IOException {
		return extractHeader.transform(message.asSource());
	}

	public int getFaultCount(Message message) throws SAXException, TransformerException, IOException {
		if (Message.isEmpty(message)) {
			log.warn("getFaultCount(): message is empty");
			return 0;
		}
		String faultCount = extractFaultCount.transform(message.asSource());
		if (StringUtils.isEmpty(faultCount)) {
			log.warn("getFaultCount(): could not extract fault count, result is empty");
			return 0;
		}
		log.debug("getFaultCount(): transformation result [{}]", faultCount);
		return Integer.parseInt(faultCount);
	}

	public String getFaultCode(Message message) throws SAXException, TransformerException, IOException {
		return extractFaultCode.transform(message.asSource());
	}

	public String getFaultString(Message message) throws SAXException, TransformerException, IOException {
		return extractFaultString.transform(message.asSource());
	}

	public Message putInEnvelope(Message message, String encodingStyleUri) throws IOException {
		return putInEnvelope(message, encodingStyleUri, null);
	}

	public Message putInEnvelope(Message message, String encodingStyleUri, String targetObjectNamespace) throws IOException {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, null);
	}

	public Message putInEnvelope(Message message, String encodingStyleUri, String targetObjectNamespace, String soapHeader) throws IOException {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, null);
	}

	public Message putInEnvelope(Message message, String encodingStyleUri, String targetObjectNamespace, String soapHeader, String namespaceDefs) throws IOException {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, namespaceDefs, null, null, false);
	}

	public Message putInEnvelope(Message message, String encodingStyleUri, String targetObjectNamespace, String soapHeaderInitial, String namespaceDefs, String soapNamespace, CredentialFactory wsscf, boolean passwordDigest) throws IOException {
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
				log.debug("namespaceDef [{}]", namespaceDef);
				int separatorPos = namespaceDef.indexOf('=');
				if (separatorPos < 1) {
					namespaceClause.append(" xmlns=\"").append(namespaceDef).append("\"");
				} else {
					namespaceClause.append(" xmlns:" + namespaceDef.substring(0, separatorPos) + "=\"" + namespaceDef.substring(separatorPos + 1) + "\"");
				}
			}
			log.debug("namespaceClause [{}]", namespaceClause);
		}
		String soapns = StringUtils.isNotEmpty(soapNamespace) ? soapNamespace : SoapVersion.SOAP11.namespace;
		Message result = new Message("<soapenv:Envelope xmlns:soapenv=\"" + soapns + "\"" + encodingStyle + targetObjectNamespaceClause
				+ namespaceClause + ">" + soapHeader + "<soapenv:Body>" + XmlUtils.skipXmlDeclaration(message.asString())
				+ "</soapenv:Body>" + "</soapenv:Envelope>");
		if (wsscf != null) {
			result = signMessage(result, wsscf.getUsername(), wsscf.getPassword(), passwordDigest);
		}
		return result;
	}

	public Message createSoapFaultMessage(String faultcode, String faultstring) {
		String faultCdataString = "<![CDATA[" + faultstring + "]]>";
		String fault = "<soapenv:Fault>" + "<faultcode>" + faultcode + "</faultcode>" +
						"<faultstring>" + faultCdataString + "</faultstring>" + "</soapenv:Fault>";
		try {
			return putInEnvelope(new Message(fault), null, null, null);
		} catch (IOException e) {
			log.warn("Could not create SoapFaultMessage", e);
			return new Message(faultstring);
		}
	}

	public Message createSoapFaultMessage(String faultstring) {
		return createSoapFaultMessage("soapenv:Server", faultstring);
	}

	public Message signMessage(Message soapMessage, String user, String password, boolean passwordDigest) {
		try {
			// We only support signing for soap1_1 ?
			// Create an empty message and populate it later. createMessage(MimeHeaders, InputStream) requires proper headers to be set which we do not have...
			MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
			SOAPMessage msg = factory.createMessage();
			SOAPPart part = msg.getSOAPPart();
			part.setContent(new StreamSource(soapMessage.asInputStream()));

			// create unsigned envelope
			SOAPEnvelope unsignedEnvelope = part.getEnvelope();
			Document doc = unsignedEnvelope.getOwnerDocument();

			// create security header and insert it into unsigned envelope
			WSSecHeader secHeader = new WSSecHeader(doc);
			secHeader.insertSecurityHeader();

			// add a UsernameToken
			WSSecUsernameToken tokenBuilder = new WSSecUsernameToken(secHeader);
			tokenBuilder.setIdAllocator(idAllocator);
			if (passwordDigest) {
				tokenBuilder.setPasswordType(WSS4JConstants.PASSWORD_DIGEST);
			} else {
				tokenBuilder.setPasswordType(WSS4JConstants.PASSWORD_TEXT);
			}
			tokenBuilder.setPrecisionInMilliSeconds(false);
			tokenBuilder.setUserInfo(user, password);
			WSTimeSource timesource = tokenBuilder.getWsTimeSource();
			tokenBuilder.addNonce();
			tokenBuilder.addCreated();
			tokenBuilder.prepare(null);
			Element element = tokenBuilder.getUsernameTokenElement();
			String nonce = XmlUtils.getChildTagAsString(element, "wsse:Nonce");
			byte[] decodedNonce = org.apache.xml.security.utils.XMLUtils.decode(nonce);
			String created = XmlUtils.getChildTagAsString(element, "wsu:Created");

			WSSecSignature sign = new WSSecSignature(secHeader);
			sign.setIdAllocator(idAllocator);
			sign.setCustomTokenValueType(WSS4JConstants.WSS_USERNAME_TOKEN_VALUE_TYPE);
			sign.setCustomTokenId(tokenBuilder.getId());
			sign.setSigCanonicalization(WSS4JConstants.C14N_EXCL_OMIT_COMMENTS);
			sign.setAddInclusivePrefixes(false);
			String signatureValue = UsernameTokenUtil.doPasswordDigest(decodedNonce, created, password); //conform WS-Trust spec
			sign.setSecretKey(signatureValue.getBytes(StreamUtil.DEFAULT_CHARSET));
			sign.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING); //UT_SIGNING no longer exists since v1.5.11
			sign.setSignatureAlgorithm(WSS4JConstants.HMAC_SHA1);
			sign.build(null);

			tokenBuilder.prependToHeader();

			// add a Timestamp
			WSSecTimestamp timestampBuilder = new WSSecTimestamp(secHeader);
			timestampBuilder.setWsTimeSource(timesource);
			timestampBuilder.setTimeToLive(300);
			timestampBuilder.setIdAllocator(idAllocator);
			timestampBuilder.build();

			return new Message(doc);
		} catch (Exception e) {
			throw new RuntimeException("Could not sign message", e);
		}
	}
}
