package org.frankframework.extentions.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.frankframework.util.StringResolver;

public class JexlEvaluationInStringVarSubstTest {

	@Test
	void testResolveWithStringVarSubst() {
		// Arrange
		Map<String, Object> vars = new HashMap<>();
		vars.put("v1", 15);
		vars.put("v2", 20);

		String input = "To the max: ${=Math.max(v1, v2)}";

		// Act
		String result = StringResolver.substVars(input, vars);

		// Assert
		assertEquals("To the max: 20", result);
	}

	@Test
	void testResolveWithNestedEvaluations() {
		// Arrange
		Map<String, Object> vars = new HashMap<>();
		vars.put("values.v1", "5");
		vars.put("values.v2", "20");

		String input = "${=${values.v1} + ${values.v2}}";

		// Act
		String result = StringResolver.substVars(input, vars);

		// Assert
		assertEquals("25", result);
	}

	@Test
	void testResolveWithDottedVariableNames() {
		// Arrange
		Map<String, Object> vars = new HashMap<>();
		vars.put("values.v1", 5);
		vars.put("values.v2", 20);

		String input = "${=values.v1 + values.v2}";

		// Act
		String result = StringResolver.substVars(input, vars);

		// Assert
		assertEquals("25", result);
	}
}
