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

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.xml.FullXmlFilter;

/**
 * SAX filter that extracts the namespaceURI and MessageID from a SOAP message.
 */
@Log4j2
public class SoapNamespaceUriExtractor extends FullXmlFilter {
	private @Getter String namespaceURI;

	private static final String BODY = "Body";

	private final Deque<String> elementStack = new ArrayDeque<>();

	public SoapNamespaceUriExtractor(@Nullable ContentHandler contentHandler) {
		super(contentHandler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		final String name = StringUtils.isEmpty(localName) ? qName : localName;
		elementStack.push(name);

		// Extract the namespace URI of the first SOAP:Body element.
		if (elementStack.size() == 3 && elementStack.contains(BODY)) {
			namespaceURI = uri;
		}

		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		elementStack.pop();
		super.endElement(uri, localName, qName);
	}

}
