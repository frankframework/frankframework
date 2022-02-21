/*
   Copyright 2022 WeAreFrank!

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

import lombok.SneakyThrows;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Builds a XML-element with attributes and sub-elements. Attributes can be
 * added with the addAttribute method, the content can be set with the setValue
 * method. Subelements can be added with the addSubElement method. the toXML
 * function returns the node and subnodes as an indented xml string.
 * <p/>
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 **/
public class XmlBuilderNew {
	static Logger log = LogUtil.getLogger(XmlBuilderNew.class);
	
	private String root;
	private AttributesImpl attributes = new AttributesImpl();
	private List<XmlBuilderNew> subElements;
	private String text;
	private boolean parseText;
	private boolean cdata;

	@SneakyThrows
	public XmlBuilderNew(String tagName) {
		root = tagName;
	}

	@SneakyThrows
	public void addAttribute(String name, String value) {
		if (value != null) {
			attributes.addAttribute("", name, name, "STRING", value);
		}
	}

	public void addAttribute(String name, boolean value) {
		addAttribute(name, "" + value);
	}

	public void addAttribute(String name, long value) {
		addAttribute(name, "" + value);
	}

	public void addSubElement(String name, String value) {
		XmlBuilderNew subElement = new XmlBuilderNew(name);
		subElement.setValue(value);
		addSubElement(subElement);
	}

	public void addSubElement(XmlBuilderNew newElement) {
		if (subElements==null) {
			if (text!=null) {
				throw new IllegalStateException("XmlBuilder cannot have mixed content, text already set to ["+text+"] when trying to add element");
			}
			subElements = new LinkedList<>();
		}
		subElements.add(newElement);
	}

//	private void addNamespaceRecursive(Element element, Namespace namespace) {
//		if (StringUtils.isEmpty(element.getNamespaceURI())) {
//			element.setNamespace(namespace);
//			List<Element> childList = element.getChildren();
//			if (!childList.isEmpty()) {
//				for (Element child : childList) {
//					addNamespaceRecursive(child, namespace);
//				}
//			}
//		}
//	}

	public void setCdataValue(String value) {
		setValue(value);
		cdata=true;
//		if (value != null) {
//			if (value.contains(CDATA_END)) {
//				int cdata_end_pos;
//				while ((cdata_end_pos=value.indexOf(CDATA_END))>=0) {
//					element.addContent(new CDATA(value.substring(0, cdata_end_pos+1)));
//					value = value.substring(cdata_end_pos+1);
//				}
//				element.addContent(new CDATA(value));
//			} else {
//				element.setContent(new CDATA(value));
//			}
//		}
	}

	public void setValue(String value) {
		if (value != null) {
			text = value;
		}
	}

	public void setValue(String value, boolean encode) {
		setValue(value);
		parseText = !encode && XmlUtils.isWellFormed(value);
	}

//	private Element buildElement(String value) throws JDOMException, IOException {
//		StringReader stringReader = new StringReader(value);
//		SAXBuilder saxBuilder = new SAXBuilder();
//		Document document;
//		document = saxBuilder.build(stringReader);
//		Element element = document.getRootElement();
//		return element.detach();
//	}

	public String toXML() {
		return toXML(false);
	}

	public String toXML(boolean xmlHeader) {
		XmlWriter writer = new XmlWriter();
		try {
			toXML(writer, true);
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
					for(XmlBuilderNew subElement:subElements) {
						subElement.toXML(handler, false);
					}
				}
				if (text!=null) {
					if (parseText) {
						XmlUtils.parseXml(text, handler);
					} else {
						if (cdata && handler instanceof LexicalHandler) {
							((LexicalHandler)handler).startCDATA();
						}
						handler.characters(text.toCharArray(), 0, text.length());
						if (cdata && handler instanceof LexicalHandler) {
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
	
}
