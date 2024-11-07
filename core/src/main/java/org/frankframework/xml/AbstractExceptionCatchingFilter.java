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
package org.frankframework.xml;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class AbstractExceptionCatchingFilter extends FullXmlFilter {

	protected AbstractExceptionCatchingFilter(ContentHandler handler) {
		super(handler);
	}

	protected abstract void handleException(Exception e) throws SAXException;

	@Override
	public void startDocument() throws SAXException {
		try {
			super.startDocument();
		} catch (SAXException e) {
			handleException(e);
		}
	}
	@Override
	public void endDocument() throws SAXException {
		try {
			super.endDocument();
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		try {
			super.startElement(uri, localName, qName, atts);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			super.characters(ch, start, length);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			super.endElement(uri, localName, qName);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		try {
			super.comment(ch, start, length);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		try {
			super.startCDATA();
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		try {
			super.endCDATA();
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		try {
			super.startDTD(name, publicId, systemId);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void endDTD() throws SAXException {
		try {
			super.endDTD();
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		try {
			super.startEntity(name);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void endEntity(String name) throws SAXException {
		try {
			super.endEntity(name);
		} catch (Exception e) {
			handleException(e);
		}
	}


	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		try {
			super.startPrefixMapping(prefix, uri);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		try {
			super.endPrefixMapping(prefix);
		} catch (Exception e) {
			handleException(e);
		}
	}


	@Override
	public void warning(SAXParseException e) throws SAXException {
		try {
			super.warning(e);
		} catch (Exception se) {
			handleException(se);
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		try {
			super.error(e);
		} catch (Exception se) {
			handleException(se);
		}
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		try {
			super.fatalError(e);
		} catch (Exception se) {
			handleException(se);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		try {
			super.ignorableWhitespace(ch, start, length);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void notationDecl(String name, String publicId, String systemId) throws SAXException {
		try {
			super.notationDecl(name, publicId, systemId);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		try {
			super.processingInstruction(target, data);
		} catch (Exception e) {
			handleException(e);
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		try {
			return super.resolveEntity(publicId, systemId);
		} catch (Exception e) {
			handleException(e);
			throw e;
		}
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		try {
			super.skippedEntity(name);
		} catch (Exception e) {
			handleException(e);
		}
	}


	@Override
	public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
		try {
			super.unparsedEntityDecl(name, publicId, systemId, notationName);
		} catch (Exception e) {
			handleException(e);
		}
	}

}
