package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

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


public class JsonXmlReader implements XMLReader {

//	private String atttributePrefix="@";
	private String TARGETNAMESPACE="http://www.w3.org/2013/XSL/json";
	
	private String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
	private String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
	
	private ErrorHandler errorHandler;
	private ContentHandler contentHandler;
	private EntityResolver entityResolver;
	private DTDHandler dtdHandler;
	
	private boolean elementEnded=false;
	
	public JsonXmlReader() {
		super();
	}

	public JsonXmlReader(ContentHandler handler) {
		this();
		setContentHandler(handler);
		if (handler instanceof ErrorHandler) {
			setErrorHandler((ErrorHandler)handler);
		}
	}
	
	public boolean parse(String key, JsonParser parser) throws IOException, SAXException {
		Event event = parser.next();
		if (event.equals(Event.START_OBJECT)) {
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
		ch.startElement(TARGETNAMESPACE, typename, typename, attr);
		elementEnded=false;
	}
	private void endElement(String typename) throws SAXException {
		if (elementEnded) {
			newLine();
		}
		ContentHandler ch=getContentHandler();
		ch.endElement(TARGETNAMESPACE, typename, typename);
		elementEnded=true;
	}

	private void simpleElement(String typename, String key, String value) throws SAXException {
		startElement(typename, key);
		if (value!=null) getContentHandler().characters(value.toCharArray(), 0, value.length());
		endElement(typename);
	}
	
	

	@Override
	public void parse(InputSource input) throws IOException, SAXException {
		ContentHandler ch=getContentHandler();
		ch.startDocument();
		ch.startPrefixMapping("", TARGETNAMESPACE);
		parse(null, Json.createParser(input.getCharacterStream()));
		ch.endPrefixMapping("");
		ch.endDocument();
	}

	@Override
	public void parse(String systemId) throws IOException, SAXException {
		parse(new InputSource(systemId));
	}

	
	
	@Override
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException("Feature not recognized ["+name+"]");
	}

	@Override
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		if (name.equals(FEATURE_NAMESPACES)) {
			if (!value) {
				throw new SAXNotRecognizedException("Cannot set feature ["+name+"] to false");
			}
		} else if (name.equals(FEATURE_NAMESPACE_PREFIXES)) {
			if (value) {
				throw new SAXNotRecognizedException("Cannot set feature ["+name+"] to true");
			}
		} else {
			throw new SAXNotRecognizedException("Feature not recognized ["+name+"]");
		}
	}

	@Override
	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException("Protperty not recognized ["+name+"]");
	}

	@Override
	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
		throw new SAXNotRecognizedException("Protperty not recognized ["+name+"]");
	}


	@Override
	public void setEntityResolver(EntityResolver resolver) {
		entityResolver=resolver;
	}
	@Override
	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	@Override
	public void setDTDHandler(DTDHandler handler) {
		dtdHandler=handler;
	}
	@Override
	public DTDHandler getDTDHandler() {
		return dtdHandler;
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		contentHandler=handler;
	}
	@Override
	public ContentHandler getContentHandler() {
		return contentHandler;
	}

	@Override
	public void setErrorHandler(ErrorHandler handler) {
		errorHandler=handler;
	}
	@Override
	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

}
