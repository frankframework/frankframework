package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


public class StringResolverTest {

	Properties properties;

	@BeforeEach
	public void setUp() throws Exception {
		properties = new Properties();
		URL propertiesURL = getClass().getResource("/StringResolver.properties");
		assertNotNull(propertiesURL, "properties file [StringResolver.properties] not found!");

		InputStream propsStream = propertiesURL.openStream();
		properties.load(propsStream);
		assertTrue(!properties.isEmpty(), "did not find any properties!");

		System.setProperty("authAliases.expansion.allowed", "${allowedAliases}");
	}

	@Test
	public void resolveFromEnvironment() {
		// Arrange
		String envVarName = System.getenv().containsKey("Path") ? "Path" : "PATH";
		String substString = "${" + envVarName + "}";

		// Act
		String result = StringResolver.substVars(substString, null);

		// Assert
		assertNotNull(result);
		assertFalse(StringUtils.isBlank(result));
		assertTrue(result.contains(File.pathSeparator));

		// Act
		result = StringResolver.substVars(substString, null, true);

		// Assert
		assertTrue(result.startsWith("${" + envVarName + ":-"));
		assertTrue(result.contains(File.pathSeparator));
	}

	@Test
	void resolveFromSystemProperties() {
		// Arrange
		properties.put("user.home", "wrong answer");

		// Act
		String result = StringResolver.substVars("${user.home}", properties);

		// Assert
		assertEquals(System.getProperty("user.home"), result);
	}

	@Test
	void resolveWithTwoDefaultValues() {
		// Act
		String result = StringResolver.substVars("${testMultipleDefaults}", properties);

		// Assert
		assertEquals("/dev/null | /dev/null", result);
	}

	@Test
	public void resolveFromEnvironmentBeforeProps() {
		// Arrange
		String envVarName = System.getenv().containsKey("Path") ? "Path" : "PATH";
		String substString = "${" + envVarName + "}";
		properties.put(envVarName, "not the right answer");

		// Act
		String result = StringResolver.substVars(substString, properties);

		// Assert
		assertNotNull(result);
		assertFalse(StringUtils.isBlank(result));
		assertTrue(result.contains(File.pathSeparator));
		assertFalse(result.contains("not the right answer"));

		// Act
		result = StringResolver.substVars(substString, properties, true);

		// Assert
		assertNotNull(result);
		assertTrue(result.startsWith("${" + envVarName + ":-"));
		assertTrue(result.contains(File.pathSeparator));
		assertFalse(result.contains("not the right answer"));
	}

	@Test
	public void resolveSimpleWithProperties() {
		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", properties);

		// Assert
		assertEquals("blalblalab value1", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", properties, true);

		// Assert
		assertEquals("blalblalab ${key1:-value1}", result);
	}

	@Test
	public void resolveSimpleWithPropertiesDefault() {
		// This test verifies that a property is retrieved from a Properties that is supplied
		// as default to the Properties parameter given to the StringResolver.

		// Arrange
		Properties props = new Properties(properties);


		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", props);

		// Assert
		assertEquals("blalblalab value1", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", properties, true);

		// Assert
		assertEquals("blalblalab ${key1:-value1}", result);
	}

	@Test
	public void resolveSimpleFromProps2() {
		// Arrange
		Map<Object, Object> emptyMap = Collections.emptyMap();

		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", emptyMap, properties);

		// Assert
		assertEquals("blalblalab value1", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", emptyMap, properties, true);

		// Assert
		assertEquals("blalblalab ${key1:-value1}", result);
	}

	@Test
	void resolveSimpleWithMap() {
		// Arrange
		Map<String, Object> propsMap = Collections.singletonMap("key1", "map-value");

		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", propsMap, properties);

		// Assert
		assertEquals("blalblalab map-value", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", propsMap, properties, true);

		// Assert
		assertEquals("blalblalab ${key1:-map-value}", result);
	}

	@Test
	void resolveSimpleWithMapInProps2() {
		// Arrange
		Map<String, Object> propsMap = Collections.singletonMap("map-key", "map-value");

		// Act
		String result = StringResolver.substVars("blalblalab ${map-key}", properties, propsMap);

		// Assert
		assertEquals("blalblalab map-value", result);

		// Act
		result = StringResolver.substVars("blalblalab ${map-key}", properties, propsMap, true);

		// Assert
		assertEquals("blalblalab ${map-key:-map-value}", result);
	}

	@Test
	public void resolveNonExistent() {
		String result = StringResolver.substVars("blalblalab ${key7}", properties);
		assertEquals("blalblalab ", result);

		result = StringResolver.substVars("blalblalab ${key7}", properties, true);
		assertEquals("blalblalab ${key7:-}", result);
	}

	@Test
	public void resolveNonExistentWithDefault() {
		String result = StringResolver.substVars("blalblalab ${dir}", properties);
		assertEquals("blalblalab d:/temporary/dir", result);

		result = StringResolver.substVars("blalblalab ${dir}", properties, true);
		assertEquals("blalblalab ${dir:-${path:-d:/temporary}/dir}", result);
	}

	@Test
	public void resolveNonExistentWithProps2() {
		String result = StringResolver.substVars("blalblalab ${key7}", null, properties);
		assertEquals("blalblalab ", result);

		result = StringResolver.substVars("blalblalab ${key7}", null, properties, true);
		assertEquals("blalblalab ${key7:-}", result);
	}

	@Test
	public void resolveNonExistentWithDefaultWithProps2() {
		String result = StringResolver.substVars("blalblalab ${dir}", null, properties);
		assertEquals("blalblalab d:/temporary/dir", result);

		result = StringResolver.substVars("blalblalab ${dir}", null, properties, true);
		assertEquals("blalblalab ${dir:-${path:-d:/temporary}/dir}", result);
	}

	@Test
	public void resolveWithDefault() {
		String result = StringResolver.substVars("blalblalab ${dir2}", properties);
		assertEquals("blalblalab c:/temp/dir", result);

		result = StringResolver.substVars("blalblalab ${dir2}", properties, true);
		assertEquals("blalblalab ${dir2:-${log.dir:-c:/temp}/dir}", result);
	}

	@Test
	public void resolveWithDefaultFromProps2() {
		// Arrange
		Map<Object, Object> emptyMap = Collections.emptyMap();

		// Act
		String result = StringResolver.substVars("blalblalab ${dir2}", emptyMap, properties);

		// Assert
		assertEquals("blalblalab c:/temp/dir", result);

		// Act
		result = StringResolver.substVars("blalblalab ${dir2}", emptyMap, properties, true);

		// Assert
		assertEquals("blalblalab ${dir2:-${log.dir:-c:/temp}/dir}", result);
	}

	@Test
	public void resolveRecursively() {
		String result = StringResolver.substVars("blalblalab ${key4}", properties);
		assertEquals("blalblalab value1.value2.value1", result);

		result = StringResolver.substVars("blalblalab ${key4}", properties, true);
		assertEquals("blalblalab ${key4:-${key1:-value1}.${key3:-value2.${key1:-value1}}}", result);
	}

	@Test
	public void resolveRecursivelyAvoidStackOverflow() {
		String result = StringResolver.substVars("blalblalab ${key5}", properties);
		assertEquals("blalblalab ${key5}", result);

		result = StringResolver.substVars("blalblalab ${key5}", properties, true);
		assertEquals("blalblalab ${key5:-${key5}}", result);
	}

	@Test
	public void resolveComplexRecursively() {
		String result = StringResolver.substVars("blalblalab ${key1_${key2}}", properties);
		assertEquals("blalblalab value101", result);

		result = StringResolver.substVars("blalblalab ${key1_${key2}}", properties, true);
		assertEquals("blalblalab ${key1_${key2:-${key1:-value1}}:-value101}", result);

	}

	@Test
	public void resolveMFHNested() {
		String result = StringResolver.substVars("${mfh}", properties, false);
		assertEquals("c:/temp/testdata/mfh", result);

		result = StringResolver.substVars("${mfh}", properties, true);
		assertEquals("${mfh:-${testdata:-${log.dir:-c:/temp}/testdata}/mfh}", result);

	}

	@Test
	public void resolveComplexProperty() {
		String result = StringResolver.substVars("${testMultiResolve}", properties);
		assertEquals("one,two,three_value1value1,my_value2.value1,StageSpecifics_value1.value2.value1", result);

		result = StringResolver.substVars("${testMultiResolve}", properties, true);
		assertEquals("${testMultiResolve:-one,two,three_${key1:-value1}${key2:-${key1:-value1}},my_${key3:-value2.${key1:-value1}},StageSpecifics_${key4:-${key1:-value1}.${key3:-value2.${key1:-value1}}}}", result);
	}

	@Test
	public void resolveCyclicProperty() {
		String result = StringResolver.substVars("${cyclic}", properties);
		assertEquals("prefix ${cyclic} suffix", result);

		result = StringResolver.substVars("${cyclic}", properties, true);
		assertEquals("${cyclic:-prefix ${cyclic} suffix}", result);
	}

	@Test
	public void resolveWithCustomDelimiters() {
		// Act
		String result = StringResolver.substVars("blalblalab [key1]", properties, null, null, "[", "]");

		// Assert
		assertEquals("blalblalab value1", result);

		// Act
		result = StringResolver.substVars("blalblalab [key1]", properties, null, null, "[", "]", true);

		// Assert
		assertEquals("blalblalab [key1:-value1]", result);
	}

	@Test
	public void resolveWithIncorrectExpression() {
		// Act
		assertThrows(IllegalArgumentException.class, () -> StringResolver.substVars("blablabla ${key1", properties));
	}

	@Test
	public void resolveWithIncorrectCustomDelimiters() {
		// Act
		assertThrows(IllegalArgumentException.class, () -> StringResolver.substVars("blalblalab |key1|", properties, null, null, "|", "|"));
	}


	@Test
	public void resolveSimpleHideProperty() {
		// Arrange
		Set<String> propsToHide = Collections.singleton("key1");

		// Act
		String result = StringResolver.substVars("blalblalab ${key1}", properties, null, propsToHide);

		// Assert
		assertEquals("blalblalab ******", result);

		// Act
		result = StringResolver.substVars("blalblalab ${key1}", properties, null, propsToHide, true);

		// Assert
		assertEquals("blalblalab ${key1:-******}", result);
	}

	@Test
	public void resolveViaAdditionalResolver() {
		// Act
		String result = StringResolver.substVars("lalala ${reversi} blah", properties);

		// Assert
		assertEquals("lalala isrever blah", result);
	}

	@Test
	public void testAdditionalResolversMatchBeforeProperties() {
		// Arrange
		Properties props2 = new Properties();
		props2.setProperty("reversi", "not reversed");

		// Act
		String result = StringResolver.substVars("lalala ${reversi} blah", properties, props2);

		// Assert
		assertEquals("lalala isrever blah", result);
	}

	@ParameterizedTest
	@CsvSource({
			"'blah${key}blah', true",
			"'blah}booh${blah', false",
			"'blah${blah', false",
			"'blah$}blah', false"
	}
	)
	void testNeedsResolution(String input, boolean expected) {
		// Act / Assert
		assertEquals(expected, StringResolver.needsResolution(input));
	}
}
