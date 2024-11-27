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
package org.frankframework.xml;

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
		if(insertedException != null) {
			log.error("exception ({}) [{}] overwrites existing exception", exception.getClass().getCanonicalName(), exception.getMessage(), insertedException);
		}

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
		try {
			super.startDocument();
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			super.endDocument();
		} finally {
			checkInserted();
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		try {
			super.startElement(uri, localName, qName, atts);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			super.characters(ch, start, length);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			super.endElement(uri, localName, qName);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		try {
			super.comment(ch, start, length);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		try {
			super.startCDATA();
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		try {
			super.endCDATA();
		} finally {
			checkInserted();
		}
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		try {
			super.startDTD(name, publicId, systemId);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endDTD() throws SAXException {
		try {
			super.endDTD();
		} finally {
			checkInserted();
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		try {
			super.startEntity(name);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endEntity(String name) throws SAXException {
		try {
			super.endEntity(name);
		} finally {
			checkInserted();
		}
	}


	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		try {
			super.startPrefixMapping(prefix, uri);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		try {
			super.endPrefixMapping(prefix);
		} finally {
			checkInserted();
		}
	}


	@Override
	public void warning(SAXParseException e) throws SAXException {
		try {
			super.warning(e);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		try {
			super.error(e);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		try {
			super.fatalError(e);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		try {
			super.ignorableWhitespace(ch, start, length);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void notationDecl(String name, String publicId, String systemId) throws SAXException {
		try {
			super.notationDecl(name, publicId, systemId);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		try {
			super.processingInstruction(target, data);
		} finally {
			checkInserted();
		}
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		try {
			return super.resolveEntity(publicId, systemId);
		} finally {
			checkInserted();
		}
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		try {
			super.skippedEntity(name);
		} finally {
			checkInserted();
		}
	}


	@Override
	public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
		try {
			super.unparsedEntityDecl(name, publicId, systemId, notationName);
		} finally {
			checkInserted();
		}
	}

}
