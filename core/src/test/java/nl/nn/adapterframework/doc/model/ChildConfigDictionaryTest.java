package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ChildConfigDictionaryTest {
	private ConfigChildDictionary instance;

	@Before
	public void setUp() {
		instance = new ConfigChildDictionary("doc/fake-digester-rules.xml");
	}

	@Test
	public void whenSingularRuleThenSingularInDictionary() {
		ConfigChildDictionary.Item dictItem = instance.getDictionaryItem("setItemSingular");
		assertNotNull(dictItem);
		assertEquals("setItemSingular", dictItem.getMethodName());
		assertEquals("syntax1NameItemSingular", dictItem.getSyntax1Name());
		assertFalse(dictItem.isMandatory());
		assertFalse(dictItem.isAllowMultiple());
	}

	@Test
	public void whenPluralAddRuleThenPluralInDictionary() {
		ConfigChildDictionary.Item dictItem = instance.getDictionaryItem("addItemPlural");
		assertNotNull(dictItem);
		assertEquals("addItemPlural", dictItem.getMethodName());
		assertEquals("syntax1NameItemPluralAdd", dictItem.getSyntax1Name());
		assertFalse(dictItem.isMandatory());
		assertTrue(dictItem.isAllowMultiple());
	}

	@Test
	public void whenPluralRegisterThenPluralInDictionary() {
		ConfigChildDictionary.Item dictItem = instance.getDictionaryItem("registerItemPlural");
		assertNotNull(dictItem);
		assertEquals("registerItemPlural", dictItem.getMethodName());
		assertEquals("syntax1NameItemPluralRegister", dictItem.getSyntax1Name());
		assertFalse(dictItem.isMandatory());
		assertTrue(dictItem.isAllowMultiple());
	}

	@Test
	public void onlyRulesWithRegisterMethodsGoInDictionary() {
		assertEquals(3, instance.size());
	}

	@Test
	public void whenNoRuleThenNotInDictionary() {
		assertNull(instance.getDictionaryItem("xyz"));
	}
}
