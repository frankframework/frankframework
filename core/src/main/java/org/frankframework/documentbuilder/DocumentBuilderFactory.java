/*
   Copyright 2021, 2024 WeAreFrank!

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
package org.frankframework.documentbuilder;

import java.io.Writer;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.stream.MessageBuilder;

public class DocumentBuilderFactory {

	public static IDocumentBuilder startDocument(DocumentFormat format, String rootElement, MessageBuilder messageBuilder, boolean prettyPrint) throws SAXException {
		switch (format) {
		case XML:
			return new XmlDocumentBuilder(rootElement, messageBuilder.asXmlWriter(), prettyPrint);
		case JSON:
			return new JsonDocumentBuilder(messageBuilder.asJsonWriter());
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}

	public static IDocumentBuilder startDocument(DocumentFormat format, String rootElement, Writer writer) throws SAXException {
		switch (format) {
		case XML:
			return new XmlDocumentBuilder(rootElement, writer);
		case JSON:
			return new JsonDocumentBuilder(writer);
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}

	public static IDocumentBuilder startDocument(String rootElement, ContentHandler handler, boolean prettyPrint) throws SAXException {
		return new XmlDocumentBuilder(rootElement, handler, prettyPrint);
	}

	public static IDocumentBuilder startDocument(JsonEventHandler handler) throws SAXException {
		return new JsonDocumentBuilder(handler);
	}

	public static ObjectBuilder startObjectDocument(DocumentFormat format, String rootElement, MessageBuilder outputStream, boolean prettyPrint) throws SAXException {
		return startDocument(format, rootElement, outputStream, prettyPrint).asObjectBuilder();
	}

	public static ArrayBuilder startArrayDocument(DocumentFormat format, String rootElement, String elementName, MessageBuilder outputStream, boolean prettyPrint) throws SAXException {
		return startDocument(format, rootElement, outputStream, prettyPrint).asArrayBuilder(elementName);
	}

}
