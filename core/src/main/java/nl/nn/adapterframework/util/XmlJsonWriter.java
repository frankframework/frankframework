package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlJsonWriter extends DefaultHandler {

	StringBuffer buffer=new StringBuffer();
	boolean commaRequired=false;
	boolean stringOpen=false;
	
	@Override
	public void startElement(String uri, String localname, String qname, Attributes attrs) throws SAXException {
		if (commaRequired) {
			buffer.append(",");
		}
		commaRequired=false;
		if (attrs!=null) {
			String key=attrs.getValue("key");
			if (key!=null) {
				buffer.append('"').append(key).append("\":");
			}
		}
		if (localname.equals("array")) {
			buffer.append("[");
		} else if (localname.equals("map")) {
			buffer.append("{");
		} else if (localname.equals("null")) {
			buffer.append("null");
		} else if (localname.equals("string")) {
			stringOpen=true;
		}
	}

	@Override
	public void endElement(String uri, String localname, String qname) throws SAXException {
		if (localname.equals("array")) {
			buffer.append("]");
		} else if (localname.equals("map")) {
			buffer.append("}");
		} else if (localname.equals("string")) {
			stringOpen=false;
		}
		commaRequired=true;
	}

	@Override
	public void characters(char[] chars, int start, int length) throws SAXException {
		if (stringOpen) buffer.append('"');
		buffer.append(chars, start, length);
		if (stringOpen) buffer.append('"');
	}

	@Override
	public String toString() {
		return buffer.toString().trim();
	}

}
