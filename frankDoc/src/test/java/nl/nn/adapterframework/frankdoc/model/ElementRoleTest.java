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
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.model.ElementRole.Key;

public class ElementRoleTest {
	private static final String ELEMENT = "Element";

	private ElementRole.Factory factory;

	@Before
	public void setUp() {
		factory = new ElementRole.Factory();
	}

	@Test
	public void whenTwoElementRolesWithSameRoleNameCreatedThenDifferentSeqs() {
		ElementRole first = factory.create(null, "x");
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementRole second = factory.create(null, "x");
		assertEquals("XElement_2", second.createXsdElementName(ELEMENT));
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
	}

	@Test
	public void whenTwoElementRolesWithDifferentRoleNameCreatedThenNoSeqsInNames() {
		ElementRole first = factory.create(null, "x");
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
		ElementRole second = factory.create(null, "y");
		assertEquals("YElement", second.createXsdElementName(ELEMENT));
		assertEquals("XElement", first.createXsdElementName(ELEMENT));
	}

	@Test
	public void testKeys() {
		Key first = new Key("type", "role");
		Key eqFirst = new Key("type", "role");
		Key second = new Key("otherType", "role");
		assertEquals(first, eqFirst);
		assertFalse(first.equals(second));
	}
}
