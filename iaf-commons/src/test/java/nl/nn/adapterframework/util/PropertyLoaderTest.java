package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PropertyLoaderTest {

	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private String propertyFile = "StringResolver.properties";
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
		assertEquals(null, constants.getProperty("i.dont.exist"));
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
		assertEquals(true, constants.getBoolean("property.type.boolean", false));
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
}
