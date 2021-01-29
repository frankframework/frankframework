package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static nl.nn.adapterframework.doc.model.ElementChild.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class ElementRoleIntegrationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.role.";

	private static FrankDocModel model;

	@BeforeClass
	public static void setUp() {
		model = FrankDocModel.populate("doc/role-digester-rules.xml", PACKAGE + "Master");
	}

	/**
	 * Test that the sequence numbers assigned to {@link ElementRole} objects are
	 * determiniestic. There is no logic in the chosen numbers other then the
	 * sequence in which the {@link ElementRole} are created.
	 */
	@Test
	public void testRolesFromJavaInterfaces() {
		ElementRole er = model.findElementRole(PACKAGE + "Interface1", "role1");
		assertFalse(er.isSuperseded());
		assertFalse(er.isDeprecated());
		assertNotNull(er);
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element_2", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role2");
		assertFalse(er.isSuperseded());
		assertFalse(er.isDeprecated());
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element_2", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface1", "role2");
		assertFalse(er.isSuperseded());
		assertFalse(er.isDeprecated());
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role1");
		assertFalse(er.isSuperseded());
		assertFalse(er.isDeprecated());
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element", er.createXsdElementName("Element"));		
	}

	@Test
	public void testGetElementTypeMemberChildRoles() {
		ElementRole i1r2 = model.findElementRole(PACKAGE + "Interface1", "role2");
		ElementRole i2r1 = model.findElementRole(PACKAGE + "Interface2", "role1");
		ElementRole superseded = model.findElementRole(PACKAGE + "ElementWithSupersededRole", "roleSuperseded");
		ElementRole remaining = model.findElementRole(PACKAGE + "ElementWithSupersededRole", "roleNonInterface");
		ElementRole simple = model.findElementRole(PACKAGE + "SimpleElement", "roleNonInterface");
		checkElementRolesAre("Interface1", Arrays.asList(i2r1, i1r2, remaining, simple, superseded));
	}

	private void checkElementRolesAre(String elementTypeSimpleName, List<ElementRole> expected) {
		ElementType elementType = model.findElementType(PACKAGE + elementTypeSimpleName);
		List<ElementRole> actual = model.getElementTypeMemberChildRoles(elementType, ALL, NONE, f -> true);
		assertEquals(expected.size(), actual.size());
		for(int i = 0; i < expected.size(); i++) {
			assertSame(expected.get(i), actual.get(i));
		}
	}

	@Test
	public void testSupersededAndDeprecated() {
		ElementRole superseded = model.findElementRole(PACKAGE + "ElementWithSupersededRole", "roleSuperseded");
		assertTrue(superseded.isSuperseded());
		assertTrue(superseded.isDeprecated());
		ElementRole remaining = model.findElementRole(PACKAGE + "ElementWithSupersededRole", "roleNonInterface");
		assertFalse(remaining.isSuperseded());
		assertFalse(remaining.isDeprecated());
		ElementRole simple = model.findElementRole(PACKAGE + "SimpleElement", "roleNonInterface");
		assertFalse(simple.isSuperseded());
		assertTrue(simple.isDeprecated());
	}
}
