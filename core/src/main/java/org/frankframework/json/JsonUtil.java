/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.parameters.ParameterList;
import org.frankframework.util.StreamUtil;

@Log4j2
public class JsonUtil {
	private JsonUtil() {
		// Private constructor to prevent instance creations
	}

	public static JsonMapper buildJsonMapper(IScopeProvider scopeProvider, String stylesheetName, DataSonnetOutputType outputType, boolean computeMimeType, ParameterList parameters) throws ConfigurationException {
		return new JsonMapper(getStyleSheet(scopeProvider, stylesheetName), outputType, computeMimeType, parameters.getParameterNames());
	}

	private static String getStyleSheet(IScopeProvider scopeProvider, String styleSheetName) throws ConfigurationException {
		Resource styleSheet = Resource.getResource(scopeProvider, styleSheetName);
		if (styleSheet == null) {
			throw new ConfigurationException("StyleSheet [" + styleSheetName + "] not found");
		}
		try (InputStream is = styleSheet.openStream()) {
			return StreamUtil.streamToString(is);
		} catch (IOException e) {
			throw new ConfigurationException("unable to open/read StyleSheet [" + styleSheetName + "]", e);
		}
	}

	public static String jsonPretty(String json) {
		StringWriter sw = new StringWriter();
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonStructure jobj = jr.read();

			Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);

			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
				jsonWriter.write(jobj);
			}
		}
		return sw.toString().trim();
	}
}
