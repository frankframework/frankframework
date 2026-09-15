/*
   Copyright 2025-2026 WeAreFrank!

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.JSONParser;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

public class JsonUtil {
	// Since 2.6.0 json-smart accepts incomplete JSON by default, so we have to use MODE_PERMISSIVE to disable that.
	private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
			.jsonProvider(new JsonSmartJsonProvider(JSONParser.MODE_PERMISSIVE))
			.build();

	private JsonUtil() {
		// Private constructor to prevent instance creations
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
	public static @Nullable String evaluateJsonPathToSingleValue(@NonNull JsonPath jsonPath, Message message) throws JsonException {
		if (Message.isNull(message)) {
			return null;
		}
		// Try to match the jsonPath expression on the given json string
		try {
			// May return null!
			Object jsonPathResult = evaluateJsonPathWithBom(jsonPath, message);

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
	 * Empty is treated as no filter matches.
	 * If the result is not an array and not a scalar value, then return an empty string. (What would the result be?)
	 */
	private static @Nullable String getSingleValueJsonPathResult(@Nullable Object jsonPathResult) {
		if (jsonPathResult instanceof String string) {
			return string;
		}
		if (jsonPathResult instanceof Number number) { // JSONValue.toJSONString(obj)
			return number.toString();
		}
		if (jsonPathResult instanceof Boolean bool) {
			return bool.toString();
		}

		if (jsonPathResult instanceof JSONArray jsonArray) {
			if (jsonArray.isEmpty()) {
				// We found nothing, we got an empty array and there are no matches.
				return null;
			}
			return getSingleValueJsonPathResult(jsonArray.getFirst());
		}

		if (jsonPathResult == null) {
			return null;
		}

		// We found something, but it does not have a proper string representation!
		// Usable for the IF-pipe...
		// Do not return NULL because NULL indicates that nothing is found.
		return "";
	}

	public static @NonNull Message evaluateJsonPath(@NonNull JsonPath jsonPath, @Nullable Object input) throws JsonException {
		try {
			Message inputMessage = MessageUtils.convertToJsonMessage(input);
			Object result = evaluateJsonPathWithBom(jsonPath, inputMessage);
			return getJsonPathResult(result);
		} catch (PathNotFoundException e) {
			throw new JsonPathNotFoundException("Cannot find path in input", e);
		} catch (Exception e) {
			throw new JsonException("Failed to evaluate JSonPathExpression [" + jsonPath + "]", e);
		}
	}

	private static @Nullable Object evaluateJsonPathWithBom(@NonNull JsonPath jsonPath, @NonNull Message inputMessage) throws IOException {
		// Optimise for the String case
		if (inputMessage.isRequestOfType(String.class)) {
			return jsonPath.read(inputMessage.asString(), JSON_PATH_CONFIGURATION);
		}

		StreamAndCharset streamAndCharset = getInputStreamWithoutBom(inputMessage);
		return jsonPath.read(streamAndCharset.inputStream, streamAndCharset.charset, JSON_PATH_CONFIGURATION);

	}

	/**
	 * Get from the Message an InputStream where an optional BOM has already been skipped, because the JayWay
	 * library doesn't do that.
	 * This also returns the charset that was derived from the BOM if present, or otherwise the charset computed
	 * from the Message, or UTF-8 default if all else fails.
	 */
	private static @NonNull StreamAndCharset getInputStreamWithoutBom(@NonNull Message message) throws IOException {
		// The JayWay library does not handle a BOM at start of stream. Thus instead of directly passing
		// an InputStream from the Message, which may start with a BOM, we wrap that in a BOMInputStream and
		// check the BOM and BOM charset ourselves.
		// This way we can pass back to JayWay an InputStream where the BOM has already been read past.
		BOMInputStream bomDetectingInputStream = StreamUtil.getBomDetectingInputStream(message.asInputStream());
		String charset;
		if (bomDetectingInputStream.hasBOM()) {
			charset = bomDetectingInputStream.getBOM().getCharsetName();
		} else {
			Charset computedCharset = MessageUtils.computeDecodingCharset(message);
			charset = Objects.requireNonNullElse(computedCharset, StandardCharsets.UTF_8).name();
		}
		message.getContext().withCharset(charset);
		return new StreamAndCharset(bomDetectingInputStream, charset);
	}

	/**
	 * Official json-smart conversion. Tweaked a bit, when we know it's json, sets the correct mimetype.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static @NonNull Message getJsonPathResult(@Nullable Object obj) {
		if (obj instanceof Map jsonObject) {
			Message result = new Message(JSONObject.toJSONString(jsonObject, JSONStyle.LT_COMPRESS));
			result.getContext().withMimeType(MediaType.APPLICATION_JSON);
			return result;
		} else if (obj instanceof List jsonArray) {
			Message result = new Message(JSONArray.toJSONString(jsonArray, JSONStyle.LT_COMPRESS));
			result.getContext().withMimeType(MediaType.APPLICATION_JSON);
			return result;
		} else if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
			// Scalar value, not JSON!
			Message result =  Message.asMessage(getSingleValueJsonPathResult(obj));
			result.getContext().withMimeType(MediaType.TEXT_PLAIN).withCharset(StandardCharsets.UTF_8);
			return result;
		} else if (obj == null) {
			return Message.nullMessage();
		} else {
			throw new UnsupportedOperationException(obj.getClass().getName() + " can not be converted to JSON");
		}
	}

	public static @NonNull String jsonPretty(@NonNull String json) {
		StringWriter sw = new StringWriter();
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonStructure jobj = jr.read();

			Map<String, Object> properties = HashMap.newHashMap(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);

			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
				jsonWriter.write(jobj);
			}
		}
		return sw.toString().trim();
	}

	private record StreamAndCharset(@NonNull InputStream inputStream, @NonNull String charset) {}
}
