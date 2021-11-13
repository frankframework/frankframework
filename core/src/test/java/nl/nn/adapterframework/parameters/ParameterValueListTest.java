package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import nl.nn.adapterframework.core.ParameterException;

public class ParameterValueListTest {

	@Test
	public void testParameterValueList() throws ParameterException {
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
		assertEquals("[value1, value2, value3, value4]", list.getValueMap().values().toString());

		List<String> sortedList = new ArrayList<>();
		list.forAllParameters(new IParameterHandler() {
			@Override
			public void handleParam(String paramName, Object paramValue) throws ParameterException {
				sortedList.add(paramName);
			}
		});
		assertEquals("[key1, key2, key4, key3]", sortedList.toString());

		List<String> sortedList2 = new ArrayList<>();
		for (ParameterValue param : list) {
			sortedList2.add(param.getName());
		}
		assertEquals("[key1, key2, key4, key3]", sortedList2.toString());

		assertTrue(key2 == list.removeParameterValue("key2"));
		assertNull(list.removeParameterValue("doesnt-exist"));

		assertEquals("value3", list.getParameterValue("key3").getValue());
		assertEquals("value1", list.getParameterValue(0).getValue());
	}

	public static class ResolvedParameterValue extends ParameterValue {

		ResolvedParameterValue(String name, Object value) {
			super(new Parameter(), value);
			getDefinition().setName(name);
		}
	}
}
