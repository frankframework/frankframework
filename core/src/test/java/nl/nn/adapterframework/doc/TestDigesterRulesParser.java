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
package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;

public class TestDigesterRulesParser {

	@Test
	public void testParser() throws Exception {
		List<ChildIbisBeanMapping> beanList = InfoBuilderSource.getChildIbisBeanMappings();
		assertTrue(beanList.size() >= 30); //Expect to find at least 33 pattern mappings

		ChildIbisBeanMapping mapping = beanList.get(0);
		assertNotNull(mapping.getMethodName()); //Should never be null
		assertNotNull(mapping.getChildIbisBeanName()); //Should never be null
	}
}
