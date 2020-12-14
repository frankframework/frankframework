package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Class {@link FrankElement} has many different method to get children
 * and to search ancestors that have children. The tests in this class
 * are to test all these methods.
 * @author martijn
 *
 */
public class AncestorKindsTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.sparse.";

	private FrankDocModel model;

	@Before
	public void setUp() {
		model = FrankDocModel.populate("doc/sparse-digester-rules.xml", PACKAGE + "ContainerChild");
	}

	@Test
	public void testConfigChildrenOfPackageSparse() {
		ConfigChild child = model.findFrankElement(PACKAGE + "ContainerChild")
				.getConfigChildren(ALL).get(0);
		assertFalse(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated")
				.getConfigChildren(ALL).get(0);
		assertTrue(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerAncestor")
				.getConfigChildren(ALL).get(0);
		assertFalse(child.isDeprecated());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeChild")
				.getConfigChildren(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated")
				.getConfigChildren(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeAncestor")
				.getConfigChildren(ALL).size());
	}

	@Test
	public void testAttributesOfPackageSparse() {
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerChild")
				.getAttributes(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated")
				.getAttributes(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerAncestor")
				.getAttributes(ALL).size());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeChild")
				.getAttributes(ALL).get(0).isDeprecated());
		assertTrue(model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated")
				.getAttributes(ALL).get(0).isDeprecated());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeAncestor")
				.getAttributes(ALL).get(0).isDeprecated());		
	}
}
