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

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;

public class FrankDocModelPopulateTest {
	private FrankDocModel instance;

	/**
	 * Test that the model gets class-level Javadoc comments. This is achieved using project Therapi. That
	 * project uses an annotation processor. The test will only succeed after a successful Maven build, not
	 * when this test is executed for the first time in Eclipse.
	 */
	@Test
	public void testJavadocsCaptured() {
		assumeTrue(TestAssertions.isTestRunningOnCI());
		instance = FrankDocModel.populate();
		FrankElement configuration = instance.findFrankElement("nl.nn.adapterframework.configuration.Configuration");
		assertTrue(configuration.getClassLevelDoc().startsWith("The Configuration is placeholder of all configuration objects."));
	}
}
