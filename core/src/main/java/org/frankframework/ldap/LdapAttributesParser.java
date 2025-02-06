/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.ldap;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;

import org.frankframework.util.XmlUtils;

public class LdapAttributesParser extends XMLFilterImpl {
	private @Getter @Nullable Attributes attributes;
	private Attribute lastAttribute;
	private boolean readingAttributeValue = false;
	private final @Nonnull StringBuilder attributeValue = new StringBuilder();

	private LdapAttributesParser() {
		// No-op constructor to make external instance creation impossible. Use the static method to read the attributes.
	}

	@Override
	public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
		switch (localName) {
			case "attributes" -> attributes = new BasicAttributes();
			case "attribute" -> {
				Objects.requireNonNull(attributes, "Invalid XML; element 'attribute' did not have parent 'attributes'");
				lastAttribute = new BasicAttribute(atts.getValue(0));
				attributes.put(lastAttribute);
			}
			case "value" -> {
				if (readingAttributeValue) {
					throw new SAXException("Invalid XML; Cannot nest value-elements");
				}
				readingAttributeValue = true;
				attributeValue.setLength(0);
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (readingAttributeValue) {
			attributeValue.append(ch, start, length);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (localName) {
			case "attribute" -> lastAttribute = null;
			case "value" -> {
				readingAttributeValue = false;
				if (attributeValue.isEmpty()) {
					lastAttribute.add(null);
				} else {
					lastAttribute.add(attributeValue.toString());
				}
				attributeValue.setLength(0);
			}
		}
	}

	public static @Nullable Attributes parseAttributes(Reader reader) throws SAXException, IOException {
		LdapAttributesParser parser = new LdapAttributesParser();
		XmlUtils.parseXml(reader, parser);
		return parser.getAttributes();
	}
}
