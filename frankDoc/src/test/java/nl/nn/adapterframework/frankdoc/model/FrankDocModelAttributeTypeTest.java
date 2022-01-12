package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class FrankDocModelAttributeTypeTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.enumattr.";
	private static final String CHILD = PACKAGE + "Child";
	private static final String MY_ENUM = PACKAGE + "MyEnum";

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
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
		AttributeEnum myEnum = model.findAttributeEnum(MY_ENUM);
		assertEquals(MY_ENUM, myEnum.getFullName());
		String[] actualLabels = myEnum.getValues().stream().map(AttributeEnumValue::getLabel).collect(Collectors.toList()).toArray(new String[] {});
		assertArrayEquals(new String[] {"TWO", "customLabelOne", "THREE"}, actualLabels);
		AttributeEnumValue v = myEnum.getValues().get(0);
		// This one has no annotation and no description.
		assertEquals("TWO", v.getLabel());
		assertNull(v.getDescription());
		// This one has a custom label and a description
		v = myEnum.getValues().get(1);
		assertEquals("customLabelOne", v.getLabel());
		assertEquals("Description of customLabelOne", v.getDescription());

		// By fixing the list index like this, we test that the attributes are sorted correctly.
		FrankAttribute childAttribute = child.getAttributes(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertEquals("childStringAttribute", childAttribute.getName());
		assertEquals(myEnum, childAttribute.getAttributeEnum());
		assertEquals(AttributeType.STRING, childAttribute.getAttributeType());
		// Test the int attribute
		childAttribute = child.getAttributes(ElementChild.ALL_NOT_EXCLUDED).get(1);
		assertEquals(AttributeType.INT, childAttribute.getAttributeType());
	}
}
