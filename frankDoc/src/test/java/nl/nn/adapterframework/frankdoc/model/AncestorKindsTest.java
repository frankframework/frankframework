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
package nl.nn.adapterframework.frankdoc.model;

import static nl.nn.adapterframework.frankdoc.model.ElementChild.ALL_NOT_EXCLUDED;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.IN_XSD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

/**
 * Class {@link FrankElement} has many different method to get children
 * and to search ancestors that have children. The tests in this class
 * are to test all these methods.
 * @author martijn
 *
 */
public class AncestorKindsTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.sparse.";

	private FrankDocModel model;

	@Before
	public void setUp() {
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		model = FrankDocModel.populate("doc/sparse-digester-rules.xml", PACKAGE + "ContainerChild", classRepository);
	}

	@Test
	public void testConfigChildrenOfPackageSparse() {
		ConfigChild child = model.findFrankElement(PACKAGE + "ContainerChild").getConfigChildren(ALL_NOT_EXCLUDED).get(0);
		assertFalse(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated").getConfigChildren(ALL_NOT_EXCLUDED).get(0);
		assertTrue(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "ContainerAncestor").getConfigChildren(ALL_NOT_EXCLUDED).get(0);
		assertFalse(child.isDeprecated());
		child = model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedConfigChildren").getConfigChildren(ALL_NOT_EXCLUDED).get(0);
		assertTrue(child.isDeprecated());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeChild").getConfigChildren(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated").getConfigChildren(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "AttributeAncestor").getConfigChildren(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedAttributes").getConfigChildren(ALL_NOT_EXCLUDED).size());
	}

	@Test
	public void testAttributesOfPackageSparse() {
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerChild").getAttributes(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerNoAncestorBecauseChildrenDeprecated").getAttributes(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "ContainerAncestor").getAttributes(ALL_NOT_EXCLUDED).size());
		assertEquals(0, model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedConfigChildren").getAttributes(ALL_NOT_EXCLUDED).size());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeChild").getAttributes(ALL_NOT_EXCLUDED).get(0).isDeprecated());
		assertTrue(model.findFrankElement(PACKAGE + "AttributeNoAncestorBecauseAttributesDeprecated").getAttributes(ALL_NOT_EXCLUDED).get(0).isDeprecated());
		assertFalse(model.findFrankElement(PACKAGE + "AttributeAncestor").getAttributes(ALL_NOT_EXCLUDED).get(0).isDeprecated());
		assertTrue(model.findFrankElement(PACKAGE + "GrandParentWithDeprecatedAttributes").getAttributes(ALL_NOT_EXCLUDED).get(0).isDeprecated());
	}

	@Test
	public void testFindingNextAncestorWithConfigChildren() {
		FrankElement child = model.findFrankElement(PACKAGE + "ContainerChild");
		FrankElement ancestorNotDeprecated = child.getNextAncestorThatHasConfigChildren(IN_XSD);
		assertEquals("ContainerAncestor", ancestorNotDeprecated.getSimpleName());
		FrankElement ancestorAll = child.getNextAncestorThatHasConfigChildren(ALL_NOT_EXCLUDED);
		assertEquals("ContainerNoAncestorBecauseChildrenDeprecated", ancestorAll.getSimpleName());
	}

	@Test
	public void testFindingNextAncestorWithAttributes() {
		FrankElement child = model.findFrankElement(PACKAGE + "AttributeChild");
		FrankElement ancestorNotDeprecated = child.getNextAncestorThatHasAttributes(IN_XSD);
		assertEquals("AttributeAncestor", ancestorNotDeprecated.getSimpleName());
		FrankElement ancestorAll = child.getNextAncestorThatHasAttributes(ALL_NOT_EXCLUDED);
		assertEquals("AttributeNoAncestorBecauseAttributesDeprecated", ancestorAll.getSimpleName());
	}

	@Test
	public void testAncestorWithConfigChildrenOrAttributes() {
		assertTrue(model.findFrankElement(PACKAGE + "ContainerChild").hasAncestorThatHasConfigChildrenOrAttributes(ALL_NOT_EXCLUDED));
		assertFalse(model.findFrankElement(PACKAGE + "ContainerAncestor").hasAncestorThatHasConfigChildrenOrAttributes(IN_XSD));
		assertTrue(model.findFrankElement(PACKAGE + "ContainerAncestor").hasAncestorThatHasConfigChildrenOrAttributes(ALL_NOT_EXCLUDED));
		assertTrue(model.findFrankElement(PACKAGE + "AttributeChild").hasAncestorThatHasConfigChildrenOrAttributes(ALL_NOT_EXCLUDED));
		assertFalse(model.findFrankElement(PACKAGE + "AttributeAncestor").hasAncestorThatHasConfigChildrenOrAttributes(IN_XSD));
		assertTrue(model.findFrankElement(PACKAGE + "AttributeAncestor").hasAncestorThatHasConfigChildrenOrAttributes(ALL_NOT_EXCLUDED));
	}
}
