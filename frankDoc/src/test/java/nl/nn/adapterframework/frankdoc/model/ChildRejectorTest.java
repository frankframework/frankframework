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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class ChildRejectorTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.walking";

	private FrankDocModel model;
	private ChildRejector<FrankAttribute> instance;

	private void init(String modelPopulateClassSimpleName, Predicate<ElementChild> selector, Predicate<ElementChild> rejector, String subject) throws Exception {
		String rootClassName = PACKAGE + "." + modelPopulateClassSimpleName;
		FrankClassRepository repository = TestUtil.getClassRepository(PACKAGE);
		model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName, repository);
		instance = new ChildRejector<FrankAttribute>(
				selector, rejector, FrankAttribute.class);
		instance.init(getElement(subject));
	}

	private FrankElement getElement(String simpleName) throws Exception {
		String rootClassName = PACKAGE + "." + simpleName;
		return model.findOrCreateFrankElement(rootClassName);
	}

	private Set<String> childNames(String frankElementSimpleName) throws Exception {
		return instance.getChildrenFor(getElement(frankElementSimpleName)).stream()
				.map(a -> ((FrankAttribute) a).getName()).collect(Collectors.toSet());
	}

	private Set<String> setOf(String ...names) {
		return new HashSet<>(Arrays.asList(names));
	}

	@Test
	public void whenNoChildrenRejectedThenAllChildrenReturned() throws Exception {
		init("Child", ElementChild.ALL, ElementChild.NONE, "Parent");
		assertEquals(setOf("parentAttributeFirst", "parentAttributeSecond"), childNames("Parent"));
		assertTrue(instance.isNoDeclaredRejected(getElement("Parent")));
		assertTrue(instance.isNoCumulativeRejected(getElement("Parent")));
	}

	@Test
	public void whenChildRejectedThenChildNotReturned() throws Exception {
		init("Child2", ElementChild.ALL, ElementChild.DEPRECATED, "Child2");
		assertEquals(setOf(), childNames("Child2"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Child2")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Child2")));
	}

	@Test
	public void whenChildRejectsParentAttributeThenAttributeRejectedForChild() throws Exception {
		init("Child3", ElementChild.ALL, ElementChild.DEPRECATED, "Child3");
		assertEquals(setOf("parentAttributeSecond"), childNames("Parent"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Parent")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Parent")));
		assertEquals(setOf(), childNames("Child3"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Child3")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Child3")));
	}

	@Test(expected = NullPointerException.class)
	public void whenRejectorInitializedForParentThenChildDoesNotAppearAsInheritanceLevel() throws Exception {
		init("Child3", ElementChild.ALL, ElementChild.DEPRECATED, "Parent");
		instance.getChildrenFor(getElement("Child3"));
	}

	@Test
	public void cumulativeAttributesOfParentIncludeAttributesRejectedByIrrelevantChild() throws Exception {
		init("Child3", ElementChild.ALL, ElementChild.DEPRECATED, "Parent");
		assertEquals(setOf("parentAttributeFirst", "parentAttributeSecond"), childNames("Parent"));
		assertTrue(instance.isNoDeclaredRejected(getElement("Parent")));
		assertTrue(instance.isNoCumulativeRejected(getElement("Parent")));
	}

	@Test
	public void whenNonAttributeRejectedByChildThenThatAttributeRejectedByParent() throws Exception {
		init("Child3", ElementChild.IN_XSD, ElementChild.DEPRECATED, "Child3");
		assertEquals(setOf(), childNames("Child3"));
		assertTrue(instance.isNoDeclaredRejected(getElement("Child3")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Child3")));
		assertEquals(setOf("parentAttributeSecond"), childNames("Parent"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Parent")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Parent")));
	}

	@Test
	public void whenBothParentAndGrandParentRejectThenCumulativeGroupRejected() throws Exception {
		init("GrandChild3", ElementChild.ALL, ElementChild.DEPRECATED, "GrandChild3");
		assertEquals(setOf(), childNames("GrandChild3"));
		assertFalse(instance.isNoDeclaredRejected(getElement("GrandChild3")));
		assertFalse(instance.isNoCumulativeRejected(getElement("GrandChild3")));
		assertEquals(setOf(), childNames("Child3"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Child3")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Child3")));
		assertEquals(setOf(), childNames("Parent"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Parent")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Parent")));
	}

	@Test
	public void whenChildrenRejectNonAttributesForParentAndGrandParentThenThoseAttributesRejected() throws Exception {
		init("GrandChild4", ElementChild.IN_XSD, ElementChild.DEPRECATED, "GrandChild4");
		assertEquals(setOf("grandChild4Attribute"), childNames("GrandChild4"));
		assertTrue(instance.isNoDeclaredRejected(getElement("GrandChild4")));
		assertFalse(instance.isNoCumulativeRejected(getElement("GrandChild4")));
		assertEquals(setOf("child4Attribute"), childNames("Child4"));
		assertTrue(instance.isNoDeclaredRejected(getElement("Child4")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Child4")));
		assertEquals(setOf(), childNames("Parent"));
		assertFalse(instance.isNoDeclaredRejected(getElement("Parent")));
		assertFalse(instance.isNoCumulativeRejected(getElement("Parent")));
	}
}
