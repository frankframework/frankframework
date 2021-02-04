package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
