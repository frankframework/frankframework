package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.ElementRole.Key;

public class ElementRoleTest {
	private static final String ELEMENT = "Element";

	private ElementRole.Factory factory;

	@Before
	public void setUp() {
		factory = new ElementRole.Factory();
	}

	@Test
	public void whenTwoElementRolesWithSameSyntax1NameCreatedThenDifferentSeqs() {
		ElementRole first = factory.create(null, "x", false);
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementRole second = factory.create(null, "x", false);
		assertEquals("XElement_2", second.createXsdElementName(ELEMENT));
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
	}

	@Test
	public void whenTwoElementRolesWithDifferentSyntax1NameCreatedThenNoSeqsInNames() {
		ElementRole first = factory.create(null, "x", false);
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementRole second = factory.create(null, "y", false);
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
