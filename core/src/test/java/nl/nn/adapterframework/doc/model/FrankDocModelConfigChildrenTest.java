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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelConfigChildrenTest {
	private static String CONTAINER = "nl.nn.adapterframework.doc.testtarget.children.Container";
	private static String CONTAINER_DERIVED = "nl.nn.adapterframework.doc.testtarget.children.ContainerDerived";
	private static String CONTAINER_OTHER = "nl.nn.adapterframework.doc.testtarget.children.ContainerOther";
	
	private FrankDocModel instance;
	private List<ConfigChild> configChildren;
	private List<ConfigChild> configChildrenOfDerived;

	@Before
	public void setUp() throws SAXException, IOException, ReflectiveOperationException {
		instance = new FrankDocModel();
		instance.createConfigChildDescriptorsFrom("doc/simple-digester-rules.xml");
		instance.findOrCreateElementType(Utils.getClass(CONTAINER));
		instance.findOrCreateElementType(Utils.getClass(CONTAINER_DERIVED));
		instance.findOrCreateElementType(Utils.getClass(CONTAINER_OTHER));
		instance.setOverriddenFrom();
		configChildren = instance.getAllElements().get(CONTAINER).getConfigChildren(ALL);
		configChildrenOfDerived = instance.getAllElements().get(CONTAINER_DERIVED).getConfigChildren(ALL);
	}

	@Test
	public void whenConfigChildMethodThenConfigChildProduced() throws Exception {
		ConfigChild actual = selectChild("roleNameChild");
		assertEquals("roleNameChild", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(100, actual.getOrder());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertTrue(IN_XSD.test(actual));
	}

	private ConfigChild selectChild(String name) {
		List<ConfigChild> selected = configChildren.stream()
				.filter(c -> c.getRoleName().equals(name))
				.collect(Collectors.toList());
		assertEquals(1, selected.size());
		return selected.get(0);
	}

	@Test
	public void whenConfigChildMethodDeprecatedThenConfigChildDeprecated() {
		ConfigChild actual = selectChild("roleNameDeprecatedChild");
		assertEquals("roleNameDeprecatedChild", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(200, actual.getOrder());
		assertFalse(actual.isAllowMultiple());
		assertTrue(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildInheritedFromProtectedThenChildAndNotOverriddenButAnnotationsInherited() {
		ConfigChild actual = selectChild("roleNameInheritedChilds");
		assertEquals("roleNameInheritedChilds", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChild", actual.getElementType().getSimpleName());
		assertFalse(actual.isDocumented());
		assertEquals(50, actual.getOrder());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
		assertFalse(actual.isTechnicalOverride());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void whenIbisDocOnDerivedMethodThenStillOrderFromIbisDoc() {
		ConfigChild actual = selectChild("roleNameInheritedChildDocOnDerived");
		assertEquals("roleNameInheritedChildDocOnDerived", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChildDocOnDerived", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(70, actual.getOrder());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isMandatory());
		assertFalse(actual.isDeprecated());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
		assertFalse(actual.isTechnicalOverride());
	}

	@Test
	public void whenIbisDocBothOnParentAndDerivedThenDerivedValueTaken() {
		ConfigChild actual = selectChild("roleNameInheritedChildDocWithOrderOverride");
		assertEquals("roleNameInheritedChildDocWithOrderOverride", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChildDocWithOrderOverride", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(10, actual.getOrder());
		assertFalse(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
		assertTrue(actual.isTechnicalOverride());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void whenConfigChildOnAncestorNotOverriddenThenOmitted() {
		checkChildNotPresent("roleNameInheritedWithoutOverride");
	}

	private void checkChildNotPresent(String name) {
		boolean result = configChildren.stream()
				.filter(c -> c.getRoleName().equals(name))
				.collect(Collectors.counting())
				.longValue() == 0;
		assertTrue(result);
	}

	@Test
	public void onlyWantedConfigChildrenProduced() {
		assertEquals(7, configChildren.size());
	}

	@Test
	public void whenConfigChildOverriddenTwiceTheGrandparentTaken() {
		ConfigChild grandChild = checkAndFindGrandChild("roleNameInheritedChildDocWithOrderOverride");
		assertEquals("Container", grandChild.getOverriddenFrom().getSimpleName());
	}

	private ConfigChild checkAndFindGrandChild(final String roleName) {
		List<ConfigChild> grandChildList = configChildrenOfDerived.stream()
				.filter(c -> c.getRoleName().equals(roleName))
				.collect(Collectors.toList());
		assertEquals(1, grandChildList.size());
		return grandChildList.get(0);
	}

	@Test
	public void whenConfigChildOverriddenFromGrandparentThenGrandparentTaken() {
		ConfigChild grandChild = checkAndFindGrandChild("roleNameInheritedWithoutOverride");
		assertEquals("ContainerParent", grandChild.getOverriddenFrom().getSimpleName());
	}

	@Test
	public void whenConfigChildOverriddenNotDocumentedThenChildCreatedButNotSelected() {
		ConfigChild actual = selectChild("roleNameInheritedChildNonSelected");
		assertEquals(120, actual.getOrder());
		assertFalse(actual.isDocumented());
		assertFalse(actual.isDeprecated());
		assertNotNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildOverridesWithoutDocsThenNotDocumentedButAnnotationsInherited() {
		ConfigChild actual = selectChild("roleNameChildOverriddenOnlyParentAnnotated");
		assertEquals("roleNameChildOverriddenOnlyParentAnnotated", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("ChildOverriddenOnlyParentAnnotated", actual.getElementType().getSimpleName());
		assertFalse(actual.isDocumented());
		assertEquals(110, actual.getOrder());
		assertFalse(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
		// Not selected because deprecated
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenInheritedConfigChildNotDeprecatedInheritedFromDeprecatedThenNotDeprecated() throws Exception {
		ConfigChild theConfigChild = instance.findOrCreateFrankElement(Utils.getClass(CONTAINER_OTHER))
				.getConfigChildren(c -> ((ConfigChild)c).getOrder() == 110).get(0);
		assertFalse(theConfigChild.isDeprecated());
	}
}
