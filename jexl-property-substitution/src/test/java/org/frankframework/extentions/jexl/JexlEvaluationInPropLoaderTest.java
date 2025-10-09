package org.frankframework.extentions.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.util.PropertyLoader;

public class JexlEvaluationInPropLoaderTest {

	@AfterEach
	public void afterEach() {
		System.clearProperty("test.v3");
	}

	@Test
	public void testJexlInProperties1() {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");

		// Act
		String value = props.getProperty("test.vmin");

		// Assert
		assertEquals("2", value);
	}

	@ParameterizedTest
	@ValueSource(strings = {"test.expr1", "test.expr2"})
	public void testJexlInProperties2(String propertyName) {
		// Arrange
		System.setProperty("test.v3", "5");
		PropertyLoader props = new PropertyLoader("test.properties");

		// Act
		String value = props.getProperty(propertyName);

		// Assert
		assertEquals("10", value);
	}
}
