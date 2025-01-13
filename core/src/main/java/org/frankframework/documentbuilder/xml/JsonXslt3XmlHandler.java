/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.documentbuilder.xml;

import java.io.IOException;

import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.documentbuilder.JsonEventHandler;


public class JsonXslt3XmlHandler implements JsonEventHandler {

	static final String TARGET_NAMESPACE = "http://www.w3.org/2013/XSL/json";

	private @Getter @Setter ContentHandler contentHandler;

	private boolean elementEnded=false;
	private String parsedKey=null;

	public JsonXslt3XmlHandler() {
		super();
	}

	public JsonXslt3XmlHandler(ContentHandler handler) {
		this();
		setContentHandler(handler);
	}

	public boolean parse(String key, JsonParser parser) throws IOException, SAXException {
		Event event = parser.next();
		if (event == Event.START_OBJECT) {
			startElement("map", key);
			while ((event = parser.next())!=Event.END_OBJECT) {
				if (event!=Event.KEY_NAME) {
					throw new SAXException("expected key name at "+parser.getLocation());
				}
				key=parser.getString();
				parse(key,parser);
			};
			endElement("map");
		} else if (event==Event.END_ARRAY) {
			return false;
		} else if (event==Event.START_ARRAY) {
			startElement("array", key);
			while (parse(null,parser)); // parse array elements until the close
			endElement("array");
		} else {
			if (event==Event.VALUE_NULL) {
				simpleElement("null", key, null);
			} else {
				if (event==Event.VALUE_FALSE) {
					simpleElement("boolean", key, "false");
				} else {
					if (event==Event.VALUE_TRUE) {
						simpleElement("boolean", key, "true");
					} else {
						String value =parser.getString();
						if (event==Event.VALUE_NUMBER) {
							simpleElement("number", key, value);
						} else {
							simpleElement("string", key, value);
						}
					}
				}
			}
		}
		return true;
	}

	private void newLine() throws SAXException {
		ContentHandler ch=getContentHandler();
		ch.characters("\n".toCharArray(), 0, 1);
	}

	private void addAttribute(AttributesImpl attr, String name, String value) {
		attr.addAttribute("", name, name, "", value); // Saxon requires type to be not null
	}
	private void startElement(String typename, String key) throws SAXException {
		startElement(typename, key, null, null);
	}
	private void startElement(String typename, String key, String attrName, String attrValue) throws SAXException {
		ContentHandler ch=getContentHandler();
		AttributesImpl attr=new AttributesImpl(); // Saxon requires attr to be not null
		if (key!=null) {
			addAttribute(attr, "key", key);
		}
		if (attrName!=null) {
			addAttribute(attr, attrName, attrValue);
		}
		newLine();
		ch.startElement(TARGET_NAMESPACE, typename, typename, attr);
		elementEnded=false;
	}
	private void endElement(String typename) throws SAXException {
		if (elementEnded) {
			newLine();
		}
		ContentHandler ch=getContentHandler();
		ch.endElement(TARGET_NAMESPACE, typename, typename);
		elementEnded=true;
	}

	private void simpleElement(String typename, String key, Object value) throws SAXException {
		startElement(typename, key);
		if (value!=null) {
			String valueString = value.toString();
			getContentHandler().characters(valueString.toCharArray(), 0, valueString.length());
		}
		endElement(typename);
	}



	@Override
	public void startDocument() throws SAXException {
		contentHandler.startDocument();
		contentHandler.startPrefixMapping("", TARGET_NAMESPACE);
	}

	@Override
	public void endDocument() throws SAXException {
		contentHandler.endPrefixMapping("");
		contentHandler.endDocument();
	}

	@Override
	public void startObject() throws SAXException {
		startElement("map", parsedKey);
	}

	@Override
	public void startObjectEntry(String key) throws SAXException {
		parsedKey=key;
	}

	@Override
	public void endObject() throws SAXException {
		endElement("map");
	}

	@Override
	public void startArray() throws SAXException {
		startElement("array", parsedKey);
	}

	@Override
	public void endArray() throws SAXException {
		endElement("array");
	}

	@Override
	public void primitive(Object value) throws SAXException {
		if (value == null) {
			simpleElement("null", parsedKey, value);
		} else {
			if (value instanceof Number) {
				simpleElement("number", parsedKey, value);
			} else {
				if (value instanceof Boolean) {
					simpleElement("boolean", parsedKey, value);
				} else {
					simpleElement("string", parsedKey, value);
				}
			}
		}
	}

	@Override
	public void number(String value) throws SAXException {
		if (value == null) {
			simpleElement("null", parsedKey, value);
		} else {
			simpleElement("number", parsedKey, value);
		}
	}

}
