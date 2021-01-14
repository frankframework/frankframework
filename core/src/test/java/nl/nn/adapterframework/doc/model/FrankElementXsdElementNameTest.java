package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankElementXsdElementNameTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.simple.";
	private static final String CONTAINER = PACKAGE + "Container";
	private static final String DIGESTER_RULES = "doc/xsd-element-name-digester-rules.xml";

	private FrankDocModel model;

	@Before
	public void setUp() {
		model = FrankDocModel.populate(DIGESTER_RULES, CONTAINER);
	}

	@Test
	public void whenNameDoesNotEndWithInterfaceNameThenGroupSyntax1NameAppended() throws Exception {
		String className = PACKAGE + "ListenerParent";
		String typeName = PACKAGE + "IListener";
		FrankElement instance = model.findOrCreateFrankElement(Utils.getClass(className));
		ElementType elementType = model.findOrCreateElementType(Utils.getClass(typeName));
		String actual = instance.getXsdElementName(elementType, "testListener");
		assertEquals("ListenerParentTestListener", actual);
	}

	@Test
	public void whenNameEndsWithInterfaceNameThenRemovedAndGroupSyntax1NameAppended() throws Exception {
		String className = PACKAGE + "ParentListener";
		String typeName = PACKAGE + "IListener";
		FrankElement instance = model.findOrCreateFrankElement(Utils.getClass(className));
		ElementType elementType = model.findOrCreateElementType(Utils.getClass(typeName));
		String actual = instance.getXsdElementName(elementType, "testListener");
		assertEquals("ParentTestListener", actual);
	}

	@Test
	public void whenElementTypeIsNotInterfaceThenSyntax1NameBecomesElementName() throws Exception {
		String classAndTypeName = PACKAGE + "ListenerParent";
		FrankElement instance = model.findOrCreateFrankElement(Utils.getClass(classAndTypeName));
		ElementType elementType = model.findOrCreateElementType(Utils.getClass(classAndTypeName));
		String actual = instance.getXsdElementName(elementType, "someName");
		assertEquals("SomeName", actual);
	}

	@Test
	public void whenConfigChildInXsdThenItsSyntax1NameRegisteredWithElementType() throws Exception {
		String typeName = PACKAGE + "IListener";
		ElementType elementType = model.findOrCreateElementType(Utils.getClass(typeName));
		// The syntax 1 name corresponding to setter Container.setTestListener.
		assertTrue(getSyntax1NamesOf(elementType).contains("testListener"));
	}

	private Set<String> getSyntax1NamesOf(ElementType elementType) {
		return elementType.getElementRoles().stream()
				.map(ElementRole::getSyntax1Name).collect(Collectors.toSet());
	}
}
