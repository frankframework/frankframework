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
import static org.junit.Assert.assertArrayEquals;
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

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class FrankDocModelConfigChildrenTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.children.";
	private static String CONTAINER = PACKAGE + "Container";
	private static String CONTAINER_DERIVED = PACKAGE + "ContainerDerived";
	private static String CONTAINER_OTHER = PACKAGE + "ContainerOther";
	
	private FrankDocModel instance;
	private FrankClassRepository classRepository;
	private List<ConfigChild> configChildren;
	private List<ConfigChild> configChildrenOfDerived;

	@Before
	public void setUp() throws SAXException, IOException, FrankDocException {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		instance = new FrankDocModel(classRepository);
		instance.createConfigChildDescriptorsFrom("doc/simple-digester-rules.xml");
		instance.findOrCreateElementType(classRepository.findClass(CONTAINER));
		instance.findOrCreateElementType(classRepository.findClass(CONTAINER_DERIVED));
		instance.findOrCreateElementType(classRepository.findClass(CONTAINER_OTHER));
		instance.setOverriddenFrom();
		configChildren = instance.getAllElements().get(CONTAINER).getConfigChildren(ALL_NOT_EXCLUDED);
		configChildrenOfDerived = instance.getAllElements().get(CONTAINER_DERIVED).getConfigChildren(ALL_NOT_EXCLUDED);
	}

	@Test
	public void whenTextConfigChildMethodThenTextConfigChildProduced() throws Exception {
		ConfigChild rawActual = selectChild("roleNameText");
		assertTrue(rawActual instanceof TextConfigChild);
		TextConfigChild actual = (TextConfigChild) rawActual;
		assertEquals("roleNameText", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertFalse(actual.isDocumented());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertTrue(IN_XSD.test(actual));		
	}

	@Test
	public void whenConfigChildMethodThenConfigChildProduced() throws Exception {
		ConfigChild rawActual = selectChild("roleNameChild");
		assertTrue(rawActual instanceof ObjectConfigChild);
		ObjectConfigChild actual = (ObjectConfigChild) rawActual;
		assertEquals("roleNameChild", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
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
		ConfigChild rawActual = selectChild("roleNameDeprecatedChild");
		assertTrue(rawActual instanceof ObjectConfigChild);
		ObjectConfigChild actual = (ObjectConfigChild) rawActual;
		assertEquals("roleNameDeprecatedChild", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertFalse(actual.isAllowMultiple());
		assertTrue(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildInheritedFromProtectedThenChildAndNotOverriddenButAnnotationsInherited() {
		ConfigChild rawActual = selectChild("roleNameInheritedChilds");
		assertTrue(rawActual instanceof ObjectConfigChild);
		ObjectConfigChild actual = (ObjectConfigChild) rawActual;
		assertEquals("roleNameInheritedChilds", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChild", actual.getElementType().getSimpleName());
		assertFalse(actual.isDocumented());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
		assertFalse(actual.isTechnicalOverride());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void whenIbisDocBothOnParentAndDerivedThenDerivedValueTaken() {
		ConfigChild rawActual = selectChild("roleNameInheritedChildDocWithDescriptionOverride");
		assertTrue(rawActual instanceof ObjectConfigChild);
		ObjectConfigChild actual = (ObjectConfigChild) rawActual;
		assertEquals("roleNameInheritedChildDocWithDescriptionOverride", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChildDocWithDescriptionOverride", actual.getElementType().getSimpleName());
		assertEquals("Description of Container.setInheritedChildDocWithDescriptionOverride", actual.getDescription());
		assertTrue(actual.isDocumented());
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

	/*
	 * Method Container.setNotConfigChildButAttribute() should not be included as config child, because
	 * the argument type is String and the name starts with "set". Such a method is an attribute
	 * setter.
	 */
	@Test
	public void testSequenceOfConfigChildrenMatchesSequenceOfConfigChildSetters() {
		List<String> actualConfigChildNames = configChildren.stream().map(ConfigChild::toString).collect(Collectors.toList());
		String[] expectedConfigChildNames = new String[] {"ObjectConfigChild(Container.setChild(Child))", "ObjectConfigChild(Container.setDeprecatedChild(Child))",
				"ObjectConfigChild(Container.registerInheritedChilds(InheritedChild))", "ObjectConfigChild(Container.setInheritedChildDocOnDerived(InheritedChildDocOnDerived))",
				"ObjectConfigChild(Container.setInheritedChildDocWithDescriptionOverride(InheritedChildDocWithDescriptionOverride))", "ObjectConfigChild(Container.setInheritedChildNonSelected(InheritedChildNonSelected))",
				"ObjectConfigChild(Container.setChildOverriddenOnlyParentAnnotated(ChildOverriddenOnlyParentAnnotated))",
				"TextConfigChild(roleName = roleNameText)"};
		assertArrayEquals(expectedConfigChildNames, actualConfigChildNames.toArray(new String[] {}));
	}

	@Test
	public void whenConfigChildOverriddenTwiceTheGrandparentTaken() {
		ConfigChild grandChild = checkAndFindGrandChild("roleNameInheritedChildDocWithDescriptionOverride");
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
		assertFalse(actual.isDocumented());
		assertFalse(actual.isDeprecated());
		assertNotNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildOverridesWithoutDocsThenNotDocumentedButAnnotationsInherited() {
		ConfigChild rawActual = selectChild("roleNameChildOverriddenOnlyParentAnnotated");
		assertTrue(rawActual instanceof ObjectConfigChild);
		ObjectConfigChild actual = (ObjectConfigChild) rawActual;
		assertEquals("roleNameChildOverriddenOnlyParentAnnotated", actual.getRoleName());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("ChildOverriddenOnlyParentAnnotated", actual.getElementType().getSimpleName());
		assertEquals("Description of ContainerParent.setChildOverriddenOnlyParentAnnotated", actual.getDescription());
		assertFalse(actual.isDocumented());
		assertFalse(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
		// Not selected because deprecated
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenInheritedConfigChildNotDeprecatedInheritedFromDeprecatedThenNotDeprecated() throws Exception {
		ConfigChild theConfigChild = instance.findOrCreateFrankElement(CONTAINER_OTHER).getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertFalse(theConfigChild.isDeprecated());
	}

	@Test
	public void whenConfigChildHasJavadocThenDocumentedAndTakenAsDescription() throws Exception {
		ConfigChild configChild = instance.findOrCreateFrankElement(PACKAGE + "ContainerForConfigChildDescriptionJavadoc").getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertTrue(configChild.isDocumented());
		assertEquals("JavaDoc of ContainerForConfigChildDescriptionJavadoc.setChild", configChild.getDescription());
	}

	@Test
	public void whenConfigChildInheritsJavadocThenNotDocumentedButDescriptionTaken() throws Exception {
		ConfigChild configChild = instance.findOrCreateFrankElement(PACKAGE + "ContainerForConfigChildDescriptionJavadocDerived").getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertFalse(configChild.isDocumented());
		assertEquals("JavaDoc of ContainerForConfigChildDescriptionJavadoc.setChild", configChild.getDescription());		
	}

	@Test
	public void whenConfigChildHasIbisDocDescriptionThenJavadocOverruled() throws Exception {
		ConfigChild configChild = instance.findOrCreateFrankElement(PACKAGE + "ContainerForConfigChildDescriptionJavadocOverruled").getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertEquals("Description of ContainerForConfigChildDescriptionJavadocOverruled.setChild", configChild.getDescription());
	}

	@Test
	public void whenConfigChildHasIbisDocWithoutDescriptionThenJavadocTaken() throws Exception {
		ConfigChild configChild = instance.findOrCreateFrankElement(PACKAGE + "ContainerForConfigChildDescriptionIbisDocOmitsDescriptionJavadocTaken").getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).get(0);
		assertEquals("Description of ContainerForConfigChildDescriptionIbisDocOmitsDescriptionJavadocTaken.setChild", configChild.getDescription());
	}
}
