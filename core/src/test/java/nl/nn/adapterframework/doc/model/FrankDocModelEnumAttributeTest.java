package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelEnumAttributeTest {
	@Test
	public void testGetEnumGettersByAttributeName() {
		Map<String, Method> actual = FrankDocModel.getEnumGettersByAttributeName(Utils.getClass("nl.nn.adapterframework.doc.testtarget.enumattr.Child"));
		assertTrue(actual.containsKey("parentAttribute"));
		assertTrue(actual.containsKey("childAttribute"));
		assertEquals(2, actual.size());
	}
}
