/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020, 2024 WeAreFrank!

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
package org.frankframework.soap;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

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
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;

/**
 * Utility class that wraps and unwraps messages from (and into) a SOAP Envelope.
 *
 * @author Gerrit van Brakel
 */
public class SoapWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private static TransformerPool extractBodySoap11;
	private static TransformerPool extractBodySoap12;
	private static TransformerPool extractHeaderSoap11;
	private static TransformerPool extractHeaderSoap12;
	private static TransformerPool extractFaultCount11;
	private static TransformerPool extractFaultCount12;
	private static TransformerPool extractFaultCode11;
	private static TransformerPool extractFaultCode12;
	private static TransformerPool extractFaultString11;
	private static TransformerPool extractFaultString12;
	private static final String NAMESPACE_DEFS_SOAP11 = "soapenv=" + SoapVersion.SOAP11.namespace;
	private static final String NAMESPACE_DEFS_SOAP12 = "soapenv=" + SoapVersion.SOAP12.namespace;
	private static final String EXTRACT_BODY_XPATH = "/soapenv:Envelope/soapenv:Body/*";
	private static final String EXTRACT_HEADER_XPATH = "/soapenv:Envelope/soapenv:Header/*";
	private static final String EXTRACT_FAULTCOUNTER_XPATH = "count(/soapenv:Envelope/soapenv:Body/soapenv:Fault)";
	private static final String EXTRACT_FAULTCODE_XPATH_SOAP11 = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultcode";
	private static final String EXTRACT_FAULTCODE_XPATH_SOAP12 = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/soapenv:Code";
	private static final String EXTRACT_FAULTSTRING_XPATH_SOAP11 = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/faultstring";
	private static final String EXTRACT_FAULTSTRING_XPATH_SOAP12 = "/soapenv:Envelope/soapenv:Body/soapenv:Fault/soapenv:Reason";
	public static final String SOAP_VERSION_SESSION_KEY = "SoapWrapper.SoapVersion";
	public static final String DEFAULT_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private static SoapWrapper self = null;
	private @Setter WsuIdAllocator idAllocator = null; //Only used for testing purposes

	private SoapWrapper() {
		super();
		JCEMapper.registerDefaultAlgorithms();
	}

	private void init() throws ConfigurationException {
		if (extractBodySoap11 == null) {
			try {
				extractBodySoap11 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_BODY_XPATH, TransformerPool.OutputType.XML, false, false), XmlUtils.DEFAULT_XSLT_VERSION);
				extractBodySoap12 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_BODY_XPATH, TransformerPool.OutputType.XML, false, false), XmlUtils.DEFAULT_XSLT_VERSION);
				extractHeaderSoap11 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_HEADER_XPATH, TransformerPool.OutputType.XML), XmlUtils.DEFAULT_XSLT_VERSION);
				extractHeaderSoap12 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_HEADER_XPATH, TransformerPool.OutputType.XML), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultCount11 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTCOUNTER_XPATH, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultCount12 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_FAULTCOUNTER_XPATH, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultCode11 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTCODE_XPATH_SOAP11, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultCode12 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_FAULTCODE_XPATH_SOAP12, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultString11 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP11, EXTRACT_FAULTSTRING_XPATH_SOAP11, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
				extractFaultString12 = TransformerPool.getUtilityInstance(XmlUtils.createXPathEvaluatorSource(NAMESPACE_DEFS_SOAP12, EXTRACT_FAULTSTRING_XPATH_SOAP12, TransformerPool.OutputType.TEXT), XmlUtils.DEFAULT_XSLT_VERSION);
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("cannot create SOAP transformer", e);
			}
		}
	}

	public static SoapWrapper getInstance() throws ConfigurationException {
		if (self == null) {
			self = new SoapWrapper();
			self.init();
		}
		return self;
	}

	public void checkForSoapFault(Message responseBody, Throwable nested, PipeLineSession session) throws SenderException {
		String faultString = null;
		String faultCode = null;
		int faultCount = 0;
		try {
			responseBody.preserve();
			faultCount = getFaultCount(responseBody);
			log.debug("fault count={}", faultCount);
			if (faultCount > 0) {
				faultCode = getFaultCode(responseBody, session);
				faultString = getFaultString(responseBody, session);
				log.debug("faultCode={}, faultString={}", faultCode, faultString);
			}
		} catch (SAXException | IOException e) {
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

	public Message getBody(Message message, boolean allowPlainXml, PipeLineSession session, String soapNamespaceSessionKey) throws SAXException, TransformerException, IOException {
		message.preserve();

		// First try with Soap 1.1 transform pool, when no result, try with Soap 1.2 transform pool
		String extractedBody = extractMessageWithTransformers(extractBodySoap11, extractBodySoap12, message, session, soapNamespaceSessionKey);
		if (StringUtils.isNotEmpty(extractedBody)) {
			return new Message(extractedBody);
		}

		if (session != null) {
			if (StringUtils.isNotEmpty(soapNamespaceSessionKey)) {
				session.putIfAbsent(soapNamespaceSessionKey, SoapVersion.NONE.namespace);
			}
			if (allowPlainXml) {
				session.putIfAbsent(SOAP_VERSION_SESSION_KEY, SoapVersion.NONE);
			}
		}
		return allowPlainXml ? message : Message.nullMessage();
	}

	private String extractMessageWithTransformers(TransformerPool transformerS11, TransformerPool transformerS12, Message message, PipeLineSession session, String soapNamespaceSessionKey) throws IOException, TransformerException, SAXException {
		// If SOAP version is already determined in the session, directly use the SOAP 1.2 transformer
		SoapVersion soapVersion = getSoapVersionFromSession(session);
		String extractedMessage;
		if (soapVersion == SoapVersion.SOAP12) {
			extractedMessage = transformerS12.transformToString(message);
			// If session had the wrong SOAP version stored (e.g. multiple SoapWrappers), try SOAP 1.1 too. (#6032)
			if (StringUtils.isEmpty(extractedMessage)) {
				extractedMessage = transformerS11.transformToString(message);
				// TODO: previous SoapWrapper configurations can write the wrong SOAP version to the session (using the same name).
				// Consider a solution to match the right saved SOAP version with the right SoapWrapper: e.g. cache SoapVersion inside Message.context
			}
		} else if (soapVersion == SoapVersion.NONE) {
			return null;
		} else {
			extractedMessage = transformerS11.transformToString(message);
			if (StringUtils.isNotEmpty(extractedMessage)) {
				soapVersion = SoapVersion.SOAP11;
			} else {
				extractedMessage = transformerS12.transformToString(message);
				if (StringUtils.isNotEmpty(extractedMessage)) {
					soapVersion = SoapVersion.SOAP12;
				}
			}
		}

		if (StringUtils.isNotEmpty(extractedMessage)) {
			if (session != null && soapVersion != null) {
				session.putIfAbsent(SOAP_VERSION_SESSION_KEY, soapVersion);
				if (StringUtils.isNotEmpty(soapNamespaceSessionKey)) {
					session.putIfAbsent(soapNamespaceSessionKey, soapVersion.namespace);
				}
			}
			return extractedMessage;
		}
		return null;
	}

	private SoapVersion getSoapVersionFromSession(final PipeLineSession session) {
		if (session == null) return null;
		Object soapVersionObject = session.getOrDefault(SoapWrapper.SOAP_VERSION_SESSION_KEY, null);
		if (soapVersionObject instanceof SoapVersion version) {
			log.debug("Found SOAP version in session: {}", version.name());
			return version;
		}
		return null;
	}

	public String getHeader(final Message message, final PipeLineSession session) throws SAXException, TransformerException, IOException {
		return extractMessageWithTransformers(extractHeaderSoap11, extractHeaderSoap12, message, session, null);
	}

	public int getFaultCount(Message message) throws SAXException, TransformerException, IOException {
		if (Message.isEmpty(message)) {
			log.warn("getFaultCount(): message is empty");
			return 0;
		}
		// Do not optimize transformer with extractMessageWithTransformers() method, since the output is always "0", even though fault parts are not found at all.
		String faultCount = extractFaultCount11.transformToString(message);
		if (StringUtils.isEmpty(faultCount) || "0".equals(faultCount)) {
			faultCount = extractFaultCount12.transformToString(message);
		}
		if (StringUtils.isEmpty(faultCount)) {
			log.warn("getFaultCount(): could not extract fault count, result is empty");
			return 0;
		}
		log.debug("getFaultCount(): transformation result [{}]", faultCount);
		return Integer.parseInt(faultCount);
	}

	protected String getFaultCode(Message message, PipeLineSession session) throws SAXException, TransformerException, IOException {
		return extractMessageWithTransformers(extractFaultCode11, extractFaultCode12, message, session, null);
	}

	protected String getFaultString(Message message, PipeLineSession session) throws SAXException, TransformerException, IOException {
		return extractMessageWithTransformers(extractFaultString11, extractFaultString12, message, session, null);
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
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace, soapHeader, namespaceDefs, null, null, false, false);
	}

	public Message putInEnvelope(Message message, String encodingStyleUri, String targetObjectNamespace, String soapHeaderInitial, String namespaceDefs, String soapNamespace, CredentialFactory wsscf, boolean passwordDigest, boolean includeXmlDeclaration) throws IOException {
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
					namespaceClause.append(" xmlns:").append(namespaceDef, 0, separatorPos).append("=\"").append(namespaceDef.substring(separatorPos + 1)).append("\"");
				}
			}
			log.debug("namespaceClause [{}]", namespaceClause);
		}
		String soapns = StringUtils.isNotEmpty(soapNamespace) ? soapNamespace : SoapVersion.SOAP11.namespace;

		// XmlUtils.skipXmlDeclaration call below removes the xml declaration from the message, so adding it back if required
		String messageContent = message.asString();
		String xmlHeader = includeXmlDeclaration ? DEFAULT_XML_HEADER : "";
		Message result = new Message(xmlHeader +
				"<soapenv:Envelope xmlns:soapenv=\"" + soapns + "\"" + encodingStyle + targetObjectNamespaceClause
				+ namespaceClause + ">" + soapHeader + "<soapenv:Body>" + XmlUtils.skipXmlDeclaration(messageContent)
				+ "</soapenv:Body>" + "</soapenv:Envelope>");
		result.getContext().withMimeType(MediaType.TEXT_XML); // soap mimetype is text/xml

		if (wsscf == null) {
			return result;
		}

		try (Message ignore = result) {
			return signMessage(result, wsscf.getUsername(), wsscf.getPassword(), passwordDigest);
		}
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
