package nl.nn.adapterframework.doc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilderTest {
	private static final String SIMPLE = "nl.nn.adapterframework.doc.target.simple";
	private static final String PARENT = SIMPLE + ".ListenerParent";
	private static final String CHILD = SIMPLE + ".ListenerChild";
	
	@Test
	public void testGetSpringBeans() {
		List<SpringBean> actual = ModelBuilder.getSpringBeans(SIMPLE + ".IListener");
		actual.sort((b1, b2) -> b1.compareTo(b2));
		Assert.assertEquals(2, actual.size());
		for(SpringBean a: actual) {
			Assert.assertEquals(a.getClazz().getName(), a.getName());					
		}
		Iterator<SpringBean> it = actual.iterator();
		SpringBean first = it.next();
		Assert.assertEquals(SIMPLE + ".ListenerChild", first.getName());
		SpringBean second = it.next();
		Assert.assertEquals(SIMPLE + ".ListenerParent", second.getName());
	}

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.addGroup("Listeners");
		builder.addElementsToGroup(InfoBuilderSource.getClass(CHILD), group);
		builder.addElementsToGroup(	InfoBuilderSource.getClass(PARENT), group);
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.addGroup("Listeners");
		builder.addElementsToGroup(InfoBuilderSource.getClass(PARENT), group);
		builder.addElementsToGroup(InfoBuilderSource.getClass(CHILD), group);
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	private void checkModelAfterChildAndParentAdded(FrankDocModel model) {
		Assert.assertEquals(1, model.getGroups().size());
		FrankDocGroup actualGroup = model.getGroups().get(0);
		Map<String, FrankElement> actualAllElements = model.getAllElements();
		Assert.assertTrue(actualGroup.getElements().containsKey(PARENT));
		Assert.assertTrue(actualGroup.getElements().containsKey(CHILD));
		for(String elementInGroup: actualGroup.getElements().keySet()) {
			FrankElement groupElement = actualGroup.getElements().get(elementInGroup);
			FrankElement allElement = actualAllElements.get(elementInGroup);
			Assert.assertSame(
					"Different objects for group element and all element for element name: " + elementInGroup,
					groupElement, allElement);
		}
		FrankElement actualParent = actualGroup.getElements().get(PARENT);
		Assert.assertEquals(PARENT, actualParent.getFullName());
		Assert.assertEquals("ListenerParent", actualParent.getSimpleName());
		Assert.assertEquals(1, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = actualParent.getAttributes().get(0);
		Assert.assertEquals("parentAttribute", actualParentAttribute.getName());
		Assert.assertSame(actualParent, actualParentAttribute.getDescribingElement());
		FrankElement actualChild = actualGroup.getElements().get(CHILD);
		Assert.assertEquals(CHILD, actualChild.getFullName());
		Assert.assertEquals("ListenerChild", actualChild.getSimpleName());
		Assert.assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		Assert.assertEquals("childAttribute", actualChildAttribute.getName());
		Assert.assertSame(actualChild, actualChildAttribute.getDescribingElement());
	}

	@Test
	public void whenSetterAndGetterThenAttribute() {
		checkAttributeCreated("attributeSetterGetter");
	}

	private FrankAttribute checkAttributeCreated(String attributeName) {
		Map<String, FrankAttribute> actual = getInvestigatedFrankAttributes();
		Assert.assertTrue(actual.containsKey(attributeName));
		Assert.assertEquals(attributeName, actual.get(attributeName).getName());
		return actual.get(attributeName);
	}

	/**
	 * 
	 * @return The FrankAttributes that ModelBuilder can produce from a Class<?>. Note
	 * that the describingElement is not set, because we are not testing here in relation
	 * to creating the enclosing FrankElement of the attributes.
	 */
	private static Map<String, FrankAttribute> getInvestigatedFrankAttributes() {
		final List<FrankAttribute> attributes = ModelBuilder.createAttributes(
				null,
				InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.reflect.FrankAttributeTarget")
					.getDeclaredMethods());
		return attributes.stream().collect(Collectors.toMap(att -> att.getName(), att -> att));
	}
	
	@Test
	public void whenSetterAndIsThenAttribute() {
		checkAttributeCreated("attributeSetterIs");
	}

	@Test
	public void whenOnlySetterThenNotAttribute() {
		checkAttributeOmitted("noAttributeOnlySetter");
	}

	private void checkAttributeOmitted(String attributeName) {
		Map<String, FrankAttribute> actual = getInvestigatedFrankAttributes();
		Assert.assertFalse(actual.containsKey(attributeName));
	}
	
	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() {
		checkAttributeOmitted("noAttributeComplexType");
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() {
		Assert.assertFalse(getAttributeNameMap("get").containsKey("Prefix"));
	}

	Map<String, String> getAttributeNameMap(String prefix) {
		return 	ModelBuilder.getAttributeToMethodNameMap(
				InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.reflect.FrankAttributeTarget")
					.getDeclaredMethods(),
				prefix);
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
		FrankAttribute actual = checkAttributeCreated("ibisDockedOnlyDescription");
		Assert.assertEquals(Integer.MAX_VALUE, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOnlyDescription", actual.getDescription());
		Assert.assertNull(actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescription() {
		FrankAttribute actual = checkAttributeCreated("ibisDockedOrderDescription");
		Assert.assertEquals(3, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOrderDescription", actual.getDescription());
		Assert.assertNull(actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDescriptionDefault() {
		FrankAttribute actual = checkAttributeCreated("ibisDockedDescriptionDefault");
		Assert.assertEquals(Integer.MAX_VALUE, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedDescriptionDefault", actual.getDescription());
		Assert.assertEquals("Default of ibisDockedDescriptionDefault", actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedOrderDescriptionDefault() {
		FrankAttribute actual = checkAttributeCreated("ibisDockedOrderDescriptionDefault");
		Assert.assertEquals(5, actual.getOrder());
		Assert.assertEquals("Description of ibisDockedOrderDescriptionDefault", actual.getDescription());
		Assert.assertEquals("Default of ibisDockedOrderDescriptionDefault", actual.getDefaultValue());
		Assert.assertFalse(actual.isDeprecated());
	}

	@Test
	public void testIbisDockedDeprecated() {
		FrankAttribute actual = checkAttributeCreated("ibisDockedDeprecated");
		Assert.assertEquals("Description of ibisDockedDeprecated", actual.getDescription());
		Assert.assertTrue(actual.isDeprecated());
	}
}