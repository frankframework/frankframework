package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

// TODO: This test no longer covers all relevant code. Please update.
public class FrankElementOwningRoleTest {
	private final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.compatibility.";
	private final String startClassName = PACKAGE + "Container";
	private final String digesterRulesFname = "doc/compatibility-digester-rules.xml";

	private FrankDocModel model;
	private ElementRole roleInterface;
	private FrankElement elementInInterface;
	private ElementRole owningRole;
	private FrankElement elementOwned;
	private ElementRole roleNotOwning;
	private FrankElement elementNotOwned;

	@Before
	public void setUp() {
		model = FrankDocModel.populate(digesterRulesFname, startClassName);
		roleInterface = model.findElementRole(new ElementRole.Key(PACKAGE + "IChildOk", "syntax1NameChildOk"));
		elementInInterface = model.findFrankElement(PACKAGE + "ChildElement");
		owningRole = model.findElementRole(new ElementRole.Key(PACKAGE + "NonInterfaceChildForOwningRole", "syntax1NameChild1"));
		elementOwned = model.findFrankElement(PACKAGE + "NonInterfaceChildForOwningRole");
		roleNotOwning = model.findElementRole(new ElementRole.Key(PACKAGE + "NonInterfaceChildNoOwningRoleBecauseDeprecated", "syntax1NameChild2"));
		elementNotOwned = model.findFrankElement(PACKAGE + "NonInterfaceChildNoOwningRoleBecauseDeprecated");
	}

	@Test
	public void testExpectedRolesAndFrankElementsExist() {
		assertTrue(roleContainsElement(roleInterface, elementInInterface));
		assertTrue(roleContainsElement(owningRole, elementOwned));
		assertTrue(roleContainsElement(roleNotOwning, elementNotOwned));
	}

	private boolean roleContainsElement(ElementRole role, FrankElement frankElement) {
		return role.getElementType().getMembers().containsKey(frankElement.getFullName());
	}

	@Test
	public void whenElementInInterfaceTypeThenNotOwned() {
		assertEquals("ChildElementSyntax1NameChildOk", elementInInterface.getXsdElementName(roleInterface));
	}

	@Test
	public void whenElementInDeprecatedConfigChildThenNotOwned() {
		assertEquals("Syntax1NameChild2", elementNotOwned.getXsdElementName(roleNotOwning));
	}

	@Test
	public void whenElementNotInInterfaceAndNonDeprecatedConfigChildThenOwned() {
		assertEquals("Syntax1NameChild1", elementOwned.getXsdElementName(owningRole));
	}
}
