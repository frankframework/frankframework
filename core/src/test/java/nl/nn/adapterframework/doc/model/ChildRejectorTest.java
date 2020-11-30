package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class ChildRejectorTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.walking";

	private FrankDocModel model;
	private ChildRejector<String, FrankAttribute> instance;

	private void init(
			String modelPopulateClassSimpleName,
			Predicate<ElementChild<?>> selector,
			Predicate<ElementChild<?>> rejector,
			String subject)
			throws Exception {
		String rootClassName = PACKAGE + "." + modelPopulateClassSimpleName;
		model = FrankDocModel.populate("doc/empty-digester-rules.xml", rootClassName);
		instance = new ChildRejector<String, FrankAttribute>(
				FrankElement::getAttributes, selector, rejector, c -> c.getName());
		instance.init(getElement(subject));
	}

	private FrankElement getElement(String simpleName) throws Exception {
		String rootClassName = PACKAGE + "." + simpleName;
		return model.findOrCreateFrankElement(Utils.getClass(rootClassName));
	}

	private Set<String> childNames(String frankElementSimpleName) throws Exception {
		return instance.getChildrenFor(getElement(frankElementSimpleName)).stream()
				.map(a -> a.getName()).collect(Collectors.toSet());
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
		init("Child3", ElementChild.SELECTED, ElementChild.DEPRECATED, "Child3");
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
		init("GrandChild4", ElementChild.SELECTED, ElementChild.DEPRECATED, "GrandChild4");
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
