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

import org.jsfr.json.JsonSaxHandler;
import org.jsfr.json.PrimitiveHolder;

public class JsonTee implements JsonSaxHandler {

	private JsonSaxHandler first;
	private JsonSaxHandler second;
	
	public JsonTee(JsonSaxHandler first, JsonSaxHandler second) {
		this.first=first;
		this.second=second;
	}

	@Override
	public boolean startJSON() {
		boolean result = first.startJSON();
		second.startJSON();
		return result;
	}

	@Override
	public boolean endJSON() {
		boolean result = first.endJSON();
		second.endJSON();
		return result;
	}

	@Override
	public boolean startObject() {
		boolean result = first.startObject();
		second.startObject();
		return result;
	}

	@Override
	public boolean startObjectEntry(String key) {
		boolean result = first.startObjectEntry(key);
		second.startObjectEntry(key);
		return result;
	}

	@Override
	public boolean endObject() {
		boolean result = first.endObject();
		second.endObject();
		return result;
	}

	@Override
	public boolean startArray() {
		boolean result = first.startArray();
		second.startArray();
		return result;
	}

	@Override
	public boolean endArray() {
		boolean result = first.endArray();
		second.endArray();
		return result;
	}

	@Override
	public boolean primitive(PrimitiveHolder primitiveHolder) {
		boolean result = first.primitive(primitiveHolder);
		second.primitive(primitiveHolder);
		return result;
	}
	
}
