package nl.nn.adapterframework.doc.model;

import static java.util.Arrays.asList;
import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static nl.nn.adapterframework.doc.model.ElementChild.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ElementRoleIntegrationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.role.";

	private FrankDocModel model;

	@Before
	public void setUp() {
		model = FrankDocModel.populate("doc/role-digester-rules.xml", PACKAGE + "Master");
	}

	@Test
	public void testExampleClassesTesttargetRole() {
		// We have two interfaces and two syntax 1 names (roles). We try all combinations
		assertEquals(4, model.getAllElementRoles().size());
		// The element roles from Container are created first. They interchange interface
		// numbers and role numbers.
		ElementRole er = model.findElementRole(PACKAGE + "Interface1", "role2");
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role1");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element", er.createXsdElementName("Element"));
		
		// The element roles from Master are created later. These match
		// the interface numbers and role numbers. The syntax 1 names are reused,
		// so we expect sequence numbers to be added in the element names.
		er = model.findElementRole(PACKAGE + "Interface1", "role1");
		assertNotNull(er);
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role1", er.getSyntax1Name());
		assertEquals("Role1Element_2", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role2");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role2", er.getSyntax1Name());
		assertEquals("Role2Element_2", er.createXsdElementName("Element"));
	}

	@Test
	public void testGetElementTypeMemberChildRoles() {
		ElementRole i1r2 = model.findElementRole(PACKAGE + "Interface1", "role2");
		ElementRole i2r1 = model.findElementRole(PACKAGE + "Interface2", "role1");
		checkElementRolesAre("Interface1", asList(i2r1, i1r2));
	}

	private void checkElementRolesAre(String elementTypeSimpleName, List<ElementRole> expected) {
		ElementType elementType = model.findElementType(PACKAGE + elementTypeSimpleName);
		List<ElementRole> actual = model.getElementTypeMemberChildRoles(elementType, ALL, NONE, f -> true);
		assertEquals(expected.size(), actual.size());
		for(int i = 0; i < expected.size(); i++) {
			assertSame(expected.get(i), actual.get(i));
		}
	}
}
