package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.ParameterBuilder;

public class ParameterValueListTest {

	@Test
	public void testParameterValueList() {
		ParameterValueList list = new ParameterValueList();
		ParameterValue key2 = new ResolvedParameterValue("key2", "value2");
		list.add(new ResolvedParameterValue("key1", "value1"));
		list.add(key2);
		list.add(new ResolvedParameterValue("key4", "value4"));
		list.add(new ResolvedParameterValue("key3", "value3"));

		assertTrue(list.contains("key1"));
		assertTrue(list.contains("key2"));
		assertFalse(list.contains("doesnt-exist"));
		assertEquals(4, list.size());
		assertEquals("[value1, value2, value4, value3]", list.getValueMap().values().toString());

		List<String> sortedList2 = new ArrayList<>();
		for (ParameterValue param : list) {
			sortedList2.add(param.getName());
		}
		assertEquals("[key1, key2, key4, key3]", sortedList2.toString());

		assertSame(key2, list.remove("key2"));
		assertNull(list.remove("doesnt-exist"));

		assertEquals("value3", list.get("key3").getValue());
		assertEquals("value1", list.getParameterValue(0).getValue());
		assertEquals("value4", list.getParameterValue(1).getValue());
		assertEquals("value3", list.getParameterValue(2).getValue());
	}

	public static class ResolvedParameterValue extends ParameterValue {

		ResolvedParameterValue(String name, Object value) {
			super(new Parameter(), value);
			getDefinition().setName(name);
		}
	}

	@Test
	public void testDuplicateNames() throws Exception {
		ParameterList list = new ParameterList();
		list.add(new Parameter("name", "dummy-1"));
		list.add(new Parameter("name", "dummy-2"));
		list.add(new Parameter("name", "dummy-3"));
		list.configure();

		assertEquals(3, list.size());

		ParameterValueList pvl = ParameterBuilder.getPVL(list);
		assertEquals(3, pvl.size());
	}
}
