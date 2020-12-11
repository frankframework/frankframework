package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ElementRoleIntegrationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.role.";

	@Test
	public void testExampleClassesTesttargetRole() {
		FrankDocModel model = FrankDocModel.populate("doc/role-digester-rules.xml", PACKAGE + "Master");
		// We have two interfaces and two syntax 1 names (roles). We try all combinations
		assertEquals(4, model.getAllElementRoles().size());
		// The element roles from Master are created first. They match interface
		// numbers and role numbers.
		ElementRole er = model.findElementRole(PACKAGE + "Interface1", "role1");
		assertNotNull(er);
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role2");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element", er.createXsdElementName("Element"));
		// The element roles from Container are created later. These interchange
		// the interface numbers and role numbers. The syntax 1 names are reused,
		// so we expect sequence numbers to be added in the element names.
		er = model.findElementRole(PACKAGE + "Interface1", "role2");
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element_2", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role1");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element_2", er.createXsdElementName("Element"));
	}
}
