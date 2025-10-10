package org.frankframework.extentions.script;

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
		System.clearProperty("str.switch");
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

	@ParameterizedTest
	@ValueSource(strings = {"str.text1", "str.text2", "str.text3"})
	public void testJexlStringTest(String propertyName) {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");

		// Act
		String value = props.getProperty(propertyName);

		// Assert
		assertEquals("We Are Frank", value);
	}

	@Test
	public void testJexlStringTest2() {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");
		System.setProperty("str.switch", "5");
		// Act
		String value = props.getProperty("str.text2");

		// Assert
		assertEquals("We Are IBIS", value);
	}

	@Test
	public void testJexlStringTest3() {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");
		System.setProperty("str.switch", "5");
		// Act
		String value = props.getProperty("str.transform1");

		// Assert
		assertEquals("we, are, iBIS", value);
	}
}
