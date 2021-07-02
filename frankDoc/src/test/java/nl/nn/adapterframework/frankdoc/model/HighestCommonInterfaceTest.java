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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class HighestCommonInterfaceTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.role.inherit.";
	private FrankDocModel model;
	private ElementType founder;
	private ElementType interfaceParent;
	private ElementType interfaceElementType;
	private ElementType interface2ElementType;

	@Before
	public void setUp() {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		model = FrankDocModel.populate("doc/role-inherit-digester-rules.xml", PACKAGE + "Master", repository);
		founder = model.findElementType(PACKAGE + "IFounder");
		interfaceParent = model.findElementType(PACKAGE + "IInterfaceParent");
		interfaceElementType = model.findElementType(PACKAGE + "IInterface");
		interface2ElementType = model.findElementType(PACKAGE + "IInterface2");
	}

	@Test
	public void testTestSetup() {
		assertNotNull(founder);
		assertNotNull(interfaceParent);
		assertNotNull(interfaceElementType);
		assertNull(model.findElementType(PACKAGE + "IIrrelevantAncestor"));
		assertNull(model.findElementType(PACKAGE + "IIrrelevant"));
		assertNotNull(interface2ElementType);
	}

	@Test
	public void testFounder() {
		assertSame(founder, founder.getHighestCommonInterface());
		assertSame(founder, interfaceParent.getHighestCommonInterface());
		assertSame(founder, interfaceElementType.getHighestCommonInterface());
		assertSame(founder, interface2ElementType.getHighestCommonInterface());
	}
}
