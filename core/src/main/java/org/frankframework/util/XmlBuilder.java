/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 - 2024 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import org.frankframework.documentbuilder.DocumentBuilderFactory;
import org.frankframework.documentbuilder.IDocumentBuilder;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.XmlWriter;

/**
 * Builds an XML-element with attributes and sub-elements. Attributes can be
 * added with the addAttribute method, the content can be set with the setValue
 * method. Sub elements can be added with the addSubElement method. the toXML
 * function returns the node and sub nodes as an indented xml string.
 * <p/>
 *
 * If possible, use {@link SaxDocumentBuilder} or {@link IDocumentBuilder} (from {@link DocumentBuilderFactory})
 *
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 */
public class XmlBuilder {
	protected Logger log = LogUtil.getLogger(this);

	private static final String CDATA_END = "]]>";

	private final String root;
	private final AttributesImpl attributes = new AttributesImpl();
	private List<XmlBuilder> subElements;
	private String text;
	private boolean parseText;
	private List<String> cdata;

	public XmlBuilder(String tagName) {
		root = XmlUtils.cleanseElementName(tagName);
	}

	public static XmlBuilder create(String tagName) {
		return new XmlBuilder(tagName);
	}

	public void addAttribute(String name, String value) {
		if (value != null) {
			attributes.addAttribute("", name, name, "STRING", XmlUtils.normalizeAttributeValue(value));
		}
	}

	public void addAttribute(String name, boolean value) {
		addAttribute(name, "" + value);
	}

	public void addAttribute(String name, long value) {
		addAttribute(name, "" + value);
	}

	public void addSubElement(String name, String value) {
		XmlBuilder subElement = new XmlBuilder(name);
		subElement.setValue(value);
		addSubElement(subElement);
	}

	public void addSubElement(XmlBuilder newElement) {
		if (subElements == null) {
			if (text != null) {
				throw new IllegalStateException("XmlBuilder cannot have mixed content, text already set to [" + text + "] when trying to add element");
			}
			if (cdata != null) {
				throw new IllegalStateException("XmlBuilder cannot have mixed content, cdata already set when trying to add element");
			}
			subElements = new ArrayList<>();
		}
		subElements.add(newElement);
	}

	public void setCdataValue(String value) {
		text = null;
		cdata = new ArrayList<>();
		if (value != null) {
			int cdata_end_pos;
			while ((cdata_end_pos = value.indexOf(CDATA_END)) >= 0) {
				cdata.add(value.substring(0, cdata_end_pos + 1));
				value = value.substring(cdata_end_pos + 1);
			}
			cdata.add(value);
		}
	}

	public void setValue(String value) {
		cdata=null;
		if (value != null) {
			text = value;
		}
	}

	public void setValue(String value, boolean encode) {
		setValue(value);
		parseText = !encode && XmlUtils.isWellFormed(value);
	}

	public Message asMessage() {
		return new Message(asXmlString(), new MessageContext().withMimeType(MediaType.APPLICATION_XML));
	}

	public String asXmlString() {
		XmlWriter writer = new XmlWriter();
		PrettyPrintFilter handler = new PrettyPrintFilter(writer);
		try {
			handler.startDocument();
			try {
				handleElement(handler);
			} finally {
				handler.endDocument();
			}
		} catch (SAXException | IOException e) {
			log.warn("cannot write XML", e);
			return e.getMessage();
		}
		return writer.toString();
	}

	private void handleElement(ContentHandler handler) throws SAXException, IOException {
		handler.startElement("", root, root, attributes);
		try {
			//Write sub-element
			if (subElements != null) {
				for (XmlBuilder subElement : subElements) {
					subElement.handleElement(handler);
				}
			}

			writeContent(handler);
		} finally {
			handler.endElement(root, text, root);
		}
	}

	private void writeContent(ContentHandler handler) throws IOException, SAXException {
		if (text != null) {
			if (parseText) {
				XmlUtils.parseNodeSet(text, handler);
			} else {
				handler.characters(text.toCharArray(), 0, text.length());
			}
		}
		if (cdata != null) {
			for (String part : cdata) {
				if (handler instanceof LexicalHandler lexicalHandler) {
					lexicalHandler.startCDATA();
				}
				handler.characters(part.toCharArray(), 0, part.length());
				if (handler instanceof LexicalHandler lexicalHandler) {
					lexicalHandler.endCDATA();
				}
			}
		}
	}
}
