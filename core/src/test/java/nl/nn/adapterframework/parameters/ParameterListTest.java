package nl.nn.adapterframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;

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
		assertNotNull(list.findParameter("key2"));
		assertNull(list.findParameter("doesnt-exist"));
		assertEquals(4, list.size());

		List<String> sortedList2 = new ArrayList<>();
		for (Parameter param : list) {
			sortedList2.add(param.getName());
		}
		assertEquals("[key1, key2, key4, key3]", sortedList2.toString());

//		assertSame(key2, list.remove("key2")); //cannot remove by name
		assertSame(key2, list.remove(1));
//		assertNull(list.remove("doesnt-exist"));

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

		Parameter key = new Parameter(null, "value");
		list.add(key);
		list.configure();
		Parameter keyWithName = list.get(4);
		assertEquals("parameter4", keyWithName.getName());
	}
}
