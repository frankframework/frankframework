package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;

public class ParameterListTest {

	@Test
	public void testParameterList() throws Exception {
		ParameterList list = new ParameterList();

		Parameter key2 = new Parameter("key2", "value2");
		list.add(new Parameter("key1", "value1"));
		list.add(key2);
		list.add(new Parameter("key4", "value4"));
		list.add(new Parameter("key3", "value3"));
		list.configure();

		assertNotNull(list.findParameter("key1"));
		assertTrue(list.hasParameter("key1"), "Expected to find parameter [key1] in parameter list");
		assertNotNull(list.findParameter("key2"));
		assertTrue(list.hasParameter("key2"), "Expected to find parameter [key2] in parameter list");
		assertNull(list.findParameter("doesnt-exist"));
		assertFalse(list.hasParameter("doesnt-exist"), "Expected not to find parameter [doesnt-exist] in parameter list");
		assertEquals(4, list.size());

		List<String> sortedList2 = new ArrayList<>();
		for (IParameter param : list) {
			sortedList2.add(param.getName());
		}
		assertEquals("[key1, key2, key4, key3]", sortedList2.toString());

		assertSame(key2, list.remove("key2")); //cannot remove by name
		assertNull(list.remove("doesnt-exist"));

		assertEquals("value3", list.findParameter("key3").getValue());
		assertEquals("value1", list.getParameter(0).getValue());
		assertEquals("value4", list.getParameter(1).getValue());
		assertEquals("value3", list.getParameter(2).getValue());
	}

	@Test
	public void testParamWithoutName() throws ConfigurationException {
		ParameterList list = new ParameterList();
		list.add(new Parameter(null, "dummy-1"));
		list.add(new Parameter(null, "dummy-2"));
		list.add(new Parameter(null, "dummy-3"));
		list.add(new Parameter(null, "dummy-4"));

		IParameter key = new Parameter(null, "value");
		list.add(key);
		list.configure();
		IParameter keyWithName = list.getParameter(4);
		assertEquals("parameter4", keyWithName.getName());
	}
}
