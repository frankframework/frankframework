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

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.JsonEventHandler;

public class JsonNodeBuilder implements INodeBuilder {

	private JsonEventHandler handler;
	
	public JsonNodeBuilder(JsonEventHandler handler) {
		this.handler = handler;
	}

	@Override
	public void close() throws SAXException {
	}

	@Override
	public JsonArrayBuilder startArray(String elementName) throws SAXException {
		return new JsonArrayBuilder(handler);
	}

	@Override
	public JsonObjectBuilder startObject() throws SAXException {
		return new JsonObjectBuilder(handler);
	}

	@Override
	public void setValue(String value) throws SAXException {
		handler.primitive(value);
	}

	@Override
	public void setValue(long value) throws SAXException {
		handler.primitive(value);
	}

	@Override
	public void setValue(boolean value) throws SAXException {
		handler.primitive(value);
	}

}
