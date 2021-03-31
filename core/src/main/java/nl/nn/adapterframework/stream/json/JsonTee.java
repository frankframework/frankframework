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
package nl.nn.adapterframework.stream.json;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.JsonEventHandler;

public class JsonTee implements JsonEventHandler {

	private JsonEventHandler first;
	private JsonEventHandler second;
	
	public JsonTee(JsonEventHandler first, JsonEventHandler second) {
		this.first=first;
		this.second=second;
	}

	@Override
	public void startDocument() throws SAXException {
		first.startDocument();
		second.startDocument();
	}

	@Override
	public void endDocument() throws SAXException {
		first.endDocument();
		second.endDocument();
	}

	@Override
	public void startObject() throws SAXException {
		first.startObject();
		second.startObject();
	}

	@Override
	public void startObjectEntry(String key) throws SAXException {
		first.startObjectEntry(key);
		second.startObjectEntry(key);
	}

	@Override
	public void endObject() throws SAXException {
		first.endObject();
		second.endObject();
	}

	@Override
	public void startArray() throws SAXException {
		first.startArray();
		second.startArray();
	}

	@Override
	public void endArray() throws SAXException {
		first.endArray();
		second.endArray();
	}

	@Override
	public void primitive(Object value) throws SAXException {
		first.primitive(value);
		second.primitive(value);
	}
	
}
