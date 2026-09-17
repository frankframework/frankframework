package org.frankframework.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

public class StringBuilderContentHandler extends DefaultHandler implements LexicalHandler {

	private final StringBuilder builder = new StringBuilder();


	@Override
	public void startDocument() {
		builder.append("startDocument\n");
	}

	@Override
	public void endDocument() {
		builder.append("endDocument\n");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) {
		builder.append("startElement ").append(localName).append("\n");
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		builder.append("endElement ").append(localName).append("\n");
	}

	@Override
	public void characters(char[] ch, int offset, int length) {
		builder.append("characters [").append(ch,offset,length).append("]\n");
	}


	@Override
	public void startPrefixMapping(String prefix, String uri) {
		builder.append("startPrefixMapping ").append(prefix).append("=").append(uri).append("\n");
	}

	@Override
	public void endPrefixMapping(String prefix) {
		builder.append("endPrefixMapping ").append(prefix).append("\n");
	}

	@Override
	public void startCDATA() {
		builder.append("startCDATA\n");
	}
	@Override
	public void endCDATA() {
		builder.append("endCDATA\n");
	}

	@Override
	public void comment(char[] ch, int offset, int length) {
		builder.append("comment [").append(ch,offset,length).append("]\n");
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) {
		builder.append("startDTD ").append(name).append(", ").append(publicId).append(", ").append(systemId).append("\n");
	}

	@Override
	public void endDTD() {
		builder.append("endCDATA\n");
	}

	@Override
	public void startEntity(String entity) {
		builder.append("startEntity ").append(entity).append("\n");
	}

	@Override
	public void endEntity(String entity) {
		builder.append("endEntity ").append(entity).append("\n");
	}


	@Override
	public String toString() {
		return builder.toString();
	}
}
