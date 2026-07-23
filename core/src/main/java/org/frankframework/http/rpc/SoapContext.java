/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.http.rpc;

import java.io.IOException;

import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.soap.filters.SoapAddressingMessageIdExtractor;
import org.frankframework.soap.filters.SoapAddressingRelatesToInjector;
import org.frankframework.soap.filters.SoapNamespaceUriExtractor;
import org.frankframework.soap.filters.ValidateSoapMessageHandler;
import org.springframework.util.MimeType;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlUtils;

@Log4j2
public class SoapContext {
	/**
	 * Get the SOAP protocol version, either SOAP 1.1 or SOAP 1.2.
	 */
	private final @Getter String soapProtocol;

	/**
	 * Get the SOAPAction as HEADER (SOAP 1_1), or from the content-type header (SOAP 1_2).
	 */
	private final @Getter String soapAction;

	/**
	 * Get the MessageID from the SOAP 'ws-addr' header element, or generate a fallback MessageID if not present.
	 */
	private final @Getter String messageId;

	/**
	 * Get the namespace that belongs to the first element in the SOAP body.
	 */
	private final @Getter String namespaceURI;

	private boolean createdMessageId = false;

	public SoapContext(Message body) throws SOAPException {
		// Let's parse the entire request in order to find out the protocol, action e.d.
		// As well as validating that it is a valid SOAP Message.
		SoapNamespaceUriExtractor nsUriHandler = new SoapNamespaceUriExtractor(null);
		SoapAddressingMessageIdExtractor messageIdExtractor = new SoapAddressingMessageIdExtractor(nsUriHandler);
		ValidateSoapMessageHandler validateSoap = new ValidateSoapMessageHandler(messageIdExtractor);
		try {
			XmlUtils.parseXml(body.asInputSource(), validateSoap);
		} catch (IOException | SAXException e) {
			throw new SOAPException("invalid SOAP message", e);
		}

		soapProtocol = validateSoap.getSoapProtocol();
		namespaceURI = nsUriHandler.getNamespaceURI();
		soapAction = getSoapAction(body.getContext());

		// Always return a MessageID, even if it's not in the message itself.
		String inputMessageId = messageIdExtractor.getMessageId();
		messageId = StringUtils.isNotBlank(inputMessageId) ? inputMessageId : generateMessageId();
	}

	private String generateMessageId() {
		createdMessageId = true;
		return MessageUtils.generateMessageId();
	}

	/**
	 * If a MessageID was provided, ensure we return a 'RelatesTo' header conform the ws-addr specification.
	 * If the MessageID is a FallbackID, do nothing.
	 */
	public Message setMessageId(Message output) throws SOAPException {
		if (MessageUtils.isFallbackMessageId(messageId) || createdMessageId) {
			return output;
		}

		try {
			MessageBuilder builder = new MessageBuilder();
			ValidateSoapMessageHandler validateSoap = new ValidateSoapMessageHandler(builder.asXmlWriter());
			SoapAddressingRelatesToInjector soapAddr = new SoapAddressingRelatesToInjector(validateSoap);
			soapAddr.setMessageId(messageId);

			XmlUtils.parseXml(output.asInputSource(), soapAddr);

			return builder.build();
		} catch (IOException | SAXException e) {
			throw new SOAPException("invalid SOAP message", e);
		}
	}

	private String getSoapAction(MessageContext context) {
		if(SOAPConstants.SOAP_1_1_PROTOCOL.equals(soapProtocol)) {
			return (String) context.get("Header.SOAPAction");
		} else {
			MimeType contentType = context.getMimeType();
			if(contentType != null) {
				String action = findAction(contentType);
				if(StringUtils.isNotEmpty(action)) {
					return action;
				}
			}
		}

		log.warn("no SOAPAction found!");
		return "";
	}

	protected static String findAction(MimeType mimeType) {
		return unquote(mimeType.getParameter("action"));
	}

	private static boolean isQuotedString(String s) {
		if (StringUtils.isEmpty(s) || s.length() < 2) {
			return false;
		}
		return ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")));
	}

	private static String unquote(String s) {
		return (isQuotedString(s) ? s.substring(1, s.length() - 1) : s);
	}
}
