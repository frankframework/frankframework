/*
   Copyright 2021, 2022 WeAreFrank!

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

import org.xml.sax.SAXException;

public class JsonObjectBuilder extends ObjectBuilder {

	private final JsonEventHandler handler;

	public JsonObjectBuilder(JsonEventHandler handler) throws SAXException {
		super();
		this.handler=handler;
		handler.startObject();
	}

	@Override
	public void close() throws SAXException {
		try {
			handler.endObject();
		} finally {
			super.close();
		}
	}


	@Override
	public INodeBuilder addField(String fieldName) throws SAXException {
		handler.startObjectEntry(fieldName);
		return new JsonNodeBuilder(handler);
	}

	@Override
	public ArrayBuilder addRepeatedField(String fieldName) throws SAXException {
		handler.startObjectEntry(fieldName);
		return new JsonArrayBuilder(handler);
	}

	@Override
	public void addNumber(String name, String value) throws SAXException {
		addField(name).setNumberValue(value);
	}

}
