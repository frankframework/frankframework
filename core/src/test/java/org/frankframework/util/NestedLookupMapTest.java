package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class NestedLookupMapTest {

	@Test
	void getCaseInsensitive() {
		// Arrange
		Map<String, Object> map = new NestedLookupMap<>();
		map.put("MyKey", 1);

		// Act / Assert
		assertEquals(1, map.get("MyKey"));
		assertEquals(1, map.get("mykey"));
		assertEquals(1, map.get("MYKEY"));
	}

	@Test
	void getFromNestedMap() {
		// Arrange
		Map<String, Object> nestedMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		nestedMap.put("a", 1);
		nestedMap.put("b", 2);

		Map<String, Object> map = new NestedLookupMap<>();
		map.put("nested", nestedMap);
		map.put("nested.b", 3);

		// Act / Assert
		assertEquals(1, map.get("nested.a"));
		assertEquals(1, map.get("Nested.A"));
		assertEquals(1, map.get("NESTED.A"));
		assertEquals(3, map.get("nested.b")); // Key from nested map is overridden by the direct key `nested.b` in the top level map
		assertNull(map.get("nested.nothing"));
		assertNull(map.get("nested.b.nothing"));
	}

	@Test
	void supportsNullValues() {
		// Arrange
		Map<String, Object> map = new NestedLookupMap<>();

		// Act
		map.put("a", null);

		// Assert
		assertTrue(map.containsKey("a"));
		assertNull(map.get("a"));
	}

	@Test
	void nullOrNonStringKeyAlwaysReturnsNullValue() {
		// Arrange
		Map<String, Object> map = new NestedLookupMap<>();
		map.put("a", null);

		// Act / Assert
		assertNull(map.get(null));
		assertNull(map.get(new Object()));
	}
}
