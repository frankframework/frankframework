package nl.nn.adapterframework.doc.model;

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

	private FrankDocModel instance;
	private List<ConfigChild> configChildren;
	private List<ConfigChild> configChildrenOfDerived;

	@Before
	public void setUp() throws SAXException, IOException, ReflectiveOperationException {
		instance = new FrankDocModel();
		instance.createConfigChildDescriptorsFrom("doc/simple-digester-rules.xml");
		instance.findOrCreateElementType(
				Utils.getClass(CONTAINER));
		instance.findOrCreateElementType(
				Utils.getClass(CONTAINER_DERIVED));
		instance.setOverriddenFrom();
		configChildren = instance.getAllElements().get(CONTAINER).getConfigChildren();
		configChildrenOfDerived = instance.getAllElements().get(CONTAINER_DERIVED).getConfigChildren();
	}

	@Test
	public void whenConfigChildMethodThenConfigChildProduced() throws Exception {
		ConfigChild actual = selectChild("syntax1NameChild");
		assertEquals("syntax1NameChild", actual.getSyntax1Name());
		assertEquals("syntax1NameChilds", actual.getSyntax1NamePlural());
		// Class Child is the only element of ElementType Child, and this type
		// is used in a ConfigChild. Therefore FrankElement Child gets the syntax 1
		// name (upper-case'd) as alias. In production, this feature is for example used
		// to alias "PipeForward" with "Forward".
		assertEquals("Syntax1NameChild", instance.findOrCreateFrankElement(Utils.getClass(
				"nl.nn.adapterframework.doc.testtarget.children.Child")).getAlias());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(100, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertTrue(IN_XSD.test(actual));
	}

	private ConfigChild selectChild(String name) {
		List<ConfigChild> selected = configChildren.stream()
				.filter(c -> c.getSyntax1Name().equals(name))
				.collect(Collectors.toList());
		assertEquals(1, selected.size());
		return selected.get(0);
	}

	@Test
	public void whenConfigChildMethodDeprecatedThenConfigChildDeprecated() {
		ConfigChild actual = selectChild("syntax1NameDeprecatedChild");
		assertEquals("syntax1NameDeprecatedChild", actual.getSyntax1Name());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(200, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertTrue(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildInheritedFromProtectedThenChildAndNotOverriddenButAnnotationsInherited() {
		ConfigChild actual = selectChild("syntax1NameInheritedChilds");
		assertEquals("syntax1NameInheritedChilds", actual.getSyntax1Name());
		assertEquals("syntax1NameInheritedChilds", actual.getSyntax1NamePlural());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChild", actual.getElementType().getSimpleName());
		assertFalse(actual.isDocumented());
		assertEquals(50, actual.getSequenceInConfig());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void whenIbisDocOnDerivedMethodThenStillOrderFromIbisDoc() {
		ConfigChild actual = selectChild("syntax1NameInheritedChildDocOnDerived");
		assertEquals("syntax1NameInheritedChildDocOnDerived", actual.getSyntax1Name());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChildDocOnDerived", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(70, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isMandatory());
		assertFalse(actual.isDeprecated());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
	}

	@Test
	public void whenIbisDocBothOnParentAndDerivedThenDerivedValueTaken() {
		ConfigChild actual = selectChild("syntax1NameInheritedChildDocWithOrderOverride");
		assertEquals("syntax1NameInheritedChildDocWithOrderOverride", actual.getSyntax1Name());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("InheritedChildDocWithOrderOverride", actual.getElementType().getSimpleName());
		assertTrue(actual.isDocumented());
		assertEquals(10, actual.getSequenceInConfig());
		assertFalse(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void whenConfigChildOnAncestorNotOverriddenThenOmitted() {
		checkChildNotPresent("syntax1NameInheritedWithoutOverride");
	}

	private void checkChildNotPresent(String name) {
		boolean result = configChildren.stream()
				.filter(c -> c.getSyntax1Name().equals(name))
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
		ConfigChild grandChild = checkAndFindGrandChild("syntax1NameInheritedChildDocWithOrderOverride");
		assertEquals("Container", grandChild.getOverriddenFrom().getSimpleName());
	}

	private ConfigChild checkAndFindGrandChild(final String syntax1Name) {
		List<ConfigChild> grandChildList = configChildrenOfDerived.stream()
				.filter(c -> c.getSyntax1Name().equals(syntax1Name))
				.collect(Collectors.toList());
		assertEquals(1, grandChildList.size());
		return grandChildList.get(0);
	}

	@Test
	public void whenConfigChildOverriddenFromGrandparentThenGrandparentTaken() {
		ConfigChild grandChild = checkAndFindGrandChild("syntax1NameInheritedWithoutOverride");
		assertEquals("ContainerParent", grandChild.getOverriddenFrom().getSimpleName());
	}

	@Test
	public void whenConfigChildOverriddenNotDocumentedThenChildCreatedButNotSelected() {
		ConfigChild actual = selectChild("syntax1NameInheritedChildNonSelected");
		assertEquals(120, actual.getSequenceInConfig());
		assertFalse(actual.isDocumented());
		assertFalse(actual.isDeprecated());
		assertNotNull(actual.getOverriddenFrom());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void whenChildOverridesWithoutDocsThenNotDocumentedButAnnotationsInherited() {
		ConfigChild actual = selectChild("syntax1NameChildOverriddenOnlyParentAnnotated");
		assertEquals("syntax1NameChildOverriddenOnlyParentAnnotated", actual.getSyntax1Name());
		assertEquals("Container", actual.getOwningElement().getSimpleName());
		assertEquals("ChildOverriddenOnlyParentAnnotated", actual.getElementType().getSimpleName());
		assertFalse(actual.isDocumented());
		assertEquals(110, actual.getSequenceInConfig());
		assertTrue(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
		// Not selected because deprecated
		assertFalse(IN_XSD.test(actual));
	}
}
