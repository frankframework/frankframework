package org.frankframework.extentions.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.frankframework.util.StringResolver;

class EmbeddedScriptEvaluationTest {

	@Test
	void testResolveBasic() {
		// Arrange
		EmbeddedScriptEvaluation embeddedScriptEvaluation = new EmbeddedScriptEvaluation();

		Map<String, Object> map = new HashMap<>();
		map.put("key", "value");

		// Act
		Optional<String> value = embeddedScriptEvaluation.resolve("=key", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("value", value.get());
	}

	@Test
	void testResolveSimpleCalculation() {
		// Arrange
		EmbeddedScriptEvaluation embeddedScriptEvaluation = new EmbeddedScriptEvaluation();

		Map<String, Object> map = new HashMap<>();

		// Act
		Optional<String> value = embeddedScriptEvaluation.resolve("=4 * 5", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("20", value.get());
	}

	@Test
	void testResolveWithTransform() {
		// Arrange
		EmbeddedScriptEvaluation embeddedScriptEvaluation = new EmbeddedScriptEvaluation();

		Map<String, Object> map = new HashMap<>();
		map.put("key", "value");

		// Act
		Optional<String> value = embeddedScriptEvaluation.resolve("=key.toUpperCase()", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("VALUE", value.get());
	}

	@Test
	void testResolveWithFunction() {
		// Arrange
		EmbeddedScriptEvaluation embeddedScriptEvaluation = new EmbeddedScriptEvaluation();

		Map<String, Object> map1 = new HashMap<>();
		map1.put("v1", 3);
		Map<String, Object> map2 = new HashMap<>();
		map2.put("v2", 1);

		// Act
		Optional<String> value = embeddedScriptEvaluation.resolve("=Math.min(v1, v2)", map1, map2, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("1", value.get());
	}

	@Test
	void testIfCondition() {
		// Arrange
		EmbeddedScriptEvaluation embeddedScriptEvaluation = new EmbeddedScriptEvaluation();
		Map<String, Object> map = new HashMap<>();
		map.put("values.v1", "x");
		map.put("values.v2", "u");

		// Act
		Optional<String> result = embeddedScriptEvaluation.resolve("=let result = -1; if (values.v1.equals(\"y\")) { result = 1} else { result = 2} return result", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(result.isPresent());
		assertEquals("2", result.get());
	}
}
