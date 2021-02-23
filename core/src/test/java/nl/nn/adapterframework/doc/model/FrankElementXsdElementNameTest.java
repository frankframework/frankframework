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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
	public void frankElementKnowsXmlElementNames() {
		FrankElement frankElement = model.findFrankElement(PACKAGE + "ListenerParent");
		assertArrayEquals(new String[] {"ListenerParentTestListener"}, frankElement.getXmlElementNames().toArray());
		frankElement = model.findFrankElement(PACKAGE + "ParentListener");
		assertArrayEquals(new String[] {"ParentTestListener"}, frankElement.getXmlElementNames().toArray());
		frankElement = model.findFrankElement(PACKAGE + "Container");
		assertArrayEquals(new String[] {"Container"}, frankElement.getXmlElementNames().toArray());
	}
}
