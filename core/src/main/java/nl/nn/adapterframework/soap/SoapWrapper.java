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
import java.io.InputStream;
import java.util.StringTokenizer;

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
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Utility class that wraps and unwraps messages from (and into) a SOAP
 * Envelope.
 * 
 * @author Gerrit van Brakel
 */
public class SoapWrapper {
	protected Logger log = LogUtil.getLogger(this);

	private static SoapWrapper self = null;

	private SoapWrapper() {
		super();
	}

	public static SoapWrapper getInstance() throws ConfigurationException {
		if (self == null) {
			self = new SoapWrapper();
			// create dummy SOAPEnvelope for performance purpose
			String dummySoap = self.putInEnvelope("<dummy/>", null);
			SOAPEnvelope env = self.buildEnvelope(dummySoap);
		}
		return self;
	}

	public void checkForSoapFault(String responseBody, Throwable nested)
			throws SenderException {
		String faultString = null;
		String faultCode = null;
		int faultCount = getFaultCount(responseBody);
		log.debug("fault count=" + faultCount);
		if (faultCount > 0) {
			faultCode = getFaultCode(responseBody);
			faultString = getFaultString(responseBody);
			log.debug(
					"faultCode=" + faultCode + ", faultString=" + faultString);
		}
		if (faultCount > 0) {
			String msg = "SOAP fault [" + faultCode + "]: " + faultString;
			log.info(msg);
			throw new SenderException(msg, nested);
		}
	}

	public String getBody(String message) {
		return getBody(buildEnvelope(message));
	}

	public String getBody(InputStream request) {
		return getBody(buildEnvelope(request));
	}

	public String getBody(SOAPEnvelope env) {
		if (env == null)
			return null;

		try {
			return env.getFirstBody().getAsString();
		} catch (Exception e) {
			log.warn("Exception during extracting body: " + e.getMessage());
			return null;
		}
	}

	public String getHeader(String message) {
		return getHeader(buildEnvelope(message));
	}

	public String getHeader(InputStream request) {
		return getHeader(buildEnvelope(request));
	}

	public String getHeader(SOAPEnvelope env) {
		if (env == null)
			return null;

		try {
			return env.getHeader().getFirstChild().toString();
		} catch (Exception e) {
			log.warn("Exception during extracting header: " + e.getMessage());
			return null;
		}

	}

	public int getFaultCount(String message) {
		SOAPEnvelope env = buildEnvelope(message);
		if (env == null)
			return 0;

		try {
			if (env.getBody().hasFault()) {
				return 1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			log.warn("Exception during extracting fault: " + e.getMessage());
			return 0;
		}
	}

	public String getFaultCode(String message) {
		SOAPEnvelope env = buildEnvelope(message);
		if (env == null)
			return null;

		try {
			if (env.getBody().hasFault()) {
				return env.getBody().getFault().getFaultCode();
			} else {
				return null;
			}
		} catch (Exception e) {
			log.warn("Exception during extracting fault: " + e.getMessage());
			return null;
		}
	}

	public String getFaultString(String message) {
		SOAPEnvelope env = buildEnvelope(message);
		if (env == null)
			return null;

		try {
			if (env.getBody().hasFault()) {
				return env.getBody().getFault().getFaultString();
			} else {
				return null;
			}
		} catch (Exception e) {
			log.warn("Exception during extracting fault: " + e.getMessage());
			return null;
		}
	}

	public SOAPEnvelope buildEnvelope(String message) {
		InputStream is = new ByteArrayInputStream(message.getBytes());
		return buildEnvelope(is);
	}

	public SOAPEnvelope buildEnvelope(InputStream request) {
		try {
			return new SOAPEnvelope(request);
		} catch (SAXException e) {
			log.info("Message is not a soap message");
			return null;
		}
	}

	public String putInEnvelope(String message, String encodingStyleUri,
			String targetObjectNamespace) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace,
				null);
	}

	public String putInEnvelope(String message, String encodingStyleUri,
			String targetObjectNamespace, String soapHeader) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace,
				soapHeader, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri,
			String targetObjectNamespace, String soapHeader,
			String namespaceDefs) {
		return putInEnvelope(message, encodingStyleUri, targetObjectNamespace,
				soapHeader, namespaceDefs, null);
	}

	public String putInEnvelope(String message, String encodingStyleUri,
			String targetObjectNamespace, String soapHeader,
			String namespaceDefs, String soapNamespace) {
		String encodingStyle = "";
		String targetObjectNamespaceClause = "";
		if (!StringUtils.isEmpty(encodingStyleUri)) {
			encodingStyle = " soapenv:encodingStyle=\"" + encodingStyleUri
					+ "\"";
		}
		if (!StringUtils.isEmpty(targetObjectNamespace)) {
			targetObjectNamespaceClause = " xmlns=\"" + targetObjectNamespace
					+ "\"";
		}
		if (StringUtils.isNotEmpty(soapHeader)) {
			soapHeader = "<soapenv:Header>"
					+ XmlUtils.skipXmlDeclaration(soapHeader)
					+ "</soapenv:Header>";
		} else {
			soapHeader = "";
		}
		String namespaceClause = "";
		if (StringUtils.isNotEmpty(namespaceDefs)) {
			StringTokenizer st1 = new StringTokenizer(namespaceDefs,
					", \t\r\n\f");
			while (st1.hasMoreTokens()) {
				String namespaceDef = st1.nextToken();
				log.debug("namespaceDef [" + namespaceDef + "]");
				int separatorPos = namespaceDef.indexOf('=');
				if (separatorPos < 1) {
					namespaceClause += " xmlns=\"" + namespaceDef + "\"";
				} else {
					namespaceClause += " xmlns:"
							+ namespaceDef.substring(0, separatorPos) + "=\""
							+ namespaceDef.substring(separatorPos + 1) + "\"";
				}
			}
			log.debug("namespaceClause [" + namespaceClause + "]");
		}
		String soapns = "http://schemas.xmlsoap.org/soap/envelope/";
		if (StringUtils.isNotEmpty(soapNamespace)) {
			soapns = soapNamespace;
		}
		String soapmsg = "<soapenv:Envelope xmlns:soapenv=\"" + soapns + "\""
				+ encodingStyle + targetObjectNamespaceClause +
				// "xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
				// "xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" " +
				namespaceClause + ">" + soapHeader + "<soapenv:Body>"
				+ XmlUtils.skipXmlDeclaration(message) + "</soapenv:Body>"
				+ "</soapenv:Envelope>";
		return soapmsg;
	}

	public String putInEnvelope(String message, String encodingStyleUri) {
		return putInEnvelope(message, encodingStyleUri, null);
	}

	public String createSoapFaultMessage(String faultcode, String faultstring) {
		String faultCdataString = "<![CDATA[" + faultstring + "]]>";
		String fault = "<soapenv:Fault>" + "<faultcode>" + faultcode
				+ "</faultcode>" + "<faultstring>" + faultCdataString
				+ "</faultstring>" + "</soapenv:Fault>";
		return putInEnvelope(fault, null, null, null);
	}

	public String createSoapFaultMessage(String faultstring) {
		return createSoapFaultMessage("soapenv:Server", faultstring);
	}

	public String signMessage(String soapMessage, String user, String password)
			throws SenderException {
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

			String result = DOM2Writer.nodeToString(signedDoc);

			return result;
		} catch (Throwable t) {
			throw new SenderException(t);
		}
	}
}
