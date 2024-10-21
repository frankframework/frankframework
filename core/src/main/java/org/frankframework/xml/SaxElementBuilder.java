/*
   Copyright 2020-2024 WeAreFrank!

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
import org.frankframework.util.XmlUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SaxElementBuilder implements AutoCloseable {

	private final ContentHandler handler;
	private String elementName;
	private final SaxElementBuilder parent;

	private AttributesImpl attributes=null;
	private boolean promotedToObject = false;

	public SaxElementBuilder(ContentHandler handler) throws SAXException {
		this(null, handler, null);
	}

	public SaxElementBuilder(String elementName) throws SAXException {
		this(elementName, new XmlWriter());
	}

	public SaxElementBuilder(String elementName, ContentHandler handler) throws SAXException {
		this(elementName, handler, null);
	}

	private SaxElementBuilder(String elementName, ContentHandler handler, SaxElementBuilder parent) {
		this.handler = handler;
		this.elementName = XmlUtils.cleanseElementName(elementName);
		this.parent = parent;
		if (elementName!=null) {
			attributes = new AttributesImpl();
		}
	}

	public SaxElementBuilder addAttribute(String name, int value) throws SAXException {
		return addAttribute(name, Integer.toString(value));
	}

	public SaxElementBuilder addAttribute(String name, String value) throws SAXException {
		if (attributes==null) {
			throw new SaxException("start of element ["+elementName+"] already written");
		}
		String attruri = "";
		String attrlocalName = name;
		String attrqName = attrlocalName;
		String attrType = "";
		attributes.addAttribute(attruri, attrlocalName, attrqName, attrType, XmlUtils.normalizeAttributeValue(value));
		return this;
	}

	public SaxElementBuilder addAttributes(Map<String,String> attributes) throws SAXException {
		if (attributes!=null) {
			for(Entry<String,String> entry:attributes.entrySet()) {
				addAttribute(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}

	private SaxElementBuilder writePendingStartElement() throws SAXException {
		if (attributes!=null) {
			String uri = "";
			String localName = elementName;
			String qName = localName;
			handler.startElement(uri, localName, qName, attributes);
			attributes=null;
		}
		return this;
	}

	public SaxElementBuilder endElement() throws SAXException {
		writePendingStartElement();
		String uri = "";
		String localName = elementName;
		String qName = localName;
		handler.endElement(uri, localName, qName);
		elementName = null;
		return parent;
	}

	public SaxElementBuilder addValue(String value) throws SAXException {
		if (StringUtils.isNotEmpty(value)) {
			char[] chars = value.toCharArray();
			addValue(chars, 0, chars.length);
		}
		return this;
	}
	public void addValue(char[] chars, int offset, int len) throws SAXException {
		writePendingStartElement();
		handler.characters(chars, offset, len);
	}

	public SaxElementBuilder startElement(String elementName) throws SAXException {
		String cleanElementName = XmlUtils.cleanseElementName(elementName);

		if (cleanElementName==null) {
			promotedToObject = true;
			return this;
		}
		writePendingStartElement();
		return new SaxElementBuilder(cleanElementName, handler, this);
	}

	public void addElement(String elementName) throws SAXException {
		addElement(elementName, null, null);
	}

	public void addElement(String elementName, Map<String,String> attributes) throws SAXException {
		addElement(elementName, attributes, null);
	}

	public void addElement(String elementName, String value) throws SAXException {
		addElement(elementName, null, value);
	}

	public void addElement(String elementName, Map<String,String> attributes, String value) throws SAXException {
		startElement(elementName).addAttributes(attributes).addValue(value).endElement();
	}

	public void addElement(String elementName, String attributeName, String attributeValue, String value) throws SAXException {
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
	ContentHandler getHandler() {
		return handler;
	}
}
