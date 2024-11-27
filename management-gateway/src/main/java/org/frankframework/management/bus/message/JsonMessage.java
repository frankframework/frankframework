/*
   Copyright 2023-2024 WeAreFrank!

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
package org.frankframework.management.bus.message;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.springframework.http.MediaType;

import org.frankframework.util.JacksonUtils;

public class JsonMessage extends StringMessage {
	private static final JsonWriterFactory WRITER_FACTORY;

	static {
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, false);
		WRITER_FACTORY = Json.createWriterFactory(config);
	}

	public JsonMessage(Object payload) {
		super(JacksonUtils.convertToJson(payload), MediaType.APPLICATION_JSON);
	}

	public JsonMessage(JsonStructure payload) {
		super(jsonStructureToString(payload), MediaType.APPLICATION_JSON);
	}

	private static String jsonStructureToString(JsonStructure payload) {
		Writer writer = new StringWriter();

		try (JsonWriter jsonWriter = WRITER_FACTORY.createWriter(writer)) {
			jsonWriter.write(payload);
		}

		return writer.toString();
	}
}
