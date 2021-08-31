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

import java.util.List;

import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class FrankDocModelGroupsTest {
	private FrankDocModel instance;
	
	@Test
	public void testGroups() {
		String thePackage = "nl.nn.adapterframework.frankdoc.testtarget.groups.";
		FrankClassRepository r = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		instance = FrankDocModel.populate("doc/fake-group-digester-rules.xml", thePackage + "Container", r);
		List<FrankDocGroup> groups = instance.getGroups();
		assertEquals(2, groups.size());
		FrankDocGroup current = groups.get(0);
		assertEquals("Listener", current.getName());
		List<ElementType> types = current.getElementTypes();
		assertEquals(1, types.size());
		assertEquals("IChild", types.get(0).getSimpleName());
		current = groups.get(1);
		assertEquals("Other", current.getName());
		types = current.getElementTypes();
		assertEquals(1, types.size());
		assertEquals("ISender", types.get(0).getSimpleName());
		// We test here that class DefaultSender is excluded from the type, because that would
		// produce a conflicting definition for XML tag "DefaultSender".
		List<FrankElement> frankElements = types.get(0).getSyntax2Members();
		assertEquals(1, frankElements.size());
		assertEquals("Default", frankElements.get(0).getSimpleName());
		assertTrue(frankElements.get(0).getXmlElementNames().contains("DefaultSender"));
		List<FrankElement> leftOvers = instance.getElementsOutsideConfigChildren();
		assertEquals(1, leftOvers.size());
		assertEquals("Container", leftOvers.get(0).getSimpleName());
	}

	@Test
	public void testFrankDocIgnoreTypeMembership() {
		String thePackage = "nl.nn.adapterframework.frankdoc.testtarget.groups.ignore.membership.";
		FrankClassRepository r = TestUtil.getFrankClassRepositoryDoclet(thePackage);
		instance = FrankDocModel.populate("doc/fake-group-digester-rules.xml", thePackage + "Container", r);
		List<FrankDocGroup> groups = instance.getGroups();
		assertEquals(2, groups.size());
		FrankDocGroup group = groups.get(0);
		assertEquals("Listeners", group.getName());
		assertEquals(1, group.getElementTypes().size());
		ElementType elementType = group.getElementTypes().get(0);
		List<FrankElement> members = elementType.getSyntax2Members();
		assertEquals(2, members.size());
		assertEquals("ChildListener", members.get(0).getSimpleName());
		assertEquals("Parent", members.get(1).getSimpleName());
		// Because of the FrankDocIgnoreGroupMembership annotation, ChildSender and ChildSenderChild are not in.
		group = groups.get(1);
		assertEquals("Senders", group.getName());
		assertEquals(1, group.getElementTypes().size());
		elementType = group.getElementTypes().get(0);
		members = elementType.getMembers();
		assertEquals(3, members.size());
		assertEquals("ChildSender", members.get(0).getSimpleName());
		assertEquals("ChildSenderChild", members.get(1).getSimpleName());
		assertEquals("ChildSenderSender", members.get(2).getSimpleName());
	}
}
