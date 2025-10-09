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

		String input = "To the max: ${Math.max(v1, v2)}";

		// Act
		String result = StringResolver.substVars(input, vars);

		// Assert
		assertEquals("To the max: 20", result);
	}
}
