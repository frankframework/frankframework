/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/
package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;
import static nl.nn.adapterframework.doc.model.ElementChild.IN_XSD;
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
		ConfigChild child = model.findFrankElement(PACKAGE + "ContainerChild").getConfigChildren(ALL).get(0);
		assertFalse(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated").getConfigChildren(ALL).get(0);
		assertTrue(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerAncestor").getConfigChildren(ALL).get(0);
		assertFalse(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedConfigChildren").getConfigChildren(ALL).get(0);
		assertTrue(child.isDeprecated());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeChild").getConfigChildren(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated").getConfigChildren(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeAncestor").getConfigChildren(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedAttributes").getConfigChildren(ALL).size());
	}

	@Test
	public void testAttributesOfPackageSparse() {
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerChild").getAttributes(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated").getAttributes(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerAncestor").getAttributes(ALL).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedConfigChildren").getAttributes(ALL).size());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeChild").getAttributes(ALL).get(0).isDeprecated());
		assertTrue(model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated").getAttributes(ALL).get(0).isDeprecated());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeAncestor").getAttributes(ALL).get(0).isDeprecated());
		assertTrue(model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedAttributes").getAttributes(ALL).get(0).isDeprecated());
	}

	@Test
	public void testFindingNextAncestorWithConfigChildren() {
		FrankElement child = model.findFrankElement(PACKAGE + "ContainerChild");
		FrankElement ancestorNotDeprecated = child.getNextAncestorThatHasConfigChildren(IN_XSD);
		assertEquals("ContainerAncestor", ancestorNotDeprecated.getSimpleName());
		FrankElement ancestorAll = child.getNextAncestorThatHasConfigChildren(ALL);
		assertEquals("ContainerNoAncestorBecauseChildrenDeprecated", ancestorAll.getSimpleName());
	}

	@Test
	public void testFindingNextAncestorWithAttributes() {
		FrankElement child = model.findFrankElement(PACKAGE + "AttributeChild");
		FrankElement ancestorNotDeprecated = child.getNextAncestorThatHasAttributes(IN_XSD);
		assertEquals("AttributeAncestor", ancestorNotDeprecated.getSimpleName());
		FrankElement ancestorAll = child.getNextAncestorThatHasAttributes(ALL);
		assertEquals("AttributeNoAncestorBecauseAttributesDeprecated", ancestorAll.getSimpleName());
	}

	@Test
	public void testAncestorWithConfigChildrenOrAttributes() {
		assertTrue(model.findFrankElement(PACKAGE + "ContainerChild").hasAncestorThatHasConfigChildrenOrAttributes(ALL));
		assertFalse(model.findFrankElement(PACKAGE + "ContainerAncestor").hasAncestorThatHasConfigChildrenOrAttributes(IN_XSD));
		assertTrue(model.findFrankElement(PACKAGE + "ContainerAncestor").hasAncestorThatHasConfigChildrenOrAttributes(ALL));
		assertTrue(model.findFrankElement(PACKAGE + "AttributeChild").hasAncestorThatHasConfigChildrenOrAttributes(ALL));
		assertFalse(model.findFrankElement(PACKAGE + "AttributeAncestor").hasAncestorThatHasConfigChildrenOrAttributes(IN_XSD));
		assertTrue(model.findFrankElement(PACKAGE + "AttributeAncestor").hasAncestorThatHasConfigChildrenOrAttributes(ALL));
	}
}
