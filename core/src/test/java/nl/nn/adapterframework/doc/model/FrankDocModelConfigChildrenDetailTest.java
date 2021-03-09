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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class FrankDocModelConfigChildrenDetailTest {
	private FrankDocModel instance;

	@Before
	public void setUp() throws SAXException, IOException {
		instance = new FrankDocModel();
		instance.createConfigChildDescriptorsFrom("doc/fake-digester-rules.xml");
	}

	@Test
	public void whenSingularRuleThenSingularInDictionary() {
		ConfigChildSetterDescriptor configChildDescriptor = instance.getConfigChildDescriptors().get("setItemSingular");
		assertNotNull(configChildDescriptor);
		assertEquals("setItemSingular", configChildDescriptor.getMethodName());
		assertEquals("roleNameItemSingular", configChildDescriptor.getRoleName());
		assertFalse(configChildDescriptor.isMandatory());
		assertFalse(configChildDescriptor.isAllowMultiple());
	}

	@Test
	public void whenPluralAddRuleThenPluralInDictionary() {
		ConfigChildSetterDescriptor configChildDescriptor = instance.getConfigChildDescriptors().get("addItemPlural");
		assertNotNull(configChildDescriptor);
		assertEquals("addItemPlural", configChildDescriptor.getMethodName());
		assertEquals("roleNameItemPluralAdd", configChildDescriptor.getRoleName());
		assertFalse(configChildDescriptor.isMandatory());
		assertTrue(configChildDescriptor.isAllowMultiple());
	}

	@Test
	public void whenPluralRegisterThenPluralInDictionary() {
		ConfigChildSetterDescriptor configChildDescriptor = instance.getConfigChildDescriptors().get("registerItemPlural");
		assertNotNull(configChildDescriptor);
		assertEquals("registerItemPlural", configChildDescriptor.getMethodName());
		assertEquals("roleNameItemPluralRegister", configChildDescriptor.getRoleName());
		assertFalse(configChildDescriptor.isMandatory());
		assertTrue(configChildDescriptor.isAllowMultiple());
	}

	@Test
	public void onlyRulesWithRegisterMethodsGoInDictionary() {
		assertEquals(3, instance.getConfigChildDescriptors().size());
	}

	@Test
	public void whenNoRuleThenNotInDictionary() {
		assertNull(instance.getConfigChildDescriptors().get("xyz"));
	}
}
