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

import org.jsfr.json.JsonSaxHandler;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;

public class DocumentBuilderFactory {

	public static NodeBuilder startDocument(DocumentFormat format, String rootElement, MessageOutputStream outputStream) throws DocumentException, StreamingException {
		switch (format) {
		case XML:
			return new XmlNodeBuilder(rootElement, outputStream.asContentHandler());
		case JSON:
			return new JsonHandlerDocumentBuilder(outputStream.asJsonSaxHandler());
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}
	
	public static NodeBuilder startDocument(String rootElement, ContentHandler handler) throws DocumentException {
		return new XmlNodeBuilder(rootElement, handler);
	}
	
	public static NodeBuilder startDocument(JsonSaxHandler handler) {
		return new JsonHandlerDocumentBuilder(handler);
	}
	
	public static ObjectBuilder startObjectDocument(DocumentFormat format, String rootElement, MessageOutputStream outputStream) throws DocumentException, StreamingException {
		switch (format) {
		case XML:
			return new XmlObjectBuilder(rootElement, outputStream.asContentHandler());
		case JSON:
			return new JsonHandlerObjectBuilder(outputStream.asJsonSaxHandler(), true);
		default:
			throw new IllegalArgumentException("Unknown document format ["+format+"]");
		}
	}

}
