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

import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;

public class JsonStructureArrayDocumentBuilder extends ArrayBuilder {
	
	private Consumer<JsonValue> register;
	private JsonArrayBuilder arrayBuilder;

	public JsonStructureArrayDocumentBuilder(Consumer<JsonValue> register) {
		this.register = register;
		this.arrayBuilder = Json.createArrayBuilder();
	}
	
	@Override
	public void close() throws DocumentException {
		register.accept(arrayBuilder.build());
	}

	@Override
	public NodeBuilder addElement() throws DocumentException {
		return new JsonStructureNodeBuilder((value) -> arrayBuilder.add(value));
	}

}
