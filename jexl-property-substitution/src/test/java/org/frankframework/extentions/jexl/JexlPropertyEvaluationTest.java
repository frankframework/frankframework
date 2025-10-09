package org.frankframework.extentions.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.frankframework.util.StringResolver;

class JexlPropertyEvaluationTest {

	@Test
	void testResolveBasic() {
		// Arrange
		JexlPropertyEvaluation jexlPropertyEvaluation = new JexlPropertyEvaluation();

		Map<String, Object> map = new HashMap<>();
		map.put("key", "value");

		// Act
		Optional<String> value = jexlPropertyEvaluation.resolve("key", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("value", value.get());
	}

	@Test
	void testResolveWithTransform() {
		// Arrange
		JexlPropertyEvaluation jexlPropertyEvaluation = new JexlPropertyEvaluation();

		Map<String, Object> map = new HashMap<>();
		map.put("key", "value");

		// Act
		Optional<String> value = jexlPropertyEvaluation.resolve("key.toUpperCase()", map, null, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("VALUE", value.get());
	}

	@Test
	void testResolveWithFunction() {
		// Arrange
		JexlPropertyEvaluation jexlPropertyEvaluation = new JexlPropertyEvaluation();

		Map<String, Object> map1 = new HashMap<>();
		map1.put("v1", 3);
		Map<String, Object> map2 = new HashMap<>();
		map2.put("v2", 1);

		// Act
		Optional<String> value = jexlPropertyEvaluation.resolve("Math.min(v1, v2)", map1, map2, null, StringResolver.DELIM_START, StringResolver.DELIM_STOP, false);

		// Assert
		assertTrue(value.isPresent());
		assertEquals("1", value.get());
	}
}
