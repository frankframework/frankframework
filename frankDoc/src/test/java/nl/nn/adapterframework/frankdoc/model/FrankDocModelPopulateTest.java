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

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;

@Ignore("Test takes a long time to run, and gives little information")
public class FrankDocModelPopulateTest {
	private FrankDocModel instance;
	private Set<String> actualTypeSimpleNames;
	private Set<String> actualElementSimpleNames;

	@Before
	public void setUp() {
		FrankClassRepository classRepository = FrankClassRepository.getReflectInstance();
		classRepository.setExcludeFilters(ExcludeFilter.getExcludeFilter());
		classRepository.setIncludeFilters(ExcludeFilter.getIncludeFilter());
		instance = FrankDocModel.populate(classRepository);
		actualTypeSimpleNames = instance.getAllTypes().values().stream()
				.map(ElementType::getSimpleName).collect(Collectors.toSet());
		actualElementSimpleNames = instance.getAllElements().values().stream()
				.map(FrankElement::getSimpleName).collect(Collectors.toSet());		
	}

	@Test
	public void testTypeIAdapterCreated() {
		assertTrue(actualTypeSimpleNames.contains("IAdapter"));
	}

	@Test
	public void testElementReceiverCreated() {
		assertTrue(actualElementSimpleNames.contains("Receiver"));
	}
}
