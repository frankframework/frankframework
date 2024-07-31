/*
   Copyright 2021 WeAreFrank!

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

import static org.frankframework.documentbuilder.xml.JsonXslt3XmlHandler.TARGET_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import org.frankframework.util.StreamUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;


public class JsonXslt3XmlReader implements XMLReader {

	private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
	private static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

	private ErrorHandler errorHandler;
	private ContentHandler contentHandler;
	private EntityResolver entityResolver;
	private DTDHandler dtdHandler;

	private boolean elementEnded = false;

	public JsonXslt3XmlReader() {
		super();
	}

	public JsonXslt3XmlReader(ContentHandler handler) {
		this();
		setContentHandler(handler);
		if (handler instanceof ErrorHandler errorHandler1) {
			setErrorHandler(errorHandler1);
		}
	}

	public boolean parse(String key, JsonParser parser) throws IOException, SAXException {
		Event event = parser.next();
		if (event == Event.START_OBJECT) {
			startElement("map", key);
			while ((event = parser.next()) != Event.END_OBJECT) {
				if (event != Event.KEY_NAME) {
					throw new SAXException("expected key name at " + parser.getLocation());
				}
				key = parser.getString();
				parse(key, parser);
			}
			endElement("map");
		} else if (event == Event.END_ARRAY) {
			return false;
		} else if (event == Event.START_ARRAY) {
			startElement("array", key);
			while (parse(null, parser)) ; // parse array elements until the close
			endElement("array");
		} else {
			if (event == Event.VALUE_NULL) {
				simpleElement("null", key, null);
			} else {
				if (event == Event.VALUE_FALSE) {
					simpleElement("boolean", key, "false");
				} else {
					if (event == Event.VALUE_TRUE) {
						simpleElement("boolean", key, "true");
					} else {
						String value = parser.getString();
						if (event == Event.VALUE_NUMBER) {
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
		ContentHandler ch = getContentHandler();
		ch.characters("\n".toCharArray(), 0, 1);
	}

	private void addAttribute(AttributesImpl attr, String name, String value) {
		attr.addAttribute("", name, name, "", value); // Saxon requires type to be not null
	}

	private void startElement(String typename, String key) throws SAXException {
		startElement(typename, key, null, null);
	}

	private void startElement(String typename, String key, String attrName, String attrValue) throws SAXException {
		ContentHandler ch = getContentHandler();
		AttributesImpl attr = new AttributesImpl(); // Saxon requires attr to be not null
		if (key != null) {
			addAttribute(attr, "key", key);
		}
		if (attrName != null) {
			addAttribute(attr, attrName, attrValue);
		}
		newLine();
		ch.startElement(TARGET_NAMESPACE, typename, typename, attr);
		elementEnded = false;
	}

	private void endElement(String typename) throws SAXException {
		if (elementEnded) {
			newLine();
		}
		ContentHandler ch = getContentHandler();
		ch.endElement(TARGET_NAMESPACE, typename, typename);
		elementEnded = true;
	}

	private void simpleElement(String typename, String key, String value) throws SAXException {
		startElement(typename, key);
		if (value != null) getContentHandler().characters(value.toCharArray(), 0, value.length());
		endElement(typename);
	}


	@Override
	public void parse(InputSource input) throws IOException, SAXException {
		ContentHandler ch = getContentHandler();
		ch.startDocument();
		ch.startPrefixMapping("", TARGET_NAMESPACE);
		Reader reader = input.getCharacterStream();
		if (reader == null) {
			InputStream stream = input.getByteStream();
			if (stream != null) {
				reader = StreamUtil.getCharsetDetectingInputStreamReader(stream);
			}
		}
		if (reader == null) {
			throw new IOException("unable to read data from InputSource");
		}
		parse(null, Json.createParser(reader));
		ch.endPrefixMapping("");
		ch.endDocument();
	}

	@Override
	public void parse(String systemId) throws IOException, SAXException {
		parse(new InputSource(systemId));
	}

	@Override
	public boolean getFeature(String name) throws SAXNotRecognizedException {
		throw new SAXNotRecognizedException("Feature not recognized [" + name + "]");
	}

	@Override
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
		if (name.equals(FEATURE_NAMESPACES)) {
			if (!value) {
				throw new SAXNotRecognizedException("Cannot set feature [" + name + "] to false");
			}
		} else if (name.equals(FEATURE_NAMESPACE_PREFIXES)) {
			if (value) {
				throw new SAXNotRecognizedException("Cannot set feature [" + name + "] to true");
			}
		} else {
			throw new SAXNotRecognizedException("Feature not recognized [" + name + "]");
		}
	}

	@Override
	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException("Property not recognized [" + name + "]");
	}

	@Override
	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException("Property not recognized [" + name + "]");
	}


	@Override
	public void setEntityResolver(EntityResolver resolver) {
		entityResolver = resolver;
	}

	@Override
	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	@Override
	public void setDTDHandler(DTDHandler handler) {
		dtdHandler = handler;
	}

	@Override
	public DTDHandler getDTDHandler() {
		return dtdHandler;
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		contentHandler = handler;
	}

	@Override
	public ContentHandler getContentHandler() {
		return contentHandler;
	}

	@Override
	public void setErrorHandler(ErrorHandler handler) {
		errorHandler = handler;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

}
