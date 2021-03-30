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

public class JsonHandlerObjectBuilder extends ObjectBuilder {

	private JsonSaxHandler handler;
	private boolean asRoot;
	
	public JsonHandlerObjectBuilder(JsonSaxHandler handler) {
		this(handler, false);
	}
	public JsonHandlerObjectBuilder(JsonSaxHandler handler, boolean asRoot) {
		super();
		this.handler=handler;
		if (asRoot) {
			handler.startJSON();
		}
		handler.startObject();
	}

	@Override
	public void close() throws DocumentException {
		handler.endObject();
		super.close();
		if (asRoot) {
			handler.endJSON();
		}
	}


	@Override
	public NodeBuilder addField(String fieldName) throws DocumentException {
		handler.startObjectEntry(fieldName);
		return new JsonHandlerNodeBuilder(handler);
	}

	@Override
	public ArrayBuilder addRepeatedField(String fieldName) throws DocumentException {
		handler.startObjectEntry(fieldName);
		return new JsonHandlerArrayBuilder(handler);
	}

}
