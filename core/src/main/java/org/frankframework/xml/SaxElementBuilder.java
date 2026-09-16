/*
   Copyright 2020-2026 WeAreFrank!

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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

@SuppressWarnings("resource")
public class SaxElementBuilder implements AutoCloseable {

	private final @NonNull ContentHandler handler;
	private @Nullable String elementName;
	private final @Nullable SaxElementBuilder parent;

	private @Nullable AttributesImpl attributes=null;
	private boolean promotedToObject = false;

	public SaxElementBuilder(@NonNull ContentHandler handler) {
		this(null, handler, null);
	}

	public SaxElementBuilder(@Nullable String elementName) {
		this(elementName, new XmlWriter());
	}

	public SaxElementBuilder(@Nullable String elementName, ContentHandler handler) {
		this(elementName, handler, null);
	}

	private SaxElementBuilder(@Nullable String elementName, @NonNull ContentHandler handler, @Nullable SaxElementBuilder parent) {
		this.handler = handler;
		this.elementName = XmlUtils.cleanseElementName(elementName);
		this.parent = parent;
		if (elementName!=null) {
			attributes = new AttributesImpl();
		}
	}

	@Contract("_, _ -> this")
	public SaxElementBuilder addAttribute(String name, int value) throws SAXException {
		return addAttribute(name, Integer.toString(value));
	}

	@Contract("_, _ -> this")
	public SaxElementBuilder addAttribute(String name, String value) throws SAXException {
		if (attributes==null) {
			throw new SaxException("start of element ["+elementName+"] already written");
		}
		String attruri = "";
		String attrlocalName = XmlUtils.cleanseElementName(name);
		@SuppressWarnings("UnnecessaryLocalVariable")
		String attrqName = attrlocalName;
		String attrType = "";
		attributes.addAttribute(attruri, attrlocalName, attrqName, attrType, XmlUtils.normalizeAttributeValue(value));
		return this;
	}

	@Contract("_ -> this")
	public SaxElementBuilder addAttributes(@Nullable Map<String,String> attributes) throws SAXException {
		if (attributes!=null) {
			for(Entry<String,String> entry:attributes.entrySet()) {
				addAttribute(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	private void writePendingStartElement() throws SAXException {
		if (attributes!=null) {
			String uri = "";
			String localName = elementName;
			@SuppressWarnings("UnnecessaryLocalVariable")
			String qName = localName;
			handler.startElement(uri, localName, qName, attributes);
			attributes=null;
		}
	}

	public @Nullable SaxElementBuilder endElement() throws SAXException {
		writePendingStartElement();
		String uri = "";
		String localName = elementName;
		@SuppressWarnings("UnnecessaryLocalVariable")
		String qName = localName;
		handler.endElement(uri, localName, qName);
		elementName = null;
		return parent;
	}

	@Contract("_ -> this")
	public SaxElementBuilder addValue(@Nullable String value) throws SAXException {
		if (StringUtils.isNotEmpty(value)) {
			char[] chars = XmlEncodingUtils.replaceNonValidXmlCharacters(value, '?', true, true).toCharArray();
			addValue(chars, 0, chars.length);
		}
		return this;
	}
	public void addValue(char[] chars, int offset, int len) throws SAXException {
		writePendingStartElement();
		handler.characters(chars, offset, len);
	}

	public @NonNull SaxElementBuilder startElement(@Nullable String elementName) throws SAXException {
		String cleanElementName = XmlUtils.cleanseElementName(elementName);

		if (cleanElementName==null) {
			promotedToObject = true;
			return this;
		}
		writePendingStartElement();
		return new SaxElementBuilder(cleanElementName, handler, this);
	}

	public void addElement(@Nullable String elementName) throws SAXException {
		addElement(elementName, null, null);
	}

	public void addElement(@Nullable String elementName, @Nullable Map<String,String> attributes) throws SAXException {
		addElement(elementName, attributes, null);
	}

	public void addElement(@Nullable String elementName, @Nullable String value) throws SAXException {
		addElement(elementName, null, value);
	}

	public void addElement(@Nullable String elementName, @Nullable Map<String,String> attributes, @Nullable String value) throws SAXException {
		startElement(elementName).addAttributes(attributes).addValue(value).endElement();
	}

	public void addElement(@Nullable String elementName, @Nullable String attributeName, @Nullable String attributeValue, @Nullable String value) throws SAXException {
		startElement(elementName).addAttribute(attributeName, attributeValue).addValue(value).endElement();
	}

	@Override
	public void close() throws SAXException {
		if (promotedToObject) {
			promotedToObject=false;
		} else {
			if (elementName != null) {
				endElement();
			}
		}
	}

	// Package private for now
	@NonNull ContentHandler getHandler() {
		return handler;
	}
}
