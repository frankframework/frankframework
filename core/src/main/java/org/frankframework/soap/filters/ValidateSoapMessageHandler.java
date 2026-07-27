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
package org.frankframework.soap.filters;

import java.util.ArrayDeque;
import java.util.Deque;

import jakarta.xml.soap.SOAPConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.xml.FullXmlFilter;

/**
 * SAX filter that validates that the SOAP message is well-formed and follows the SOAP specification.
 */
@Log4j2
public class ValidateSoapMessageHandler extends FullXmlFilter {
	private @Getter String soapProtocol;

	private static final String SOAP11_NS = "//schemas.xmlsoap.org/soap/envelope";
	private static final String SOAP12_NS = "//www.w3.org/2003/05/soap-envelope";

	private static final String ENVELOPE = "Envelope";
	private static final String HEADER = "Header";
	private static final String BODY = "Body";

	private final Deque<String> elementStack = new ArrayDeque<>();

	private String soapNamespace; // Detected from Envelope
	private boolean seenEnvelope = false;
	private boolean seenHeader = false;
	private boolean seenBody = false;

	public ValidateSoapMessageHandler(@Nullable ContentHandler contentHandler) {
		super(contentHandler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		final String name = StringUtils.isEmpty(localName) ? qName : localName;
		elementStack.push(name);

		// Root must be SOAP Envelope in SOAP 1.1 or 1.2 namespace
		if (elementStack.size() == 1) {
			if (!ENVELOPE.equals(name)) {
				throw new SAXException("Root element must be soap:Envelope, but found: " + name);
			}

			String uriWithoutProtocolAndLastSlash = Strings.CI.removeEnd(StringUtils.substringAfter(uri, ":"), "/");
			if (SOAP11_NS.equals(uriWithoutProtocolAndLastSlash)) {
				soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;
			} else if (SOAP12_NS.equals(uriWithoutProtocolAndLastSlash)) {
				soapProtocol = SOAPConstants.SOAP_1_2_PROTOCOL;
			} else {
				throw new SAXException("Envelope namespace must be SOAP 1.1 or 1.2, but found: " + uri);
			}

			soapNamespace = uri;
			seenEnvelope = true;
		} else if (elementStack.size() == 2) {
			validateElementNameAndNamespace(uri, name);
		}

		super.startElement(uri, localName, qName, attributes);
	}

	// Direct child of Envelope can be Header (optional) or Body (required)
	private void validateElementNameAndNamespace(String uri, String name) throws SAXException {
		if (!soapNamespace.equals(uri)) {
			throw new SAXException("Direct children of Envelope must use the same SOAP namespace: " + soapNamespace);
		}

		if (HEADER.equals(name)) {
			if (seenHeader) {
				throw new SAXException("SOAP Envelope can contain at most one Header.");
			}
			if (seenBody) {
				throw new SAXException("SOAP Header must appear before Body.");
			}
			seenHeader = true;
			return;
		}

		if (BODY.equals(name)) {
			if (seenBody) {
				throw new SAXException("SOAP Envelope can contain at most one Body.");
			}
			seenBody = true;
			return;
		}

		throw new SAXException("Invalid element under Envelope: " + name + ". Only Header (optional) and Body (required) are allowed.");
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String expected = elementStack.pop();
		String actual = StringUtils.isEmpty(localName) ? qName : localName;
		if (!expected.equals(actual)) {
			throw new SAXException("Malformed XML nesting. Expected closing for " + expected + " but found " + actual);
		}

		super.endElement(uri, localName, qName);
	}

	@Override
	public void endDocument() throws SAXException {
		if (!seenEnvelope) {
			throw new SAXException("Missing SOAP Envelope.");
		}
		if (!seenBody) {
			throw new SAXException("Missing required SOAP Body.");
		}

		super.endDocument();
	}

}
