package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.xml.SaxException;

public class XmlJsonWriter extends DefaultHandler implements ContentHandler {

	private Writer writer;
	private boolean commaRequired=false;
	private boolean stringOpen=false;
	
	public XmlJsonWriter(Writer writer) {
		this.writer=writer;
	}
	
	public XmlJsonWriter() {
		this(new StringWriter());
	}
	
	@Override
	public void endDocument() throws SAXException {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startElement(String uri, String localname, String qname, Attributes attrs) throws SAXException {
		try {
			if (commaRequired) {
				writer.write(",");
			}
			commaRequired=false;
			if (attrs!=null) {
				String key=attrs.getValue("key");
				if (key!=null) {
					writer.append('"').append(key).append("\":");
				}
			}
			if (localname.equals("array")) {
				writer.write("[");
			} else if (localname.equals("map")) {
				writer.write("{");
			} else if (localname.equals("null")) {
				writer.write("null");
			} else if (localname.equals("string")) {
				stringOpen=true;
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endElement(String uri, String localname, String qname) throws SAXException {
		try {
			if (localname.equals("array")) {
				writer.write("]");
			} else if (localname.equals("map")) {
				writer.write("}");
			} else if (localname.equals("string")) {
				stringOpen=false;
			}
			commaRequired=true;
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void characters(char[] chars, int start, int length) throws SAXException {
		try {
			if (stringOpen) writer.write('"');
			writer.write(chars, start, length);
			if (stringOpen) writer.write('"');
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public String toString() {
		return writer.toString().trim();
	}

}
