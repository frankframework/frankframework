package org.frankframework.console.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class RequestUtilsTest {

	@Test
	public void testConversions() throws Exception {
		InputStream string = new ByteArrayInputStream("string".getBytes());
		InputStream number = new ByteArrayInputStream("50".getBytes());
		InputStream boolTrue = new ByteArrayInputStream("true".getBytes());
		InputStream boolFalse = new ByteArrayInputStream("false".getBytes());
		InputStream boolNull = new ByteArrayInputStream("".getBytes());

		assertEquals("string", RequestUtils.convert(String.class, string));
		assertTrue(RequestUtils.convert(boolean.class, boolTrue));
		assertFalse(RequestUtils.convert(Boolean.class, boolFalse));
		assertFalse(RequestUtils.convert(boolean.class, boolNull));
		assertEquals(50, RequestUtils.convert(Integer.class, number).intValue());
		assertEquals(string, RequestUtils.convert(InputStream.class, string));
	}

	@Test
	public void testGetValue() {
		// Arrange
		Map<String, Object> input = new HashMap<>();
		input.put("string", "value");
		input.put("integer", "123");
		input.put("boolean", "true");
		input.put("empty", "");
		input.put("null", null);

		// Act + Assert
		assertEquals("value", RequestUtils.getValue(input, "string"));
		assertEquals(123, RequestUtils.getIntegerValue(input, "integer"));
		assertTrue(RequestUtils.getBooleanValue(input, "boolean"));
		assertFalse(RequestUtils.getBooleanValue(input, "empty"));
		assertNull(RequestUtils.getIntegerValue(input, "non-existing-integer"));
		assertNull(RequestUtils.getBooleanValue(input, "non-existing-boolean"));
		assertEquals("", RequestUtils.getValue(input, "empty"));
		assertNull(RequestUtils.getValue(input, "null"));
	}
}
