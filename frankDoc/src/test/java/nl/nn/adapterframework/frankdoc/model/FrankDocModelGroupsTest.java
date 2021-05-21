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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class FrankDocModelGroupsTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.groups.";

	private FrankDocModel instance;
	
	@Before
	public void setUp() throws SAXException, IOException, ReflectiveOperationException {
		FrankClassRepository r = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		instance = FrankDocModel.populate("doc/fake-group-digester-rules.xml", PACKAGE + "Container", r);
	}

	@Test
	public void testGroups() {
		List<FrankDocGroup> groups = instance.getGroups();
		assertEquals(2, groups.size());
		FrankDocGroup current = groups.get(0);
		assertEquals("Listener", current.getName());
		List<FrankElement> members = current.getElements();
		assertEquals(1, members.size());
		assertEquals("DefaultChild", members.get(0).getSimpleName());
		current = groups.get(1);
		assertEquals("Other", current.getName());
		members = current.getElements();
		assertEquals(2, members.size());
		assertEquals("Container", members.get(0).getSimpleName());
		assertEquals("Default", members.get(1).getSimpleName());
		assertTrue(members.get(1).getXmlElementNames().contains("DefaultSender"));
		// We test here that class DefaultSender is excluded from the group, because that would
		// produce a conflicting definition for XML tag "DefaultSender".
	}
}
