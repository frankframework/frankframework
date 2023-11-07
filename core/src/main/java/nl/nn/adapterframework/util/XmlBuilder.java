/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import nl.nn.adapterframework.stream.document.DocumentBuilderFactory;
import nl.nn.adapterframework.stream.document.IDocumentBuilder;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Builds a XML-element with attributes and sub-elements. Attributes can be
 * added with the addAttribute method, the content can be set with the setValue
 * method. Subelements can be added with the addSubElement method. the toXML
 * function returns the node and subnodes as an indented xml string.
 * <p/>
 * 
 * @deprecated Please replace with {@link SaxDocumentBuilder} or {@link IDocumentBuilder} (from {@link DocumentBuilderFactory})
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 * 
 **/
@Deprecated
public class XmlBuilder {
	protected Logger log = LogUtil.getLogger(this);

	private final String CDATA_END="]]>";

	private String root;
	private AttributesImpl attributes = new AttributesImpl();
	private List<XmlBuilder> subElements;
	private String text;
	private boolean parseText;
	private List<String> cdata;

	public XmlBuilder(String tagName) {
		root = tagName;
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
		if (subElements==null) {
			if (text!=null) {
				throw new IllegalStateException("XmlBuilder cannot have mixed content, text already set to ["+text+"] when trying to add element");
			}
			if (cdata!=null) {
				throw new IllegalStateException("XmlBuilder cannot have mixed content, cdata already set when trying to add element");
			}
			subElements = new LinkedList<>();
		}
		subElements.add(newElement);
	}

	public void setCdataValue(String value) {
		text=null;
		cdata=new LinkedList<>();
		if (value!=null) {
			int cdata_end_pos;
			while ((cdata_end_pos=value.indexOf(CDATA_END))>=0) {
				cdata.add(value.substring(0, cdata_end_pos+1));
				value = value.substring(cdata_end_pos+1);
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

	public String toXML() {
		return toXML(false);
	}

	public String toXML(boolean xmlHeader) {
		XmlWriter writer = new XmlWriter();
		PrettyPrintFilter ppf = new PrettyPrintFilter(writer);
		try {
			toXML(ppf);
		} catch (SAXException | IOException e) {
			log.warn("cannot write XML", e);
			return e.getMessage();
		}
		return writer.toString();
	}

	public void toXML(ContentHandler handler) throws SAXException, IOException {
		toXML(handler, true);
	}

	public void toXML(ContentHandler handler, boolean asDocument) throws SAXException, IOException {
		if (asDocument) {
			handler.startDocument();
		}
		try {
			handler.startElement("", root, root, attributes);
			try {
				if (subElements!=null) {
					for(XmlBuilder subElement:subElements) {
						subElement.toXML(handler, false);
					}
				}
				if (text!=null) {
					if (parseText) {
						XmlUtils.parseNodeSet(text, handler);
					} else {
						handler.characters(text.toCharArray(), 0, text.length());
					}
				}
				if (cdata!=null) {
					for(String part:cdata) {
						if (handler instanceof LexicalHandler) {
							((LexicalHandler)handler).startCDATA();
						}
						handler.characters(part.toCharArray(), 0, part.length());
						if (handler instanceof LexicalHandler) {
							((LexicalHandler)handler).endCDATA();
						}
					}
				}
			} finally {
				handler.endElement(root, text, root);
			}
		} finally {
			if (asDocument) {
				handler.endDocument();
			}
		}
	}

	@Override
	public String toString() {
		return toXML();
	}
}
