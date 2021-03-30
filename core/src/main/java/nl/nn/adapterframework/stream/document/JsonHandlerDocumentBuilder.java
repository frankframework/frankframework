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

import java.io.Writer;

import org.jsfr.json.JsonSaxHandler;

import nl.nn.adapterframework.stream.json.JsonWriter;

public class JsonHandlerDocumentBuilder extends JsonHandlerNodeBuilder {

	private JsonSaxHandler handler;

	public JsonHandlerDocumentBuilder(Writer writer) {
		this(new JsonWriter(writer));
	}
	
	public JsonHandlerDocumentBuilder() {
		this(new JsonWriter());
	}

	public JsonHandlerDocumentBuilder(JsonSaxHandler handler) {
		super(handler);
		this.handler=handler;
		handler.startJSON();
	}
	@Override
	public void close() throws DocumentException {
		handler.endJSON();
	}

}
