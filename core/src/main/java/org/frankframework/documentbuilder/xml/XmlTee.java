/*
   Copyright 2020 WeAreFrank!

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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.frankframework.xml.FullXmlFilter;

public class XmlTee extends FullXmlFilter {

	private ContentHandler handler=null;

	public XmlTee() {
		super();
	}

	public XmlTee(ContentHandler handler, ContentHandler secondHandler) {
		super(handler);
		this.handler = secondHandler;
	}

	public void setSecondContentHandler(ContentHandler handler) {
		this.handler = handler;
	}
	public ContentHandler getSecondContentHandler() {
		return handler;
	}

	@Override
	public void startDocument() throws SAXException {
		if (handler!=null) handler.startDocument();
		super.startDocument();
	}
	@Override
	public void endDocument() throws SAXException {
		if (handler!=null) handler.endDocument();
		super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (handler!=null) handler.startElement(uri, localName, qName, atts);
		super.startElement(uri, localName, qName, atts);
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (handler!=null) handler.endElement(uri, localName, qName);
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (handler!=null) handler.characters(ch, start, length);
		super.characters(ch, start, length);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (handler!=null) handler.startPrefixMapping(prefix, uri);
		super.startPrefixMapping(prefix, uri);
	}
	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (handler!=null) handler.endPrefixMapping(prefix);
		super.endPrefixMapping(prefix);
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.comment(ch, start, length);
		super.comment(ch, start, length);
	}

	@Override
	public void startCDATA() throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.startCDATA();
		super.startCDATA();
	}
	@Override
	public void endCDATA() throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.endCDATA();
		super.endCDATA();
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.startDTD(name, publicId, systemId);
		super.startDTD(name, publicId, systemId);
	}
	@Override
	public void endDTD() throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.endDTD();
		super.endDTD();
	}

	@Override
	public void startEntity(String name) throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.startEntity(name);
		super.startEntity(name);
	}
	@Override
	public void endEntity(String name) throws SAXException {
		if (handler!=null && handler instanceof LexicalHandler lexicalHandler) lexicalHandler.endEntity(name);
		super.endEntity(name);
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (handler!=null) handler.processingInstruction(target, data);
		super.processingInstruction(target, data);
	}

}
