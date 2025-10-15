package org.frankframework.extentions.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.util.PropertyLoader;
import org.frankframework.util.StringResolver;

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

	@Test
	public void testStringOperationOnMissingProperty() {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");

		// Act
		String value = props.getProperty("instance.name.lc");

		// Assert
		assertEquals("", value);
	}

	@ParameterizedTest
	@CsvSource({
			"true, , true", // Param hostName = null
			"true, '', true", // Param hostName = empty string
			"false, , false", // Param hostName = null
			"false, '', false", // Param hostName = empty string
			"true, example.com, false",
			"false, example.com, false"
	})
	void booleanLikeEvaluation(boolean isActive, String hostName, boolean shouldGiveWarning) {
		// Arrange
		PropertyLoader map = new PropertyLoader("test.properties");
		map.setProperty("mail.active", Boolean.toString(isActive));
		if (hostName != null) {
			map.setProperty("mail.host", hostName);
		}
		map.setProperty("mail.hostname", "${mail.host}"); // This indirection makes for a null-safe variable 'mail.hostname'

		// Act

		// In the boolean expression, none of the values can be null. (null is not automatically 'false').
		String giveWarning1 = StringResolver.substVars("${= mail.active && !\"${mail.host}\" }", map); // Put quotes and nested evaluation around the potential null-value
		String giveWarning2 = StringResolver.substVars("${= mail.active && !mail.hostname }", map); // Use indirection to solve the null-value-problem
		String giveWarning3 = StringResolver.substVars("${= mail.active && StringUtils.isBlank(mail.host) }", map); // The potential null-value is not a problem as argument to this method

		// Assert
		assertEquals(Boolean.toString(shouldGiveWarning), giveWarning1);
		assertEquals(Boolean.toString(shouldGiveWarning), giveWarning2);
		assertEquals(Boolean.toString(shouldGiveWarning), giveWarning3);
	}
}
