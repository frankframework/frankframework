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

public class LdapAttributesParser {

	private LdapAttributesParser() {
		// No-op constructor to make external instance creation impossible. Use the static method to read the attributes.
	}

	public static @Nullable Attributes parseAttributes(Reader reader) throws SAXException, IOException {
		AttributeBuilder attributeBuilder = new AttributeBuilder();
		AttributeXmlParser parser = new AttributeXmlParser(attributeBuilder);
		XmlUtils.parseXml(reader, parser);
		return attributeBuilder.getAttributes();
	}

	private static class AttributeBuilder {
		private @Getter @Nullable Attributes attributes;
		private @Nullable Attribute lastAttribute;

		void addAttribute(@Nonnull String name) {
			if (attributes == null) {
				attributes = new BasicAttributes();
			}
			lastAttribute = new BasicAttribute(name);
			attributes.put(lastAttribute);
		}

		void addValue(@Nonnull String value) {
			if (lastAttribute == null) {
				throw new IllegalStateException("No attribute found");
			}
			lastAttribute.add(value);
		}

		void addNullValue() {
			if (lastAttribute == null) {
				throw new IllegalStateException("No attribute found");
			}
			lastAttribute.add(null);
		}
	}

	private static class AttributeXmlParser extends XMLFilterImpl {
		private final AttributeBuilder attributeBuilder;
		private boolean readingAttributeValue = false;
		private final @Nonnull StringBuilder attributeValue = new StringBuilder();

		private AttributeXmlParser(AttributeBuilder attributeBuilder) {
			this.attributeBuilder = attributeBuilder;
		}

		@Override
		public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) throws SAXException {
			if (localName.equals("attribute")) {
				attributeBuilder.addAttribute(atts.getValue(0));
			} else if (localName.equals("value")) {
				if (readingAttributeValue) {
					throw new SAXException("Invalid XML; Cannot nest value-elements");
				}
				readingAttributeValue = true;
				attributeValue.setLength(0);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			if (readingAttributeValue) {
				attributeValue.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			if (localName.equals("value")) {
				readingAttributeValue = false;
				if (attributeValue.isEmpty()) {
					attributeBuilder.addNullValue();
				} else {
					attributeBuilder.addValue(attributeValue.toString());
				}
				attributeValue.setLength(0);
			}
		}
	}
}
