package org.frankframework.util;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PropertyLoaderTest {

	private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private final String propertyFile = "StringResolver.properties";
	private PropertyLoader constants;

	@BeforeEach
	public void setUp() {
		constants = new PropertyLoader(classLoader, propertyFile);
	}

	@Test
	public void getProperty() {
		assertEquals("value1", constants.getProperty("key1"));
	}

	@Test
	public void resolveProperty() {
		assertEquals("value1", constants.get("key2"));
		assertEquals("value1", constants.getProperty("key2"));
	}

	@Test
	public void testMultiCycles() {
		assertEquals("value1.value2.value1", constants.get("key4"));
		assertEquals("value1.value2.value1", constants.getProperty("key4"));
	}

	@Test
	public void getBoolean() {
		assertTrue(constants.getBoolean("boolean", false));
		assertFalse(constants.getBoolean("non.existing.boolean", false));
	}

	@Test
	public void getInt() {
		assertEquals(1, constants.getInt("int", 0));
		assertEquals(0, constants.getInt("non.existing.int", 0));
	}

	@Test
	public void getLong() {
		assertEquals(2L, constants.getLong("long", 0L));
		assertEquals(0L, constants.getLong("non.existing.long", 0L));
	}

	@Test
	public void getDouble() {
		assertEquals(3.0, constants.getDouble("double", 0.0));
		assertEquals(0.0, constants.getDouble("non.existing.double", 0.0));
	}

	@Test
	public void getListProperty() {
		List<String> list = constants.getListProperty("testMultiResolve");
		assertEquals(5, list.size());
		assertTrue(list.contains("one"));
		assertTrue(list.contains("two"));
	}

	@Test
	public void getEmptyListProperty() {
		List<String> list = constants.getListProperty("empty.list.property");
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testMultiResolve() {
		assertEquals("one,two,three_value1value1,my_value2.value1,StageSpecifics_value1.value2.value1", constants.getProperty("testMultiResolve"));
	}

	@Test
	public void callUnresolvedProperty() {
		assertEquals("${key1}", constants.getUnresolvedProperty("key2"));
	}

	@Test
	public void callNonExistingProperty() {
		assertNull(constants.getProperty("i.dont.exist"));
		assertEquals("default", constants.getString("i.dont.exist", "default"));
		assertEquals("default", constants.getProperty("i.dont.exist", "default"));
	}

	@Test
	public void setAndGetStringProperty() {
		constants.setProperty("property.type.string", "string");
		assertEquals("string", constants.getString("property.type.string", ""));
	}

	@Test
	public void setAndGetBooleanProperty() {
		constants.setProperty("property.type.boolean", "true");
		assertTrue(constants.getBoolean("property.type.boolean", false));
	}

	@Test
	public void setAndGetIntProperty() {
		constants.setProperty("property.type.int", "123");
		assertEquals(123, constants.getInt("property.type.int", 0));
	}

	@Test
	public void setAndGetLongProperty() {
		constants.setProperty("property.type.long", "123");
		assertEquals(123, constants.getLong("property.type.long", 0));
	}

	@Test
	public void setAndGetDoubleProperty() {
		constants.setProperty("property.type.double", "123.456");
		assertEquals(123.456, constants.getDouble("property.type.double", 0.0), 0);
	}

	@Test
	public void testUtf8EncodedPropertyFile() {
		assertEquals("‘’", constants.getProperty("encoding.utf8"));
	}

	@Test
	public void testYAML() {
		PropertyLoader properties = new PropertyLoader("ParserTestFiles/Types.yaml");

		assertAll(
				() -> assertEquals("value", properties.get("single")),
				() -> assertEquals("this is a nice value", properties.get("sentence")),
				() -> assertEquals("test1,test2,123", properties.get("list")),

				() -> assertEquals("1", properties.get("list.map.one")),
				() -> assertEquals("2", properties.get("list.map.two")),

				() -> assertEquals("1", properties.get("map.one")),
				() -> assertEquals("2", properties.get("map.two")),
				() -> assertEquals("3", properties.get("map.three")),

				() -> assertEquals("100", properties.get("array.Sergi.value")),
				() -> assertEquals("50", properties.get("array.Niels.value")),

				() -> assertEquals("1", properties.get("recursive.map.one")),
				() -> assertEquals("2", properties.get("recursive.map.two")),
				() -> assertEquals("3", properties.get("recursive.map.three")),

				() -> assertEquals("drie", properties.get("map.with.property")),
				() -> assertEquals("", properties.get("empty"))
		);
	}

	@Test
	public void wrongExtensionThrowError() {
		IllegalArgumentException eae = assertThrows(IllegalArgumentException.class, () -> new PropertyLoader("ParserTestFiles/Properties.extensionNonSupported"));
		assertEquals("Extension not supported: extensionNonSupported", eae.getMessage());
	}

	@Test
	public void testMultiFilesOverwritingProperties() {
		PropertyLoader yamlConstants = new PropertyLoader("ParserTestFiles/ResolveTest1.yaml");
		yamlConstants.load(PropertyLoaderTest.class.getClassLoader(), "ParserTestFiles/ResolveTest2.properties");
		yamlConstants.load(PropertyLoaderTest.class.getClassLoader(), "ParserTestFiles/ResolveTest3.yaml");

		assertEquals("Piet", yamlConstants.getProperty("Resolve1"));
		assertEquals("Pat", yamlConstants.getProperty("InverseResolve3"));
	}

	@Test
	public void testYamlFromPropertiesConverter() {
		PropertyLoader yamlConstants = new PropertyLoader("ParserTestFiles/YamlProperties.yaml");

		String p2y = PropertiesParser.parseFile(property2Reader(yamlConstants));

		YamlParser parser = new YamlParser();
		Properties yamlProperties = parser.load(new StringReader(p2y));

		assertThat(yamlConstants.entrySet(), everyItem(isIn(yamlProperties.entrySet())));
		assertThat(yamlProperties.entrySet(), everyItem(isIn(yamlConstants.entrySet())));
	}

	private StringReader property2Reader(PropertyLoader constants) {
		// convert the AppConstants to string, so that it can be read as a file
		StringBuilder stringBuilder = new StringBuilder();
		for (Object key : constants.keySet()) {
			stringBuilder.append(key).append("=").append(constants.getUnresolvedProperty((String) key)).append("\n");
		}
		return new StringReader(stringBuilder.toString());
	}
}
