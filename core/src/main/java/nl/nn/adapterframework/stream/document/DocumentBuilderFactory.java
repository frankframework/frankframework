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
package nl.nn.adapterframework.stream.document;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.JsonEventHandler;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;

public class DocumentBuilderFactory {

	public static IDocumentBuilder startDocument(DocumentFormat format, String rootElement, MessageOutputStream outputStream) throws SAXException, StreamingException {
		switch (format) {
		case XML:
			return new XmlDocumentBuilder(rootElement, outputStream.asContentHandler());
		case JSON:
			return new JsonDocumentBuilder(outputStream.asJsonEventHandler());
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}
	
	public static IDocumentBuilder startDocument(DocumentFormat format, String rootElement) throws SAXException, StreamingException {
		switch (format) {
		case XML:
			return new XmlDocumentBuilder(rootElement);
		case JSON:
			return new JsonDocumentBuilder();
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}
	
	public static IDocumentBuilder startDocument(String rootElement, ContentHandler handler) throws SAXException {
		return new XmlDocumentBuilder(rootElement, handler);
	}
	
	public static IDocumentBuilder startDocument(JsonEventHandler handler) throws SAXException {
		return new JsonDocumentBuilder(handler);
	}
	
	public static ObjectBuilder startObjectDocument(DocumentFormat format, String rootElement, MessageOutputStream outputStream) throws SAXException, StreamingException {
		return startDocument(format, rootElement, outputStream).asObjectBuilder();
	}

	public static ObjectBuilder startObjectDocument(DocumentFormat format, String rootElement) throws SAXException, StreamingException {
		return startDocument(format, rootElement).asObjectBuilder();
	}

	public static ArrayBuilder startArrayDocument(DocumentFormat format, String rootElement, String elementName, MessageOutputStream outputStream) throws SAXException, StreamingException {
		return startDocument(format, rootElement, outputStream).asArrayBuilder(elementName);
	}

	public static ArrayBuilder startArrayDocument(DocumentFormat format, String rootElement, String elementName) throws SAXException, StreamingException {
		return startDocument(format, rootElement).asArrayBuilder(elementName);
	}

}
