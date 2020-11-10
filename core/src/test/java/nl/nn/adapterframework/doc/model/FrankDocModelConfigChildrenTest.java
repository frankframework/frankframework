package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
		instance.setConfigChildrenOverriddenFrom();
		configChildren = instance.getAllElements().get(CONTAINER).getConfigChildren();
		configChildrenOfDerived = instance.getAllElements().get(CONTAINER_DERIVED).getConfigChildren();
	}

	@Test
	public void whenConfigChildMethodThenConfigChildProduced() {
		ConfigChild actual = selectChild("syntax1NameChild");
		assertEquals("syntax1NameChild", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertEquals(100, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
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
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertEquals(200, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertTrue(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		assertNull(actual.getOverriddenFrom());
	}

	@Test
	public void whenChildSetterInheritedThenConfigChildProduced() {
		ConfigChild actual = selectChild("syntax1NameInheritedChild");
		assertEquals("syntax1NameInheritedChild", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("InheritedChild", actual.getElementType().getSimpleName());
		assertEquals(50, actual.getSequenceInConfig());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isDeprecated());
		assertFalse(actual.isMandatory());
		// The method in the parent is protected, so not overridden
		assertNull(actual.getOverriddenFrom());
	}

	@Test
	public void whenIbisDocOnDerivedMethodThenStillOrderFromIbisDoc() {
		ConfigChild actual = selectChild("syntax1NameInheritedChildDocOnDerived");
		assertEquals("syntax1NameInheritedChildDocOnDerived", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("InheritedChildDocOnDerived", actual.getElementType().getSimpleName());
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
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("InheritedChildDocWithOrderOverride", actual.getElementType().getSimpleName());
		assertEquals(10, actual.getSequenceInConfig());
		assertFalse(actual.isDeprecated());
		assertEquals("ContainerParent", actual.getOverriddenFrom().getSimpleName());
	}

	@Test
	public void onlyWantedConfigChildrenProduced() {
		assertEquals(5, configChildren.size());
	}

	@Test
	public void whenConfigChildOverriddenTwiceTheGrandparentTaken() {
		List<ConfigChild> grandChildList = configChildrenOfDerived.stream()
				.filter(c -> c.getSyntax1Name().equals("syntax1NameInheritedChildDocWithOrderOverride"))
				.collect(Collectors.toList());
		assertEquals(1, grandChildList.size());
		ConfigChild grandChild = grandChildList.get(0);
		assertEquals("ContainerParent", grandChild.getOverriddenFrom().getSimpleName());
	}
}
