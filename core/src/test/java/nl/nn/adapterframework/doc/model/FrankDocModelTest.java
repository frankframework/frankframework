package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.SELECTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankDocModelTest {
	private static final String SIMPLE = "nl.nn.adapterframework.doc.testtarget.simple";
	private static final String LISTENER = SIMPLE + ".IListener";
	private static final String SIMPLE_PARENT = SIMPLE + ".ListenerParent";
	private static final String SIMPLE_CHILD = SIMPLE + ".ListenerChild";
	private static final String SIMPLE_GRNAD_CHILD = SIMPLE + ".ListenerGrandChild";
	private static final String SIMPLE_GRNAD_PARENT = SIMPLE + ".AbstractGrandParent";

	private static final String IBISDOCREF = "nl.nn.adapterframework.doc.testtarget.ibisdocref";
	private static final String REFERRED_CHILD = IBISDOCREF + ".ChildTarget";
	private static final String REFERRED_PARENT = IBISDOCREF + ".ParentTarget";
	private static final String REFERRER = "nl.nn.adapterframework.doc.testtarget.ibisdocref.Referrer";
	private static final String REFERRER_CHILD = "nl.nn.adapterframework.doc.testtarget.ibisdocref.ReferrerChild";

	private FrankDocModel instance;
	private FrankElement fakeAttributeOwner;

	@Before
	public void setUp() {
		instance = new FrankDocModel();
		fakeAttributeOwner = null;
	}

	@Test
	public void whenInterfaceTypeAndSingletonTypeThenCorrectElements() throws ReflectiveOperationException {
		ElementType listenerType = instance.findOrCreateElementType(Utils.getClass(LISTENER));
		ElementType childType = instance.findOrCreateElementType(Utils.getClass(SIMPLE_CHILD));
		checkModelTypes(listenerType, childType);
	}

	@Test
	public void whenSingletonTypeAndInterfaceTypeThenCorrectElements() throws ReflectiveOperationException {
		ElementType childType = instance.findOrCreateElementType(Utils.getClass(SIMPLE_CHILD));
		ElementType listenerType = instance.findOrCreateElementType(Utils.getClass(LISTENER));
		checkModelTypes(listenerType, childType);		
	}

	private void checkModelTypes(ElementType actualListener, ElementType actualChild) {
		assertTrue(instance.hasType(actualListener.getFullName()));
		assertTrue(instance.hasType(actualChild.getFullName()));
		assertSame(instance.getAllTypes().get(LISTENER), actualListener);
		assertSame(instance.getAllTypes().get(SIMPLE_CHILD), actualChild);
		Assert.assertSame(instance.getAllElements().get(SIMPLE_CHILD), actualChild.getMembers().get(SIMPLE_CHILD));
		Assert.assertTrue(instance.getAllElements().containsKey(SIMPLE_PARENT));
		Map<String, FrankElement> listenerMembers = actualListener.getMembers();
		// Tests that AbstractGrandParent is omitted.
		assertEquals(3, listenerMembers.size());
		assertTrue(listenerMembers.containsKey(SIMPLE_PARENT));
		assertTrue(listenerMembers.containsKey(SIMPLE_CHILD));
		assertTrue(listenerMembers.containsKey(SIMPLE_GRNAD_CHILD));
		Map<String, FrankElement> childMembers = actualChild.getMembers();
		assertEquals(1, childMembers.size());
		assertTrue(childMembers.containsKey(SIMPLE_CHILD));
		assertEquals(LISTENER, actualListener.getFullName());
		assertEquals("IListener", actualListener.getSimpleName());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
	}

	@Test
	public void whenTypeRequestedTwiceThenSameInstanceReturned() throws ReflectiveOperationException {
		ElementType first = instance.findOrCreateElementType(Utils.getClass(SIMPLE_CHILD));
		ElementType second = instance.findOrCreateElementType(Utils.getClass(SIMPLE_CHILD));
		assertSame(first, second);
	}

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() throws ReflectiveOperationException {
		FrankElement child = instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_CHILD));
		FrankElement parent = instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_PARENT));
		instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_GRNAD_CHILD));
		instance.setOverriddenFrom();
		checkModelAfterChildAndParentAdded(parent, child);
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() throws ReflectiveOperationException {
		FrankElement parent = instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_PARENT));
		FrankElement child = instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_CHILD));
		instance.findOrCreateFrankElement(Utils.getClass(SIMPLE_GRNAD_CHILD));
		instance.setOverriddenFrom();
		checkModelAfterChildAndParentAdded(parent, child);
	}

	private void checkModelAfterChildAndParentAdded(FrankElement actualParent, FrankElement actualChild) {
		Map<String, FrankElement> actualAllElements = instance.getAllElements();
		assertTrue(actualAllElements.containsKey(actualParent.getFullName()));
		assertTrue(actualAllElements.containsKey(actualChild.getFullName()));
		assertSame(actualAllElements.get(actualParent.getFullName()), actualParent);
		assertSame(actualAllElements.get(actualChild.getFullName()), actualChild);
		FrankElement actualGrandParent = actualAllElements.get(SIMPLE_GRNAD_PARENT);
		FrankElement actualObject = actualAllElements.get("java.lang.Object");
		assertNull(actualObject.getParent());
		assertSame(actualObject, actualGrandParent.getParent());
		assertSame(actualGrandParent, actualParent.getParent());
		assertEquals(SIMPLE_PARENT, actualParent.getFullName());
		assertEquals("ListenerParent", actualParent.getSimpleName());
		// We check here that protected method getChildAttribute does not produce
		// an attribute
		assertEquals(2, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = findAttribute(actualParent, "parentAttribute");
		assertEquals("parentAttribute", actualParentAttribute.getName());
		assertSame(actualParent, actualParentAttribute.getDescribingElement());
		assertSame(actualParent, actualParentAttribute.getOwningElement());
		assertNull(actualParentAttribute.getOverriddenFrom());
		FrankAttribute actualInheritedAttribute = findAttribute(actualParent, "inheritedAttribute");
		assertEquals("inheritedAttribute", actualInheritedAttribute.getName());
		assertNull(actualInheritedAttribute.getOverriddenFrom());
		assertSame(actualParent, actualChild.getParent());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
		assertEquals(2, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = findAttribute(actualChild, "childAttribute");
		assertEquals("childAttribute", actualChildAttribute.getName());
		assertSame(actualChild, actualChildAttribute.getOwningElement());
		assertSame(actualChild, actualChildAttribute.getDescribingElement());
		assertNull(actualChildAttribute.getOverriddenFrom());
		actualInheritedAttribute = findAttribute(actualChild, "inheritedAttribute");
		assertEquals("inheritedAttribute", actualInheritedAttribute.getName());
		assertSame(actualParent, actualInheritedAttribute.getOverriddenFrom());
		FrankElement actualGrandChild = actualAllElements.get(SIMPLE_GRNAD_CHILD);
		assertEquals(SIMPLE_GRNAD_CHILD, actualGrandChild.getFullName());
		assertEquals(1, actualGrandChild.getAttributes().size());
		actualInheritedAttribute = actualGrandChild.getAttributes().get(0);
		assertEquals("inheritedAttribute", actualInheritedAttribute.getName());
		assertSame(actualParent, actualInheritedAttribute.getOverriddenFrom());
	}

	private FrankAttribute findAttribute(final FrankElement elem, String name) {
		for(FrankAttribute attribute: elem.getAttributes()) {
			if(attribute.getName().contentEquals(name)) {
				return attribute;
			}
		}
		return null;
	}

	@Test
	public void whenSetterAndGetterThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeSetterGetter");
	}

	private FrankAttribute checkReflectAttributeCreated(String attributeName) throws ReflectiveOperationException {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertTrue(actual.containsKey(attributeName));
		assertEquals(attributeName, actual.get(attributeName).getName());
		return actual.get(attributeName);
	}

	private Map<String, FrankAttribute> getReflectInvestigatedFrankAttributes() throws ReflectiveOperationException {
		return getAttributesOfClass("nl.nn.adapterframework.doc.testtarget.reflect.FrankAttributeTarget");
	}

	/**
	 * Asks the system-under-test class {@link FrankDocModel} for the FrankAttribute objects
	 * of a class. A dummy FrankElement is supplied as attribute owner, so the
	 * describingElement is only correct if it is parsed from an @IbisDocRef annotation.
	 */
	private Map<String, FrankAttribute> getAttributesOfClass(final String className) throws ReflectiveOperationException {
		fakeAttributeOwner = new FrankElement("dummy.Dummy", "Dummy");
		final List<FrankAttribute> attributes = instance.createAttributes(Utils.getClass(className).getDeclaredMethods(), fakeAttributeOwner);
		return attributes.stream().collect(Collectors.toMap(att -> att.getName(), att -> att));		
	}

	@Test
	public void whenSetterAndIsThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeSetterIs");
	}

	@Test
	public void whenOnlySetterThenAttribute() throws ReflectiveOperationException {
		FrankAttribute attribute = checkReflectAttributeCreated("attributeOnlySetter");
		assertFalse(attribute.isDocumented());
	}

	@Test
	public void whenSetterHasPrimitiveTypeThenAttribute() throws ReflectiveOperationException {
		FrankAttribute attribute = checkReflectAttributeCreated("attributeOnlySetterInt");
		assertFalse(attribute.isDocumented());
	}

	@Test
	public void whenSetterHasBoxedIntTypeThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeOnlySetterIntBoxed");
	}

	@Test
	public void whenSetterHasBoxedBoolTypeThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeOnlySetterBoolBoxed");
	}

	@Test
	public void whenSetterHasBoxedLongTypeThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeOnlySetterLongBoxed");
	}

	@Test
	public void whenSetterHasBoxedByteTypeThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeOnlySetterByteBoxed");
	}

	@Test
	public void whenSetterHasBoxedShortTypeThenAttribute() throws ReflectiveOperationException {
		checkReflectAttributeCreated("attributeOnlySetterShortBoxed");
	}

	private void checkReflectAttributeOmitted(String attributeName) throws ReflectiveOperationException {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertFalse(actual.containsKey(attributeName));
	}
	
	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() throws ReflectiveOperationException {
		checkReflectAttributeOmitted("noAttributeComplexType");
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() {
		assertFalse(getAttributeNameMap("get").containsKey("Prefix"));
	}

	Map<String, String> getAttributeNameMap(String prefix) {
		Map<String, Method> attributeToMethodMap = FrankDocModel.getAttributeToMethodMap(
				Utils.getClass("nl.nn.adapterframework.doc.testtarget.reflect.FrankAttributeTarget")
					.getDeclaredMethods(),
				prefix);
		Map<String, String> result = new HashMap<>();
		for(String attributeName: attributeToMethodMap.keySet()) {
			result.put(attributeName, attributeToMethodMap.get(attributeName).getName());
		}
		return result;
	}

	@Test
	public void whenAttributeNameEqualsPrefixThenFilteredOutOfAttributes() {
		assertFalse(getAttributeNameMap("get").containsKey(""));
	}

	@Test
	public void whenSetterTakesTwoValuesThenNotSetter() {
		assertFalse(getAttributeNameMap("set").containsKey("invalidSetter"));
	}

	@Test
	public void whenSetterTakesNoValuesThenNoSetter() {
		assertFalse(getAttributeNameMap("set").containsKey("invalidSetterNoParams"));
	}

	@Test
	public void testIbisDockedOnlyDescription() throws ReflectiveOperationException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOnlyDescription");
		assertTrue(actual.isDocumented());
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedOnlyDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescription() throws ReflectiveOperationException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescription");
		assertTrue(actual.isDocumented());
		assertEquals(3, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDescriptionDefault() throws ReflectiveOperationException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
		assertTrue(SELECTED.test(actual));
	}

	@Test
	public void testIbisDockedOrderDescriptionDefault() throws ReflectiveOperationException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals(5, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedOrderDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDeprecated() throws ReflectiveOperationException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDeprecated");
		assertTrue(actual.isDocumented());
		assertEquals("Description of ibisDockedDeprecated", actual.getDescription());
		assertTrue(actual.isDeprecated());
		assertFalse(SELECTED.test(actual));
	}

	@Test
	public void testIbisDocRefAddsFrankElementsForReferredClassHierarchy() throws ReflectiveOperationException {
		checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertEquals(3, instance.getAllElements().size());
		assertTrue(instance.getAllElements().containsKey(REFERRED_CHILD));
		assertTrue(instance.getAllElements().containsKey(REFERRED_PARENT));
		assertTrue(instance.getAllElements().containsKey("java.lang.Object"));
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName) throws ReflectiveOperationException {
		return checkIbisdocrefInvestigatedFrankAttribute(attributeName, REFERRER);
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName, String targetClassName) throws ReflectiveOperationException {
		Map<String, FrankAttribute> attributeMap =
				getAttributesOfClass(targetClassName);
		assertTrue(attributeMap.containsKey(attributeName));
		return attributeMap.get(attributeName);
	}

	@Test
	public void testReferredIbisDocDescriptionAppearsInFrankAttribute() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptionOtherMethodAppearsInFrankAttribute() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocReffMethodNoOrderRefersIbisDocOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals("Description of otherMethod", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptiondWithOrderAndInheritance() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertEquals(
				"Description of ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited",
				actual.getDescription());
	}

	@Test
	public void testOrderInsideIbisDocRefHasPreferenceOverReferredIbisDocOrder() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertEquals(10, actual.getOrder());
	}

	@Test
	public void whenIbisDocRefThenDescribingElementAdjusted() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertSame(
				instance.getAllElements().get(REFERRED_PARENT),
				actual.getDescribingElement());
		assertSame(
				fakeAttributeOwner,
				actual.getOwningElement());
	}

	@Test
	public void whenMethodOverriddenWithoutDocThenDocumentedFalseButIbisDocRefInfoInherited() throws ReflectiveOperationException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", REFERRER_CHILD);
		assertFalse(actual.isDocumented());
		assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}
}
