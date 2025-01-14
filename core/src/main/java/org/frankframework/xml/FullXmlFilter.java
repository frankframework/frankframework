/*
   Copyright 2019, 2022 WeAreFrank!

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
package org.frankframework.xml;

import org.apache.logging.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Setter;

import org.frankframework.util.LogUtil;

public class FullXmlFilter extends XMLFilterImpl implements LexicalHandler {
	protected Logger log = LogUtil.getLogger(this);

	private @Setter LexicalHandler lexicalHandler;

	public FullXmlFilter() {
		super();
	}

	public FullXmlFilter(ContentHandler handler) {
		this();
		if (handler!=null) {
			setContentHandler(handler);
		}
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		super.setContentHandler(handler);
		if (handler instanceof LexicalHandler lexicalHandler1) {
			setLexicalHandler (lexicalHandler1);
		}
	}

	@Override
	public void setParent(XMLReader parent) {
		super.setParent(parent);
		setParentsLexicalHandler(parent);
	}
	protected void setParentsLexicalHandler(XMLReader parent) {
		try {
			parent.setProperty("http://xml.org/sax/properties/lexical-handler", this);
		} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
			log.warn("cannot set LexicalHandler to parent [{}]", parent);
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.comment(ch, start, length);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startCDATA();
		}
	}
	@Override
	public void endCDATA() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endCDATA();
		}
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startDTD(name, publicId, systemId);
		}
	}
	@Override
	public void endDTD() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endDTD();
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startEntity(name);
		}
	}
	@Override
	public void endEntity(String name) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endEntity(name);
		}
	}

}
