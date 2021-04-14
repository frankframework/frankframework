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

import static nl.nn.adapterframework.frankdoc.model.ElementChild.ALL;
import static nl.nn.adapterframework.frankdoc.model.ElementChild.IN_XSD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.frankdoc.doclet.Environment;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

@RunWith(Parameterized.class)
public class FrankDocModelTest {
	private static final String SIMPLE = "nl.nn.adapterframework.frankdoc.testtarget.simple";
	private static final String LISTENER = SIMPLE + ".IListener";
	private static final String SIMPLE_PARENT = SIMPLE + ".ListenerParent";
	private static final String SIMPLE_CHILD = SIMPLE + ".ListenerChild";
	private static final String SIMPLE_GRNAD_CHILD = SIMPLE + ".ListenerGrandChild";
	private static final String SIMPLE_GRNAD_PARENT = SIMPLE + ".AbstractGrandParent";
	private static final String FOR_XSD_ELEMENT_NAME_TEST = SIMPLE + ".ParentListener";

	private static final String IBISDOCREF = "nl.nn.adapterframework.frankdoc.testtarget.ibisdocref";
	private static final String REFERRED_CHILD = IBISDOCREF + ".ChildTarget";
	private static final String REFERRED_PARENT = IBISDOCREF + ".ParentTarget";
	private static final String REFERRER = "nl.nn.adapterframework.frankdoc.testtarget.ibisdocref.Referrer";
	private static final String REFERRER_CHILD = "nl.nn.adapterframework.frankdoc.testtarget.ibisdocref.ReferrerChild";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> result = new ArrayList<>();
		Arrays.asList(Environment.values()).forEach(v -> result.add(new Object[] {v}));
		return result;
	}

	@Parameter
	public Environment environment;

	FrankClassRepository classRepository;

	private FrankDocModel instance;
	private FrankElement attributeOwner;

	@Before
	public void setUp() {
		List<String> allPackages = Arrays.asList(SIMPLE, IBISDOCREF, "nl.nn.adapterframework.frankdoc.testtarget.reflect");
		classRepository = environment.getRepository(allPackages, allPackages, new ArrayList<String>(), new ArrayList<>());
		instance = new FrankDocModel(classRepository);
		attributeOwner = null;
	}

	@Test
	public void whenInterfaceTypeAndSingletonTypeThenCorrectElements() throws FrankDocException {
		ElementType listenerType = instance.findOrCreateElementType(classRepository.findClass(LISTENER));
		ElementType childType = instance.findOrCreateElementType(classRepository.findClass(SIMPLE_CHILD));
		checkModelTypes(listenerType, childType);
	}

	@Test
	public void whenSingletonTypeAndInterfaceTypeThenCorrectElements() throws FrankDocException {
		ElementType childType = instance.findOrCreateElementType(classRepository.findClass(SIMPLE_CHILD));
		ElementType listenerType = instance.findOrCreateElementType(classRepository.findClass(LISTENER));
		checkModelTypes(listenerType, childType);		
	}

	private void checkModelTypes(ElementType actualListener, ElementType actualChild) {
		assertTrue(instance.hasType(actualListener.getFullName()));
		assertTrue(instance.hasType(actualChild.getFullName()));
		assertSame(instance.getAllTypes().get(LISTENER), actualListener);
		assertSame(instance.getAllTypes().get(SIMPLE_CHILD), actualChild);
		assertSame(instance.getAllElements().get(SIMPLE_CHILD), getMember(actualChild.getMembers(), SIMPLE_CHILD));
		Assert.assertTrue(instance.getAllElements().containsKey(SIMPLE_PARENT));
		Assert.assertTrue(instance.getAllElements().containsKey(FOR_XSD_ELEMENT_NAME_TEST));
		List<FrankElement> listenerMembers = actualListener.getMembers();
		// Tests that AbstractGrandParent is omitted.
		assertEquals(4, listenerMembers.size());
		assertTrue(membersContain(listenerMembers, SIMPLE_PARENT));
		assertTrue(membersContain(listenerMembers, SIMPLE_CHILD));
		assertTrue(membersContain(listenerMembers, SIMPLE_GRNAD_CHILD));
		List<FrankElement> childMembers = actualChild.getMembers();
		assertEquals(1, childMembers.size());
		assertTrue(membersContain(childMembers, SIMPLE_CHILD));
		assertEquals(LISTENER, actualListener.getFullName());
		assertEquals("IListener", actualListener.getSimpleName());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
	}

	private boolean membersContain(List<FrankElement> elements, String fullName) {
		return elements.stream().anyMatch(elem -> elem.getFullName().equals(fullName));
	}

	private FrankElement getMember(List<FrankElement> elements, String fullName) {
		for(FrankElement element: elements) {
			if(element.getFullName().equals(fullName)) {
				return element;
			}
		}
		return null;
	}

	@Test
	public void whenTypeRequestedTwiceThenSameInstanceReturned() throws FrankDocException {
		ElementType first = instance.findOrCreateElementType(classRepository.findClass(SIMPLE_CHILD));
		ElementType second = instance.findOrCreateElementType(classRepository.findClass(SIMPLE_CHILD));
		assertSame(first, second);
	}

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() throws FrankDocException {
		FrankElement child = instance.findOrCreateFrankElement(SIMPLE_CHILD);
		FrankElement parent = instance.findOrCreateFrankElement(SIMPLE_PARENT);
		instance.findOrCreateFrankElement(SIMPLE_GRNAD_CHILD);
		instance.setOverriddenFrom();
		checkModelAfterChildAndParentAdded(parent, child);
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() throws FrankDocException {
		FrankElement parent = instance.findOrCreateFrankElement(SIMPLE_PARENT);
		FrankElement child = instance.findOrCreateFrankElement(SIMPLE_CHILD);
		instance.findOrCreateFrankElement(SIMPLE_GRNAD_CHILD);
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
		assertTrue(actualGrandParent.isAbstract());
		FrankElement actualObject = actualAllElements.get("java.lang.Object");
		assertNull(actualObject.getParent());
		assertSame(actualObject, actualGrandParent.getParent());
		assertSame(actualGrandParent, actualParent.getParent());
		assertEquals(SIMPLE_PARENT, actualParent.getFullName());
		assertEquals("ListenerParent", actualParent.getSimpleName());
		assertFalse(actualParent.isAbstract());
		// We check here that protected method getChildAttribute does not produce
		// an attribute
		assertEquals(3, actualParent.getAttributes(ALL).size());
		FrankAttribute actualParentAttribute = findAttribute(actualParent, "parentAttribute");
		assertEquals("parentAttribute", actualParentAttribute.getName());
		assertSame(actualParent, actualParentAttribute.getDescribingElement());
		assertSame(actualParent, actualParentAttribute.getOwningElement());
		assertNull(actualParentAttribute.getOverriddenFrom());
		assertFalse(actualParentAttribute.isTechnicalOverride());
		FrankAttribute actualInheritedAttribute = findAttribute(actualParent, "inheritedAttribute");
		assertEquals("inheritedAttribute", actualInheritedAttribute.getName());
		assertNull(actualInheritedAttribute.getOverriddenFrom());
		assertSame(actualParent, actualChild.getParent());
		assertEquals(SIMPLE_CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
		assertEquals(3, actualChild.getAttributes(ALL).size());
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
		assertEquals(1, actualGrandChild.getAttributes(ALL).size());
		actualInheritedAttribute = actualGrandChild.getAttributes(ALL).get(0);
		assertEquals("inheritedAttribute", actualInheritedAttribute.getName());
		assertSame(actualChild, actualInheritedAttribute.getOverriddenFrom());
		// Deprecated
		FrankAttribute attribute = actualParent.getAttributes(a -> ((FrankAttribute) a).getName().equals(
				"deprecatedInParentAttribute")).get(0);
		assertTrue(attribute.isDeprecated());
		attribute = actualChild.getAttributes(a -> ((FrankAttribute) a).getName().equals(
				"deprecatedInParentAttribute")).get(0);
		assertFalse(attribute.isDeprecated());
	}

	private FrankAttribute findAttribute(final FrankElement elem, String name) {
		for(FrankAttribute attribute: elem.getAttributes(ALL)) {
			if(attribute.getName().contentEquals(name)) {
				return attribute;
			}
		}
		return null;
	}

	@Test
	public void whenSetterAndGetterThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeSetterGetter");
	}

	private FrankAttribute checkReflectAttributeCreated(String attributeName) throws FrankDocException {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertTrue(actual.containsKey(attributeName));
		assertEquals(attributeName, actual.get(attributeName).getName());
		return actual.get(attributeName);
	}

	private Map<String, FrankAttribute> getReflectInvestigatedFrankAttributes() throws FrankDocException {
		return getAttributesOfClass("nl.nn.adapterframework.frankdoc.testtarget.reflect.FrankAttributeTarget");
	}

	/**
	 * Asks the system-under-test class {@link FrankDocModel} for the FrankAttribute objects
	 * of a class. A dummy FrankElement is supplied as attribute owner, so the
	 * describingElement is only correct if it is parsed from an @IbisDocRef annotation.
	 */
	private Map<String, FrankAttribute> getAttributesOfClass(final String className) throws FrankDocException {
		attributeOwner = instance.findOrCreateFrankElement(className);
		final List<FrankAttribute> attributes = instance.createAttributes(classRepository.findClass(className), attributeOwner);
		return attributes.stream().collect(Collectors.toMap(att -> att.getName(), att -> att));		
	}

	@Test
	public void whenSetterAndIsThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeSetterIs");
	}

	@Test
	public void whenOnlySetterThenAttribute() throws FrankDocException {
		FrankAttribute attribute = checkReflectAttributeCreated("attributeOnlySetter");
		assertFalse(attribute.isDocumented());
	}

	/**
	 * This test only has added value when testing with ClassDoc-s. This has
	 * to do with method {@link nl.nn.adapterframework.frankdoc.Utils#isAttributeGetterOrSetter(FrankMethod)}.
	 * That method filters method using {@link nl.nn.adapterframework.frankdoc.doclet.FrankMethod#isVarargs()}.
	 * That filter is only needed when a varargs String argument appears as a String argument type.
	 * This might be the case for ClassDoc but not for Java reflection. With reflection, a
	 * varargs String appears as type String[].
	 * <p>
	 * Probably, vararg strings arguments only appear as simple String arguments for ClassDoc-s
	 * if a doclet does not set the languageVersion to JAVA_1_5. There was no need to investigate
	 * further, because the JAVA_1_5 language version is now set, see {@link nl.nn.adapterframework.frankdoc.front.DocletBuilder#languageVersion()}.
	 * Furthermore, filtering with isVarargs() certainly does no harm.
	 * @throws FrankDocException
	 */
	@Test
	public void whenArgIsVarargThenNotAttribute() throws FrankDocException {
		checkReflectAttributeOmitted("setNonAttributeVararg");
	}

	@Test
	public void whenSetterHasPrimitiveTypeThenAttribute() throws FrankDocException {
		FrankAttribute attribute = checkReflectAttributeCreated("attributeOnlySetterInt");
		assertFalse(attribute.isDocumented());
	}

	@Test
	public void whenSetterHasBoxedIntTypeThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeOnlySetterIntBoxed");
	}

	@Test
	public void whenSetterHasBoxedBoolTypeThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeOnlySetterBoolBoxed");
	}

	@Test
	public void whenSetterHasBoxedLongTypeThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeOnlySetterLongBoxed");
	}

	@Test
	public void whenSetterHasBoxedByteTypeThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeOnlySetterByteBoxed");
	}

	@Test
	public void whenSetterHasBoxedShortTypeThenAttribute() throws FrankDocException {
		checkReflectAttributeCreated("attributeOnlySetterShortBoxed");
	}

	private void checkReflectAttributeOmitted(String attributeName) throws FrankDocException {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		assertFalse(actual.containsKey(attributeName));
	}
	
	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() throws FrankDocException {
		checkReflectAttributeOmitted("noAttributeComplexType");
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() throws FrankDocException {
		assertFalse(getAttributeNameMap("get").containsKey("Prefix"));
	}

	Map<String, String> getAttributeNameMap(String prefix) throws FrankDocException {
		Map<String, FrankMethod> attributeToMethodMap = FrankDocModel.getAttributeToMethodMap(
				classRepository.findClass("nl.nn.adapterframework.frankdoc.testtarget.reflect.FrankAttributeTarget").getDeclaredMethods(), prefix);
		Map<String, String> result = new HashMap<>();
		for(String attributeName: attributeToMethodMap.keySet()) {
			result.put(attributeName, attributeToMethodMap.get(attributeName).getName());
		}
		return result;
	}

	@Test
	public void whenAttributeNameEqualsPrefixThenFilteredOutOfAttributes() throws FrankDocException {
		assertFalse(getAttributeNameMap("get").containsKey(""));
	}

	@Test
	public void whenSetterTakesTwoValuesThenNotSetter() throws FrankDocException {
		assertFalse(getAttributeNameMap("set").containsKey("invalidSetter"));
	}

	@Test
	public void whenSetterTakesNoValuesThenNoSetter() throws FrankDocException {
		assertFalse(getAttributeNameMap("set").containsKey("invalidSetterNoParams"));
	}

	@Test
	public void testIbisDockedOnlyDescription() throws FrankDocException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOnlyDescription");
		assertTrue(actual.isDocumented());
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedOnlyDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescription() throws FrankDocException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescription");
		assertTrue(actual.isDocumented());
		assertEquals(3, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescription", actual.getDescription());
		assertNull(actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDescriptionDefault() throws FrankDocException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals(Integer.MAX_VALUE, actual.getOrder());
		assertEquals("Description of ibisDockedDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
		assertTrue(IN_XSD.test(actual));
	}

	@Test
	public void testIbisDockedOrderDescriptionDefault() throws FrankDocException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals(5, actual.getOrder());
		assertEquals("Description of ibisDockedOrderDescriptionDefault", actual.getDescription());
		assertEquals("Default of ibisDockedOrderDescriptionDefault", actual.getDefaultValue());
		assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDeprecated() throws FrankDocException {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDeprecated");
		assertTrue(actual.isDocumented());
		assertEquals("Description of ibisDockedDeprecated", actual.getDescription());
		assertTrue(actual.isDeprecated());
		assertFalse(IN_XSD.test(actual));
	}

	@Test
	public void testIbisDocRefAddsFrankElementsForReferredClassHierarchy() throws FrankDocException {
		checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertEquals(4, instance.getAllElements().size());
		assertTrue(instance.getAllElements().containsKey(REFERRER));
		assertTrue(instance.getAllElements().containsKey(REFERRED_CHILD));
		assertTrue(instance.getAllElements().containsKey(REFERRED_PARENT));
		assertTrue(instance.getAllElements().containsKey("java.lang.Object"));
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName) throws FrankDocException {
		return checkIbisdocrefInvestigatedFrankAttribute(attributeName, REFERRER);
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName, String targetClassName) throws FrankDocException {
		Map<String, FrankAttribute> attributeMap = getAttributesOfClass(targetClassName);
		assertTrue(attributeMap.containsKey(attributeName));
		return attributeMap.get(attributeName);
	}

	@Test
	public void testReferredIbisDocDescriptionAppearsInFrankAttribute() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptionOtherMethodAppearsInFrankAttribute() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocReffMethodNoOrderRefersIbisDocOrderDescriptionDefault");
		assertTrue(actual.isDocumented());
		assertEquals("Description of otherMethod", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptiondWithOrderAndInheritance() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertEquals("Description of ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited", actual.getDescription());
	}

	@Test
	public void testOrderInsideIbisDocRefHasPreferenceOverReferredIbisDocOrder() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertEquals(10, actual.getOrder());
	}

	@Test
	public void whenIbisDocRefThenDescribingElementAdjusted() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		assertTrue(actual.isDocumented());
		assertSame(instance.getAllElements().get(REFERRED_PARENT), actual.getDescribingElement());
		assertSame(attributeOwner, actual.getOwningElement());
	}

	@Test
	public void whenMethodOverriddenWithoutDocThenDocumentedFalseButIbisDocRefInfoInherited() throws FrankDocException {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", REFERRER_CHILD);
		assertFalse(actual.isDocumented());
		assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}

	@Test
	public void testFrankElementDeprecatedAttribute() throws Exception {
		FrankElement element = instance.findOrCreateFrankElement(SIMPLE + ".NonDeprecatedDescendant");
		assertNotNull(element);
		assertFalse(element.isDeprecated());
		assertTrue(element.getParent().isDeprecated());
	}
}
