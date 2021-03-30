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
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class JsonStructureObjectDocumentBuilder extends ObjectBuilder {

	private Consumer<JsonValue> register;
	private JsonObjectBuilder objectBuilder;

	public JsonStructureObjectDocumentBuilder(Consumer<JsonValue> register) {
		this.register = register;
		this.objectBuilder = Json.createObjectBuilder();
	}
	
	@Override
	public void close() throws DocumentException {
		register.accept(objectBuilder.build());
		super.close();
	}

	@Override
	public NodeBuilder addField(String fieldName) throws DocumentException {
		return new JsonStructureNodeBuilder((value) -> objectBuilder.add(fieldName, value));
	}

	@Override
	public ArrayBuilder addRepeatedField(String fieldName) throws DocumentException {
		return new JsonStructureArrayDocumentBuilder((value) -> objectBuilder.add(fieldName, value));
	}

}
