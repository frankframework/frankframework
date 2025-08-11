package org.frankframework.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

/**
 * Utility class for matching JSON strings in tests.
 * Provides methods to assert equality of JSON strings and to pretty-print JSON.
 * <br/>
 * Pleased note that this class is an distilled version of the original MatchUtils class, because we can't use the original class here
 * without introducing a dependency on the core module.
 *
 * @see org.frankframework.testutil.MatchUtils
 * @see org.frankframework.json.JsonUtil#jsonPretty(String)
 */
public class MatchUtils {
	public static void assertJsonEquals(String jsonExp, String jsonAct) {
		assertJsonEquals(null, jsonExp, jsonAct);
	}

	public static void assertJsonEquals(String description, String jsonExp, String jsonAct) {
		assertEquals(jsonPretty(jsonExp), jsonPretty(jsonAct), description);
	}

	public static String jsonPretty(String json) {
		StringWriter stringWriter = new StringWriter();
		try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
			JsonStructure jsonStructure = jsonReader.read();

			Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);

			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
				jsonWriter.write(jsonStructure);
			}
		}
		return stringWriter.toString().trim();
	}
}
