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

import static java.util.Arrays.asList;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.ALL;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.DEPRECATED;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.IN_XSD;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.NONE;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.sun.javadoc.ClassDoc;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

@RunWith(Parameterized.class)
public class NavigationCumulativeTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.walking";

	@Parameters(name = "{0} with {1} and {2}")
	public static Collection<Object[]> data() {
		return asList(new Object[][] {
			{"Parent", "Parent", IN_XSD, NONE, asList("parentAttributeFirst", "parentAttributeSecond")},
			{"Child inXsd", "Child", IN_XSD, NONE, asList("childAttribute", "parentAttributeFirst", "parentAttributeSecond")},
			{"Child all", "Child", ALL, NONE, asList("childAttribute", "parentAttributeFirst", "parentAttributeSecond")},
			{"GrandChild", "GrandChild", ALL, NONE, asList("grandChildAttribute", "parentAttributeSecond", "childAttribute", "parentAttributeFirst")},
			{"GrandChild2 no reject", "GrandChild2", ALL, NONE, asList("grandChild2Attribute", "child2Attribute", "parentAttributeFirst", "parentAttributeSecond")},
			{"GrandChild2 reject deprecated", "GrandChild2", ALL, DEPRECATED, asList("grandChild2Attribute", "parentAttributeFirst", "parentAttributeSecond")} 
		});
	}

	@Parameter(0)
	public String title;

	@Parameter(1)
	public String simpleClassName;

	@Parameter(2)
	public Predicate<ElementChild> childSelector;

	@Parameter(3)
	public Predicate<ElementChild> childRejector;

	@Parameter(4)
	public List<String> childNames;

	@Test
	public void test() throws Exception {
		ClassDoc[] classes = TestUtil.getClassDocs(PACKAGE);
		String rootClassName = PACKAGE + "." + simpleClassName;
		FrankClassRepository repository = FrankClassRepository.getDocletInstance(classes, new HashSet<>(Arrays.asList(PACKAGE)), new HashSet<>(), new HashSet<>());
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName, repository);
		FrankElement subject = model.findFrankElement(rootClassName);
		List<String> actual = subject.getCumulativeAttributes(childSelector, childRejector).stream()
				.map(a -> a.getName()).collect(Collectors.toList());
		assertEquals(childNames, actual);
	}
}
