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
package org.frankframework.http.cxf;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import lombok.extern.log4j.Log4j2;

import org.frankframework.xml.FullXmlFilter;

/** 
 * SAX filter that injects a SOAP:Header with a RelatesTo element containing the
 * reply messageId if it is not already present in the SOAP message.
 */
@Log4j2
public class SoapAddressingRelatesToInjector extends FullXmlFilter {

	private static final String DEFAULT_SOAP_ADDR_NS = "https://www.w3.org/2006/03/addressing/ws-addr.xsd";
	private static final String SOAP_ADDR_NS_PREFIX = "wsa";
	private static final String SOAP_ADDR_NS_DOMAIN = "//www.w3.org/";
	private static final String SOAP_ADDR_NS_POSTFIX = "addressing";

	private static final String ENVELOPE = "Envelope";
	private static final String HEADER = "Header";
	private static final String RELATES_TO = "RelatesTo";

	private final Deque<String> elementStack = new ArrayDeque<>();

	private boolean headerSeen = false;
	private boolean foundRelatesTo = false;
	private boolean writtenRelatesTo = false;
	private String messageId;

	public SoapAddressingRelatesToInjector(@NonNull ContentHandler contentHandler) {
		super(contentHandler);
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		final String name = StringUtils.isEmpty(localName) ? qName : localName;
		elementStack.push(name);

		// Stack is 2, either a SOAP:Header or a SOAP:Body.
		if (elementStack.size() == 2) {
			if (HEADER.equals(name)) {
				headerSeen = true;
			}
			if (!headerSeen) {
				// If we've not seen a SOAP:Header it must be the body without a header (valid SOAP).
				createSoapHeader(uri, StringUtils.substringBefore(qName, ":"));
			}
		}

		// We're looking for a RelatesTo element in the SOAP:Header, which is 3 levels deep.
		if (hasSoapHeaderRelatesToElement(name) && hasSoapAddressingNamespace(uri)) {
			// We've found a RelatesTo element in the SOAP:Header, no need to inject one.
			foundRelatesTo = true;
		}

		super.startElement(uri, localName, qName, attributes);
	}

	private boolean hasSoapHeaderRelatesToElement(String name) {
		return elementStack.size() == 3 && elementStack.contains(HEADER) && RELATES_TO.equals(name);
	}

	private static boolean hasSoapAddressingNamespace(String uri) {
		return uri.contains(SOAP_ADDR_NS_DOMAIN) && uri.contains(SOAP_ADDR_NS_POSTFIX);
	}

	/**
	 * Creates a SOAP:Header element with a RelatesTo child element containing the messageId.
	 */
	private void createSoapHeader(String soapEnvUri, String soapEnvQNamePrefix) throws SAXException {
		// The soapEnvQNamePrefix is the prefix used in the SOAP envelope, e.g. "soapenv" or "soap".
		// This qName must match the prefix used in the SOAP envelope, otherwise the SOAP message will be invalid.
		String headerQName = soapEnvQNamePrefix + ":" + HEADER;
		try {
			// Write the SOAP:Header element.
			super.startElement(soapEnvUri, HEADER, headerQName, new AttributesImpl());

			// Write the RelatesTo element.
			writeRelatesToElement();

			// Finish off with the SOAP:Header end element.
			super.endElement(soapEnvUri, HEADER, headerQName);
		} catch (SAXException e) {
			throw new SAXException("unable to inject SOAP:Header", e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (foundRelatesTo && length > 0) {
			// We may have found a RelatesTo element, but if it has no content, we still need to write the messageId to it.
			writtenRelatesTo = true;
		}
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		elementStack.pop();
		String name = StringUtils.isEmpty(localName) ? qName : localName;

		// We've found a RelatesTo element, no need to do anything.
		if (foundRelatesTo && RELATES_TO.equals(name)) {
			// Unless no data was written to it (empty element).
			writeRelatesToId();
		}
		if (!foundRelatesTo && elementStack.size() == 1 && ENVELOPE.equals(elementStack.peek())) {
			// We're about to close the SOAP:Header and we didn't find a RelatesTo element, so we need to inject it.
			writeRelatesToElement();
		}

		super.endElement(uri, localName, qName);
	}

	private void writeRelatesToId() throws SAXException {
		if (!writtenRelatesTo) {
			char[] idToWrite = messageId.toCharArray();
			super.characters(idToWrite, 0, idToWrite.length);
		}

		writtenRelatesTo = true;
	}

	/**
	 * Writes the {@code <RelatesTo>} element with the messageId as its content.
	 * Uses the 'WSA' namespace prefix.
	 */
	private void writeRelatesToElement() throws SAXException {
		// Use the WSA namespacePrefix
		super.startPrefixMapping(SOAP_ADDR_NS_PREFIX, DEFAULT_SOAP_ADDR_NS);
		super.startElement(DEFAULT_SOAP_ADDR_NS, RELATES_TO, SOAP_ADDR_NS_PREFIX+":"+RELATES_TO, new AttributesImpl());

		writeRelatesToId();

		super.endElement(DEFAULT_SOAP_ADDR_NS, RELATES_TO, SOAP_ADDR_NS_PREFIX+":"+RELATES_TO);
		super.endPrefixMapping(SOAP_ADDR_NS_PREFIX);
	}

}
