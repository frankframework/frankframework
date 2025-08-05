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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;

import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

@Log4j2
public class JsonUtil {
	// Since 2.6.0 json-smart accepts incomplete JSON by default, so we have to use MODE_PERMISSIVE to disable that.
	private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
			.jsonProvider(new JsonSmartJsonProvider(JSONParser.MODE_PERMISSIVE))
			.build();

	private JsonUtil() {
		// Private constructor to prevent instance creations
	}

	public static JsonMapper buildJsonMapper(IScopeProvider scopeProvider, String stylesheetName, DataSonnetOutputType outputType, boolean computeMimeType, ParameterList parameters) throws ConfigurationException {
		try {
			return new JsonMapper(getStyleSheet(scopeProvider, stylesheetName), outputType, computeMimeType, parameters.getParameterNames());
		} catch (RuntimeException e) {
			throw new ConfigurationException("Cannot configure DataSonnet Mapper", e);
		}
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

	/**
	 * Compile a {@link JsonPath} from the {@code jsonPathExpression} parameter, if it's not {@code null}. If {@code jsonPathExpression} is {@code null},
	 * return {@code null}.
	 *
	 * @param jsonPathExpression JSON Path Expression to compile. May be {@code null}.
	 * @return {@link JsonPath} compiled from the {@code jsonPathExpression}, or {@code null} if {@code jsonPathExpression} was {@code null}.
	 * @throws ConfigurationException If there was any exception in trying to compile the {@code jsonPathExpression}, throw a {@link ConfigurationException}.
	 */
	public static @Nullable JsonPath compileJsonPath(@Nullable String jsonPathExpression) throws ConfigurationException {
		if (StringUtils.isEmpty(jsonPathExpression)) {
			return null;
		}
		try {
			return JsonPath.compile(jsonPathExpression);
		} catch (Exception e) {
			throw new ConfigurationException("Invalid JSON path expression: [" + jsonPathExpression + "]", e);
		}
	}

	/**
	 * Evaluate given {@link JsonPath} against the input {@link Message} and return a single scalar value,
	 * represented as {@link String}.
	 * <br/>
	 * <ul>
	 *     <li>If the expression evaluation results in a JsonArray, the first value of the array is returned.</li>
	 *     <li>If the expression evaluation results in a JsonObject, an empty string is returned to indicate a non-scalar value was found.</li>
	 *     <li>If the expression evaluation does not result in any value found, then {@code null} is returned to indicate
	 *         that there was no result.</li>
	 * </ul>
	 *
	 * @param jsonPath {@link JsonPath} expression to evaluate
	 * @param message {@link Message} from which to extract data
	 * @return Single scalar value from the message, an empty string if the value could not be reduced to a scalar, or {@code null} if there was no result.
	 * @throws JsonException If an exception occurred evaluating the expression a {@link JsonException} is thrown.
	 */
	public static @Nullable String evaluateJsonPathToSingleValue(@Nonnull JsonPath jsonPath, Message message) throws JsonException {
		if (Message.isNull(message)) {
			return null;
		}
		// Try to match the jsonPath expression on the given json string
		try {
			Object jsonPathResult = jsonPath.read(message.asInputStream(), JSON_PATH_CONFIGURATION);

			// if we get to this point, we have a match (and no PathNotFoundException)

			return getSingleValueJsonPathResult(jsonPathResult);
		} catch (PathNotFoundException e) {
			// No results found for path, return NULL to indicate nothing was found
			return null;
		} catch (IOException ioe) {
			throw new JsonException("error reading message", ioe);
		} catch (Exception e) {
			throw new JsonException("error evaluating expression", e);
		}
	}

	/**
	 * When using expressions, jsonPath returns a JsonArray, even if there is only one match. Make sure to get a String from it.
	 * If the result is not an array and not a scalar value, then return an empty string. Do not return NULL
	 * when a result was found.
	 */
	private static @Nullable String getSingleValueJsonPathResult(@Nonnull Object jsonPathResult) {
		if (jsonPathResult instanceof String string) {
			return string;
		}
		if (jsonPathResult instanceof Number number) {
			return number.toString();
		}
		if (jsonPathResult instanceof Boolean bool) {
			return bool.toString();
		}

		if (jsonPathResult instanceof JSONArray jsonArray) {
			if (jsonArray.isEmpty()) {
				return "";
			}
			return getSingleValueJsonPathResult(jsonArray.get(0));
		}

		// We found something, but it does not have a proper string representation
		// usable for the IF-pipe.
		// Do not return NULL because NULL indicates that nothing is found.
		return "";
	}

	public static @Nonnull String evaluateJsonPath(@Nonnull JsonPath jsonPath, @Nonnull Object input) throws JsonException {
		try {
			Message inputMessage = MessageUtils.convertToJsonMessage(input);
			Object result = jsonPath.read(inputMessage.asInputStream());
			return getJsonPathResult(result);
		} catch (Exception e) {
			throw new JsonException("Cannot evaluate JSonPathExpression on parameter value", e);
		}
	}

	private static @Nonnull String getJsonPathResult(@Nonnull Object result) {
		if (result instanceof HashMap<?,?> map) {
			@SuppressWarnings("unchecked")
			JSONObject jsonObject = new JSONObject((Map<String, ?>) map);
			return jsonObject.toString();
		}
		return result.toString();
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
