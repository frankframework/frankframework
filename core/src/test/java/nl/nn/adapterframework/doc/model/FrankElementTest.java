package nl.nn.adapterframework.doc.model;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder;

public class FrankElementTest {
	private static final String IBISDOCREF = "nl.nn.adapterframework.doc.target.ibisdocref";
	private static final String REFERRED_CHILD = IBISDOCREF + ".ChildTarget";
	private static final String REFERRED_PARENT = IBISDOCREF + ".ParentTarget";

	private FrankElement.FrankElementStore store;

	@Before
	public void setUp() {
		store = new FrankDocModel();
	}

	@Test
	public void whenSetterAndGetterThenAttribute() {
		checkReflectAttributeCreated("attributeSetterGetter");
	}

	private FrankAttribute checkReflectAttributeCreated(String attributeName) {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		Assert.assertTrue(actual.containsKey(attributeName));
		Assert.assertEquals(attributeName, actual.get(attributeName).getName());
		return actual.get(attributeName);
	}

	private Map<String, FrankAttribute> getReflectInvestigatedFrankAttributes() {
		return getAttributesOfClass("nl.nn.adapterframework.doc.target.reflect.FrankAttributeTarget");
	}

	/**
	 * @param className The name of the Class<?> to get FrankAttribute objects for.
	 * @return The FrankAttributes that ModelBuilder can produce from a Class<?>. Note
	 * that the describingElement is not set, because we are not testing here in relation
	 * to creating the enclosing FrankElement of the attributes.
	 */
	private Map<String, FrankAttribute> getAttributesOfClass(final String className) {
		final List<FrankAttribute> attributes = new FrankElement("dummy.Dummy", "Dummy")
				.createAttributes(
						ModelBuilder.getClass(className).getDeclaredMethods(), store);
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

	private void checkReflectAttributeOmitted(String attributeName) {
		Map<String, FrankAttribute> actual = getReflectInvestigatedFrankAttributes();
		Assert.assertFalse(actual.containsKey(attributeName));
	}
	
	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() {
		checkReflectAttributeOmitted("noAttributeComplexType");
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() {
		Assert.assertFalse(getAttributeNameMap("get").containsKey("Prefix"));
	}

	Map<String, String> getAttributeNameMap(String prefix) {
		Map<String, Method> attributeToMethodMap = FrankElement.getAttributeToMethodMap(
				ModelBuilder.getClass("nl.nn.adapterframework.doc.target.reflect.FrankAttributeTarget")
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
		Assert.assertFalse(getAttributeNameMap("get").containsKey(""));
	}

	@Test
	public void whenSetterTakesTwoValuesThenNotSetter() {
		Assert.assertFalse(getAttributeNameMap("set").containsKey("invalidSetter"));
	}

	@Test
	public void testIbisDockedOnlyDescription() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOnlyDescription");
		Assert.assertEquals(Integer.MAX_VALUE, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOnlyDescription", actual.getDescription());
		Assert.assertNull(actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescription() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescription");
		Assert.assertEquals(3, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOrderDescription", actual.getDescription());
		Assert.assertNull(actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDescriptionDefault() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDescriptionDefault");
		Assert.assertEquals(Integer.MAX_VALUE, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedDescriptionDefault", actual.getDescription());
		Assert.assertEquals("Default of ibisDockedDescriptionDefault", actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescriptionDefault() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedOrderDescriptionDefault");
		Assert.assertEquals(5, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOrderDescriptionDefault", actual.getDescription());
		Assert.assertEquals("Default of ibisDockedOrderDescriptionDefault", actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDeprecated() {
		FrankAttribute actual = checkReflectAttributeCreated("ibisDockedDeprecated");
		Assert.assertEquals("Description of ibisDockedDeprecated", actual.getDescription());
		Assert.assertTrue(actual.isDeprecated());
	}

	@Test
	public void testIbisDocRefAddsFrankElementsForReferredClassHierarchy() {
		checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		Assert.assertEquals(3, store.numFrankElements());
		Assert.assertTrue(store.hasFrankElement(REFERRED_CHILD));
		Assert.assertTrue(store.hasFrankElement(REFERRED_PARENT));
		Assert.assertTrue(store.hasFrankElement("java.lang.Object"));
	}

	private FrankAttribute checkIbisdocrefInvestigatedFrankAttribute(String attributeName) {
		Map<String, FrankAttribute> attributeMap =
				getAttributesOfClass("nl.nn.adapterframework.doc.target.ibisdocref.Referrer");
		Assert.assertTrue(attributeMap.containsKey(attributeName));
		return attributeMap.get(attributeName);
	}

	@Test
	public void testReferredIbisDocDescriptionAppearsInFrankAttribute() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault");
		Assert.assertEquals("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptionOtherMethodAppearsInFrankAttribute() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocReffMethodNoOrderRefersIbisDocOrderDescriptionDefault");
		Assert.assertEquals("Description of otherMethod", actual.getDescription());
	}

	@Test
	public void testReferredIbisDocDescriptiondWithOrderAndInheritance() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		Assert.assertEquals(
				"Description of ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited",
				actual.getDescription());
	}

	@Test
	public void testOrderInsideIbisDocRefHasPreferenceOverReferredIbisDocOrder() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		Assert.assertEquals(10, actual.getOrder());
	}

	@Test
	public void whenIbisDocRefThenDescribingElementAdjusted() {
		FrankAttribute actual = checkIbisdocrefInvestigatedFrankAttribute("ibisDocRefClassWithOrderRefersIbisDocOrderDescriptionDefaultInherited");
		Assert.assertSame(
				store.getFrankElement(REFERRED_PARENT),
				actual.getDescribingElement());		
	}
}
