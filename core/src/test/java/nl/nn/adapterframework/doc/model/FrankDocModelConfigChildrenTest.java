package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelConfigChildrenTest {
	private static String CONTAINER = "nl.nn.adapterframework.doc.testtarget.children.Container";

	private FrankDocModel instance;
	private List<ConfigChild> configChildren;

	@Before
	public void setUp() {
		ConfigChildDictionary dictionary = new ConfigChildDictionary("doc/simple-digester-rules.xml");
		dictionary.setMethodNameOrder("setChildNoDoc", 20);
		instance = new FrankDocModel();
		instance.findOrCreateElementType(
				Utils.getClass(CONTAINER), dictionary);
		configChildren = instance.getAllElements().get(CONTAINER).getConfigChildren();
	}

	@Test
	public void whenConfigChildMethodThenConfigChildProduced() {
		ConfigChild actual = selectChild("syntax1NameChild");
		assertEquals("syntax1NameChild", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("Child", actual.getElementType().getSimpleName());
		assertEquals(100, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isMandatory());
	}

	private ConfigChild selectChild(String name) {
		List<ConfigChild> selected = configChildren.stream()
				.filter(c -> c.getSyntax1Name().equals(name))
				.collect(Collectors.toList());
		assertEquals(1, selected.size());
		return selected.get(0);
	}

	@Test
	public void whenChildSetterInheritedThenConfigChildProduced() {
		ConfigChild actual = selectChild("syntax1NameInheritedChild");
		assertEquals("syntax1NameInheritedChild", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("InheritedChild", actual.getElementType().getSimpleName());
		assertEquals(50, actual.getSequenceInConfig());
		assertTrue(actual.isAllowMultiple());
		assertFalse(actual.isMandatory());
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
	}

	@Test
	public void whenNoIbisDocThenDefaultOrderTaken() {
		ConfigChild actual = selectChild("syntax1NameChildNoDoc");
		assertEquals("syntax1NameChildNoDoc", actual.getSyntax1Name());
		assertEquals("Container", actual.getConfigParent().getSimpleName());
		assertEquals("ChildNoDoc", actual.getElementType().getSimpleName());
		assertEquals(20, actual.getSequenceInConfig());
		assertFalse(actual.isAllowMultiple());
		assertFalse(actual.isMandatory());		
	}

	@Test
	public void onlyWantedConfigChildrenProduced() {
		assertEquals(4, configChildren.size());
	}
}
