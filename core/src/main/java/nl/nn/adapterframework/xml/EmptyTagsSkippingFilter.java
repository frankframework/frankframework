/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

public class EmptyTagsSkippingFilter extends XMLFilterImpl implements LexicalHandler {
//	protected Logger log = LogUtil.getLogger(this);
	
	private LexicalHandler lexicalHandler;
	
	public EmptyTagsSkippingFilter() {
		super();
	}
		
	public EmptyTagsSkippingFilter(XMLReader xmlReader) {
		super(xmlReader);
	}
	
	private boolean topLevel=true;
	private String savedUri;
	private String savedLocalName;
	private String savedQName;
	private Attributes savedAttributes;

	private void checkPendingStartElement() throws SAXException {
		if (savedLocalName!=null) {
//			log.debug("writing delayed startElement ["+savedLocalName+"]");
			super.startElement(savedUri, savedLocalName, savedQName, savedAttributes);
			savedLocalName=null;
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
//		log.debug("startElement ["+localName+"]");
		checkPendingStartElement();
		if (atts.getLength()>0 || topLevel) {
			topLevel=false;
//			log.debug("writing ["+localName+"]");
			super.startElement(uri, localName, qName, atts);
		} else {
			//log.debug("saving ["+localName+"]");
			savedUri=uri;
			savedLocalName=localName;
			savedQName=qName;
			savedAttributes=atts;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
//		log.debug("endElement ["+localName+"]");
		if (savedLocalName!=null) {
//			log.debug("skipping element ["+savedLocalName+"]");
			savedLocalName=null; // skip element if it has no content
		} else {
//			log.debug("writing endElement ["+localName+"]");
			super.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		checkPendingStartElement();
//		log.debug("writing characters");
		super.characters(ch, start, length);
	}

	
	@Override
	public void setContentHandler(ContentHandler handler) {
		super.setContentHandler(handler);
		if (handler instanceof LexicalHandler) {
			lexicalHandler=(LexicalHandler)handler;
		}
	}

	
	@Override
	public void comment(char[] arg0, int arg1, int arg2) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.comment(arg0, arg1, arg2);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		if (lexicalHandler!=null) {
			checkPendingStartElement();
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
	public void startDTD(String arg0, String arg1, String arg2) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startDTD(arg0, arg1, arg2);
		}
	}
	@Override
	public void endDTD() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endDTD();
		}
	}

	@Override
	public void endEntity(String arg0) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endEntity(arg0);
		}
	}


	@Override
	public void startEntity(String arg0) throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startEntity(arg0);
		}
	}


}

