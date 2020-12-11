package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.ElementTypeRole.Key;

public class ElementTypeRoleTest {
	private static final String ELEMENT = "Element";

	@Before
	public void setUp() {
		ElementTypeRole.init();
	}

	@Test
	public void whenTwoElementTypeRolesWithSameSyntax1NameCreatedThenDifferentSeqs() {
		ElementTypeRole first = new ElementTypeRole(null, "x");
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementTypeRole second = new ElementTypeRole(null, "x");
		assertEquals("XElement_2", second.createXsdElementName(ELEMENT));
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
	}

	@Test
	public void whenTwoElementTypeRolesWithDifferentSyntax1NameCreatedThenNoSeqsInNames() {
		ElementTypeRole first = new ElementTypeRole(null, "x");
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementTypeRole second = new ElementTypeRole(null, "y");
		assertEquals("YElement", second.createXsdElementName(ELEMENT));
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
	}

	@Test
	public void testKeys() {
		Key first = new Key("type", "syntax1");
		Key eqFirst = new Key("type", "syntax1");
		Key second = new Key("otherType", "syntax1");
		assertEquals(first, eqFirst);
		assertFalse(first.equals(second));
	}
}
