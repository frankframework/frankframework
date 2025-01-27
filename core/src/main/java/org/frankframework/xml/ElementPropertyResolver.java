/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.xml;

import java.io.IOException;
import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.util.StringResolver;
import org.frankframework.util.XmlUtils;

public class ElementPropertyResolver extends FullXmlFilter {
	private final Properties properties;
	private final StringBuilder pendingSubstBuff = new StringBuilder();

	private boolean collectingBuffer;

	public ElementPropertyResolver(ContentHandler handler, Properties properties) {
		super(handler);
		if(properties == null) {
			throw new IllegalArgumentException("no properties defined");
		}
		this.properties = properties;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (collectingBuffer) {
			pendingSubstBuff.append(ch, start, length);
		} else {
			String characters = new String(ch, start, length);

			if(characters.contains(StringResolver.DELIM_START)) {
				collectingBuffer = true;
				pendingSubstBuff.append(characters);
			} else {
				super.characters(ch, start, length);
			}
		}
	}

	private void substitute() throws SAXException {
		if (collectingBuffer) {
			try {
				XmlUtils.parseNodeSet(StringResolver.substVars(pendingSubstBuff.toString(), properties), getContentHandler());
			} catch (IllegalArgumentException | IOException e) {
				throw new SaxException("Could not substitute", e);
			}
			collectingBuffer=false;
			pendingSubstBuff.setLength(0);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		substitute();
		super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		substitute();
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		substitute();
		super.endElement(uri, localName, qName);
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		substitute();
		super.comment(ch, start, length);
	}
}
