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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class ElementRoleIntegrationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.role.";

	private FrankDocModel model;

	@Before
	public void setUp() {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		model = FrankDocModel.populate("doc/role-digester-rules.xml", PACKAGE + "Master", repository);
	}

	@Test
	public void testExampleClassesTesttargetRole() {
		// We have two interfaces and two syntax 1 names (roles). We try all combinations
		assertEquals(4, model.getAllElementRoles().size());
		// The element roles from Container are created first. They interchange interface
		// numbers and role numbers.
		ElementRole er = model.findElementRole(PACKAGE + "Interface1", "role2");
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role2", er.getRoleName());
		assertEquals("Role2Element", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role1");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role1", er.getRoleName());
		assertEquals("Role1Element", er.createXsdElementName("Element"));
		
		// The element roles from Master are created later. These match
		// the interface numbers and role numbers. The syntax 1 names are reused,
		// so we expect sequence numbers to be added in the element names.
		er = model.findElementRole(PACKAGE + "Interface1", "role1");
		assertNotNull(er);
		assertEquals(PACKAGE + "Interface1", er.getElementType().getFullName());
		assertEquals("role1", er.getRoleName());
		assertEquals("Role1Element_2", er.createXsdElementName("Element"));
		er = model.findElementRole(PACKAGE + "Interface2", "role2");
		assertEquals(PACKAGE + "Interface2", er.getElementType().getFullName());
		assertEquals("role2", er.getRoleName());
		assertEquals("Role2Element_2", er.createXsdElementName("Element"));
	}
}
