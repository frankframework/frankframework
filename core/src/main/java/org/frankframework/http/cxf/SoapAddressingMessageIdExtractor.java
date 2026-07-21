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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.xml.FullXmlFilter;

/** 
 * SAX filter that extracts the MessageID from a SOAP message.
 */
@Log4j2
public class SoapAddressingMessageIdExtractor extends FullXmlFilter {
	private @Getter String messageId;

	private static final String SOAP_ADDR_NS_DOMAIN = "//www.w3.org/";
	private static final String SOAP_ADDR_NS_POSTFIX = "addressing";

	private static final String HEADER = "Header";
	private static final String MESSAGE_ID = "MessageID";

	private final Deque<String> elementStack = new ArrayDeque<>();
	private StringBuilder messageIdBuilder = null;

	public SoapAddressingMessageIdExtractor(ContentHandler contentHandler) {
		super(contentHandler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		final String name = StringUtils.isEmpty(localName) ? qName : localName;
		elementStack.push(name);

		if (hasSoapHeaderMessageIdElement(name) && hasSoapAddressingNamespace(uri)) {
			messageIdBuilder = new StringBuilder();
		}

		super.startElement(uri, localName, qName, attributes);
	}

	private boolean hasSoapHeaderMessageIdElement(String name) {
		return elementStack.size() == 3 && elementStack.contains(HEADER) && MESSAGE_ID.equals(name);
	}

	private static boolean hasSoapAddressingNamespace(String uri) {
		return uri.contains(SOAP_ADDR_NS_DOMAIN) && uri.contains(SOAP_ADDR_NS_POSTFIX);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (messageIdBuilder != null) {
			messageIdBuilder.append(ch, start, length);
		}

		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		elementStack.pop();

		String actual = StringUtils.isEmpty(localName) ? qName : localName;
		if (messageIdBuilder != null && MESSAGE_ID.equals(actual)) {
			messageId = messageIdBuilder.toString();
			messageIdBuilder = null;
		}

		super.endElement(uri, localName, qName);
	}

}
