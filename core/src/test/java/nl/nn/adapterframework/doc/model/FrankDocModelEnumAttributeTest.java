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

public class FrankDocModelEnumAttributeTest {
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
		assertTrue(actual.containsKey("childAttribute"));
		assertEquals(2, actual.size());
	}

	@Test
	public void testPopulate() {
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", CHILD, classRepository);
		FrankElement child = model.findFrankElement(CHILD);
		assertNotNull(child);
		AttributeValues myEnum = model.findAttributeValues(MY_ENUM);
		assertEquals(MY_ENUM, myEnum.getFullName());
		assertArrayEquals(new String[] {"ONE", "TWO", "THREE"}, myEnum.getValues().toArray());
		FrankAttribute childAttribute = child.getAttributes(ElementChild.ALL).get(0);
		assertEquals("childAttribute", childAttribute.getName());
		assertEquals(myEnum, childAttribute.getAttributeValues());
	}
}
