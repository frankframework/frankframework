package nl.nn.adapterframework.doc.model;

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

import nl.nn.adapterframework.doc.ModelBuilder;

public class FrankDocModelTest {
	private static final String SIMPLE = "nl.nn.adapterframework.doc.testtarget.simple";
	private static final String LISTENER = SIMPLE + ".IListener";
	private static final String SIMPLE_PARENT = SIMPLE + ".ListenerParent";
	private static final String SIMPLE_CHILD = SIMPLE + ".ListenerChild";

	private static final String IBISDOCREF = "nl.nn.adapterframework.doc.testtarget.ibisdocref";
	private static final String REFERRED_CHILD = IBISDOCREF + ".ChildTarget";
	private static final String REFERRED_PARENT = IBISDOCREF + ".ParentTarget";

	private FrankDocModel instance;

	@Before
	public void setUp() {
		instance = new FrankDocModel();
	}

	@Test
	public void whenInterfaceTypeAndSingletonTypeThenCorrectElements() {
		ElementType listenerType = instance.findOrCreateElementType(ModelBuilder.getClass(LISTENER));
		ElementType childType = instance.findOrCreateElementType(ModelBuilder.getClass(SIMPLE_CHILD));
		checkModelTypes(listenerType, childType);
	}

	@Test
	public void whenSingletonTypeAndInterfaceTypeThenCorrectElements() {
		ElementType childType = instance.findOrCreateElementType(ModelBuilder.getClass(SIMPLE_CHILD));
		ElementType listenerType = instance.findOrCreateElementType(ModelBuilder.getClass(LISTENER));
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
		assertEquals(2, listenerMembers.size());
		assertTrue(listenerMembers.containsKey(SIMPLE_PARENT));
		assertTrue(listenerMembers.containsKey(SIMPLE_CHILD));
		Map<String, FrankElement> childMembers = actualChild.getMembers();
		assertEquals(1, childMembers.size());
		assertTrue(childMembers.containsKey(SIMPLE_CHILD));
		assertEquals(LISTENER, actualListener.getFullName());
		assertEquals("IListener", actualListener.getSimpleName());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
	}

	@Test
	public void whenTypeRequestedTwiceThenSameInstanceReturned() {
		ElementType first = instance.findOrCreateElementType(ModelBuilder.getClass(SIMPLE_CHILD));
		ElementType second = instance.findOrCreateElementType(ModelBuilder.getClass(SIMPLE_CHILD));
		assertSame(first, second);
	}

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() {
		FrankElement child = instance.findOrCreateFrankElement(ModelBuilder.getClass(SIMPLE_CHILD));
		FrankElement parent = instance.findOrCreateFrankElement(ModelBuilder.getClass(SIMPLE_PARENT));
		checkModelAfterChildAndParentAdded(parent, child);
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() {
		FrankElement parent = instance.findOrCreateFrankElement(ModelBuilder.getClass(SIMPLE_PARENT));
		FrankElement child = instance.findOrCreateFrankElement(ModelBuilder.getClass(SIMPLE_CHILD));
		checkModelAfterChildAndParentAdded(parent, child);
	}

	private void checkModelAfterChildAndParentAdded(FrankElement actualParent, FrankElement actualChild) {
		Map<String, FrankElement> actualAllElements = instance.getAllElements();
		assertTrue(actualAllElements.containsKey(actualParent.getFullName()));
		assertTrue(actualAllElements.containsKey(actualChild.getFullName()));
		assertSame(actualAllElements.get(actualParent.getFullName()), actualParent);
		assertSame(actualAllElements.get(actualChild.getFullName()), actualChild);
		FrankElement actualObject = actualAllElements.get("java.lang.Object");
		assertNull(actualObject.getParent());
		assertSame(actualObject, actualParent.getParent());
		assertEquals(SIMPLE_PARENT, actualParent.getFullName());
		assertEquals("ListenerParent", actualParent.getSimpleName());
		assertEquals(1, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = actualParent.getAttributes().get(0);
		assertEquals("parentAttribute", actualParentAttribute.getName());
		assertSame(actualParent, actualParentAttribute.getDescribingElement());
		assertSame(actualParent, actualChild.getParent());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
		assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		assertEquals("childAttribute", actualChildAttribute.getName());
		assertSame(actualChild, actualChildAttribute.getDescribingElement());
	}

	@Test
	public void whenSetterAndGetterThenAttribute() {
		checkReflectAttributeCreated("attributeSetterGetter");
	}

	private FrankAttribute checkReflectAttributeCreated(String attributeName) {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertTrue(actual.containsKey(attributeName));
		assertEquals(attributeName, actual.get(attributeName).getName());
		return actual.get(attributeName);
	}

	private Map<String, FrankAttribute> getReflectInvestigatedFrankAttributes() {
		return getAttributesOfClass("nl.nn.adapterframework.doc.testtarget.reflect.FrankAttributeTarget");
	}

	/**
	 * Asks the system-under-test class {@link FrankDocModel} for the FrankAttribute objects
	 * of a class. A dummy FrankElement is supplied as attribute owner, so the
	 * describingElement is only correct if it is parsed from an @IbisDocRef annotation.
	 */
	private Map<String, FrankAttribute> getAttributesOfClass(final String className) {
		FrankElement dummy = new FrankElement("dummy.Dummy", "Dummy");
		final List<FrankAttribute> attributes = 
				instance.createAttributes(
						ModelBuilder.getClass(className).getDeclaredMethods(), dummy);
		return attributes.stream().collect(Collectors.toMap(att -> att.getName(), att -> att));		
	}

	@Test
	public void whenSetterAndIsThenAttribute() {
		checkReflectAttributeCreated("attributeSetterIs");
	}

	@Test
	public void whenOnlySetterThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetter");
	}

	@Test
	public void whenSetterHasPrimitiveTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterInt");
	}

	@Test
	public void whenSetterHasBoxedIntTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterIntBoxed");
	}

	@Test
	public void whenSetterHasBoxedBoolTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterBoolBoxed");
	}

	@Test
	public void whenSetterHasBoxedLongTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterLongBoxed");
	}

	@Test
	public void whenSetterHasBoxedByteTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterByteBoxed");
	}

	@Test
	public void whenSetterHasBoxedShortTypeThenAttribute() {
		checkReflectAttributeCreated("attributeOnlySetterShortBoxed");
	}

	private void checkReflectAttributeOmitted(String attributeName) {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertFalse(actual.containsKey(attributeName));
	}
	
	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() {
		checkReflectAttributeOmitted("noAttributeComplexType");
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() {
		assertFalse(getAttributeNameMap("get").containsKey("Prefix"));
	}

	Map<String, String> getAttributeNameMap(String prefix) {
		Map<String, Method> attributeToMethodMap = FrankDocModel.getAttributeToMethodMap(
				ModelBuilder.getClass("nl.nn.adapterframework.doc.testtarget.reflect.FrankAttributeTarget")
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
	public void testIbisDockedOnlyDescription() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOnlyDescription");
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedOnlyDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescription() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescription");
		assertEquals(3, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDescriptionDefault() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDescriptionDefault");
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescriptionDefault() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescriptionDefault");
		assertEquals(5, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedOrderDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDeprecated() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDeprecated");
		assertEquals("Description of ibisDockedDeprecated", actual.getDescription());
		assertTrue(actual.isDeprecated());
	}

	@Test
	public void testIbisDocRefAddsFrankElementsForReferredClassHierarchy() {
		checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertEquals(3, instance.getAllElements().size());
		assertTrue(instance.getAllElements().containsKey(REFERRED_CHILD));
		assertTrue(instance.getAllElements().containsKey(REFERRED_PARENT));
		assertTrue(instance.getAllElements().containsKey("java.lang.Object"));
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName) {
		Map<String, FrankAttribute> attributeMap =
				getAttributesOfClass("nl.nn.adapterframework.doc.testtarget.ibisdocref.Referrer");
		assertTrue(attributeMap.containsKey(attributeName));
		return attributeMap.get(attributeName);
	}

	@Test
	public void testReferredIbisDocDescriptionAppearsInFrankAttribute() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptionOtherMethodAppearsInFrankAttribute() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocReffMethodNoOrderRefersIbisDocOrderDescriptionDefault");
		assertEquals("Description of otherMethod", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptiondWithOrderAndInheritance() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertEquals(
				"Description of ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited",
				actual.getDescription());
	}

	@Test
	public void testOrderInsideIbisDocRefHasPreferenceOverReferredIbisDocOrder() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertEquals(10, actual.getOrder());
	}

	@Test
	public void whenIbisDocRefThenDescribingElementAdjusted() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertSame(
				instance.getAllElements().get(REFERRED_PARENT),
				actual.getDescribingElement());		
	}
}
