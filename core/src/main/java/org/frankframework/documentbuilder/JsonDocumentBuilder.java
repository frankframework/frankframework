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
package org.frankframework.documentbuilder;

import java.io.StringWriter;
import java.io.Writer;

import org.frankframework.documentbuilder.json.JsonWriter;
import org.xml.sax.SAXException;

public class JsonDocumentBuilder extends JsonNodeBuilder implements IDocumentBuilder {

	private JsonEventHandler handler;
	private Writer writer;

	public JsonDocumentBuilder() throws SAXException {
		this(new StringWriter());
	}

	public JsonDocumentBuilder(Writer writer) throws SAXException {
		this(new JsonWriter(writer));
		this.writer = writer;
	}

	public JsonDocumentBuilder(JsonEventHandler handler) throws SAXException {
		super(handler);
		this.handler=handler;
		handler.startDocument();
	}
	@Override
	public void close() throws SAXException {
		try {
			handler.endDocument();
		} finally {
			super.close();
		}
	}


	@Override
	public String toString() {
		return writer instanceof StringWriter ? writer.toString() : super.toString();
	}

	@Override
	public ObjectBuilder asObjectBuilder() throws SAXException {
		return ObjectBuilder.asObjectBuilder(this);
	}

	@Override
	public ArrayBuilder asArrayBuilder(String elementName) throws SAXException {
		return ArrayBuilder.asArrayBuilder(this, elementName);
	}

}
