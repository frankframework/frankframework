package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.doclet.FrankClassRepository;
import nl.nn.adapterframework.doc.doclet.FrankMethod;

public class FrankDocModelAttributeTypeTest {
	private static final String CHILD = "nl.nn.adapterframework.doc.testtarget.enumattr.Child";
	private static final String MY_ENUM = "nl.nn.adapterframework.doc.testtarget.enumattr.MyEnum";

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = FrankClassRepository.getReflectInstance();
	}

	@Test
	public void testGetEnumGettersByAttributeName() throws Exception {
		Map<String, FrankMethod> actual = FrankDocModel.getEnumGettersByAttributeName(classRepository.findClass(CHILD));
		assertTrue(actual.containsKey("parentAttribute"));
		assertTrue(actual.containsKey("childStringAttribute"));
		assertEquals(2, actual.size());
	}

	@Test
	public void testPopulate() {
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", CHILD, classRepository);
		FrankElement child = model.findFrankElement(CHILD);
		assertNotNull(child);
		// Test the attribute with a value list, which is of type STRING.
		AttributeValues myEnum = model.findAttributeValues(MY_ENUM);
		assertEquals(MY_ENUM, myEnum.getFullName());
		assertArrayEquals(new String[] {"ONE", "TWO", "THREE"}, myEnum.getValues().toArray());
		// By fixing the list index like this, we test that the attributes are sorted correctly.
		FrankAttribute childAttribute = child.getAttributes(ElementChild.ALL).get(1);
		assertEquals("childStringAttribute", childAttribute.getName());
		assertEquals(myEnum, childAttribute.getAttributeValues());
		assertEquals(AttributeType.STRING, childAttribute.getAttributeType());
		// Test the int attribute
		childAttribute = child.getAttributes(ElementChild.ALL).get(0);
		assertEquals(AttributeType.INT, childAttribute.getAttributeType());
	}
}
