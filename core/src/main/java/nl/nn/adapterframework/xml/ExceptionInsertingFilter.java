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
package nl.nn.adapterframework.xml;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XMLFilter to reinsert exceptions into a XMLFilter chain.
 * Typical use case is in a XMLFilter chain with a streaming XSLT. In that case, the actual processing of the XSLT 
 * in the tail of the chain is performed internally in a different thread than the provisioning of events from the head of the chain.
 * Exceptions in the tail of the chain do not reach the head then. 
 * In such a situation the ExceptionInsertingFilter can be used in the head of the chain, to receive unhandled exceptions of the tail.
 *
 * @author Gerrit van Brakel
 * 
 * @see TransformerFilter
 */
public class ExceptionInsertingFilter extends FullXmlFilter {

	private SAXException insertedException=null;
	
	public ExceptionInsertingFilter(ContentHandler handler) {
		super(handler);
	}

	public void insertException(SAXException exception) {
		insertedException = exception;
	}


	private void checkInserted() throws SAXException {
		if (insertedException!=null) {
			SAXException exceptionToThrow = insertedException;
			insertedException = null;
			throw exceptionToThrow;
		}
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		checkInserted();
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		checkInserted();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement(uri, localName, qName, atts);
		checkInserted();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		checkInserted();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		checkInserted();
	}
	
	
	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		super.comment(ch, start, length);
		checkInserted();
	}

	@Override
	public void startCDATA() throws SAXException {
		super.startCDATA();
		checkInserted();
	}

	@Override
	public void endCDATA() throws SAXException {
		super.endCDATA();
		checkInserted();
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		super.startDTD(name, publicId, systemId);
		checkInserted();
	}

	@Override
	public void endDTD() throws SAXException {
		super.endDTD();
		checkInserted();
	}

	@Override
	public void startEntity(String name) throws SAXException {
		super.startEntity(name);
		checkInserted();
	}

	@Override
	public void endEntity(String name) throws SAXException {
		super.endEntity(name);
		checkInserted();
	}


	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		super.startPrefixMapping(prefix, uri);
		checkInserted();
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		super.endPrefixMapping(prefix);
		checkInserted();
	}


	@Override
	public void warning(SAXParseException e) throws SAXException {
		super.warning(e);
		checkInserted();
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		super.error(e);
		checkInserted();
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		super.fatalError(e);
		checkInserted();
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		super.ignorableWhitespace(ch, start, length);
		checkInserted();
	}

	@Override
	public void notationDecl(String name, String publicId, String systemId) throws SAXException {
		super.notationDecl(name, publicId, systemId);
		checkInserted();
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		super.processingInstruction(target, data);
		checkInserted();
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		InputSource result = super.resolveEntity(publicId, systemId);
		checkInserted();
		return result;
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		super.skippedEntity(name);
		checkInserted();
	}


	@Override
	public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
		super.unparsedEntityDecl(name, publicId, systemId, notationName);
		checkInserted();
	}

}
