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

public class NamespaceRemovingFilter extends FullXmlFilter {
	
	private LexicalHandler lexicalHandler;
	
	public NamespaceRemovingFilter() {
		super();
	}
		
	public NamespaceRemovingFilter(XMLReader xmlReader) {
		super(xmlReader);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
//		log.debug("startElement("+uri+","+localName+","+qName+")");
		super.startElement("", localName, localName, new NamespaceRemovingAttributesWrapper(atts));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		//log.debug("endElement("+uri+","+localName+","+qName+")");
		super.endElement("", localName, localName);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		//log.debug("startPrefixMapping("+prefix+","+uri+")");
		//super.startPrefixMapping(prefix, "");
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		//log.debug("endPrefixMapping("+prefix+")");
		//super.endPrefixMapping(prefix);
	}

	@Override
	public void comment(char[] arg0, int arg1, int arg2) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endCDATA() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.endCDATA();
		}
	}

	@Override
	public void endDTD() throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endEntity(String arg0) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startCDATA() throws SAXException {
		if (lexicalHandler!=null) {
			lexicalHandler.startCDATA();
		}
	}

	@Override
	public void startDTD(String arg0, String arg1, String arg2) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startEntity(String arg0) throws SAXException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		super.setContentHandler(handler);
		if (handler instanceof LexicalHandler) {
			lexicalHandler=(LexicalHandler)handler;
		}
	}

}

