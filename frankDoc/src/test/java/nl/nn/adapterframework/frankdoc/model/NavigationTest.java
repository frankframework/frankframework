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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import lombok.EqualsAndHashCode;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;

@RunWith(Parameterized.class)
public class NavigationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.walking";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return asList(new Object[][] {
			{"Parent", IN_XSD, NONE, asList(ref(RefKind.DECLARED, "Parent"))},
			// Attribute childAttribute is not selected, so we do not have a real override.
			{"Child", IN_XSD, NONE, asList(ref(RefKind.DECLARED, "Child"), ref(RefKind.DECLARED, "Parent"))},
			// Attribute parentAttributeFirst is overridden. Keep with Child, omit with Parent
			{"Child", ALL, NONE, asList(ref(RefKind.DECLARED, "Child"), ref(RefKind.CHILD, "parentAttributeSecond"))},
			// All attributes of Parent were overridden. Nothing to reference for Parent.
			{"GrandChild", ALL, NONE, asList(ref(RefKind.DECLARED, "GrandChild"), ref(RefKind.DECLARED, "Child"))},
			// The override of parentAttributeSecond counts, in Child parentAttributeFirst is ignored as child
			{"GrandChild", IN_XSD, NONE, asList(ref(RefKind.DECLARED, "GrandChild"), ref(RefKind.DECLARED, "Child"), ref(RefKind.CHILD, "parentAttributeFirst"))},
			{"GrandChild2", ALL, NONE, asList(ref(RefKind.DECLARED, "GrandChild2"), ref(RefKind.CUMULATIVE, "Child2"))},
			// All children of Child2 are deprecated, so Child2 is ignored in the ancestor hierarchy
			{"GrandChild2", IN_XSD, NONE, asList(ref(RefKind.DECLARED, "GrandChild2"), ref(RefKind.DECLARED, "Parent"))},
			// All attributes of Parent are overridden by deprecated methods and should be de-inherited
			{"GrandChild3", ALL, DEPRECATED, asList()}
		});
	}

	private static enum RefKind {
		CHILD,
		DECLARED,
		CUMULATIVE;
	}

	@EqualsAndHashCode
	private static class Ref {
		private RefKind kind;
		private String name;

		Ref(RefKind kind, String name) {
			this.kind = kind;
			this.name = name;
		}

		@Override
		public String toString() {
			return "(" + kind.toString() + ", " + name + ")"; 
		}
	}

	private static Ref ref(RefKind kind, String name) {
		return new Ref(kind, name);
	}

	@Parameter(0)
	public String simpleClassName;

	@Parameter(1)
	public Predicate<ElementChild> childSelector;

	@Parameter(2)
	public Predicate<ElementChild> childRejector;

	@Parameter(3)
	public List<Ref> expectedRefs;

	private List<Ref> actual;

	@Before
	public void setUp() {
		actual = new ArrayList<>();
	}

	@Test
	public void test() throws Exception {
		String rootClassName = PACKAGE + "." + simpleClassName;
		FrankClassRepository repository = FrankClassRepository.getReflectInstance(PACKAGE);
		FrankDocModel model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName, repository);
		FrankElement walkFrom = model.findFrankElement(rootClassName);
		walkFrom.walkCumulativeAttributes(new CumulativeChildHandler<FrankAttribute>() {
			@Override
			public void handleSelectedChildren(List<FrankAttribute> children, FrankElement owner) {
				children.forEach(c -> actual.add(ref(RefKind.CHILD, c.getName())));
			}

			@Override
			public void handleChildrenOf(FrankElement frankElement) {
				actual.add(ref(RefKind.DECLARED, frankElement.getSimpleName()));
			}

			@Override
			public void handleCumulativeChildrenOf(FrankElement frankElement) {
				actual.add(ref(RefKind.CUMULATIVE, frankElement.getSimpleName()));
			}
			
		}, childSelector, childRejector);
		assertEquals(expectedRefs, actual);
	}
}
