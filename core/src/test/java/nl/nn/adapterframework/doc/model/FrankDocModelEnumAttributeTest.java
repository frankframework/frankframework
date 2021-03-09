package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelEnumAttributeTest {
	private static final String CHILD = "nl.nn.adapterframework.doc.testtarget.enumattr.Child";
	private static final String MY_ENUM = "nl.nn.adapterframework.doc.testtarget.enumattr.MyEnum";

	@Test
	public void testGetEnumGettersByAttributeName() {
		Map<String, Method> actual = FrankDocModel.getEnumGettersByAttributeName(Utils.getClass(CHILD));
		assertTrue(actual.containsKey("parentAttribute"));
		assertTrue(actual.containsKey("childAttribute"));
		assertEquals(2, actual.size());
	}

	@Test
	public void testPopulate() {
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", CHILD);
		FrankElement child = model.findFrankElement(CHILD);
		assertNotNull(child);
		AttributeValuesList myEnum = model.findAttributeValuesList(MY_ENUM);
		assertEquals(MY_ENUM, myEnum.getFullName());
		assertArrayEquals(new String[] {"ONE", "TWO", "THREE"}, myEnum.getValues().toArray());
		FrankAttribute childAttribute = child.getAttributes(ElementChild.ALL).get(0);
		assertEquals("childAttribute", childAttribute.getName());
		assertEquals(myEnum, childAttribute.getAttributeValuesList());
	}
}
