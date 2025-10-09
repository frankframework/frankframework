package org.frankframework.extentions.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.util.PropertyLoader;

public class JexlEvaluationInPropLoaderTest {

	@Test
	public void testJexlInProperties() {
		// Arrange
		PropertyLoader props = new PropertyLoader("test.properties");

		// Act
		String value = props.getProperty("test.vmin");

		// Assert
		assertEquals("1", value);
	}
}
