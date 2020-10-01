package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder.AttributeSeed;
import nl.nn.adapterframework.doc.ModelBuilder.ElementSeed;
import nl.nn.adapterframework.doc.ModelBuilder.Type;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilderTest {
	private static final String SIMPLE = "nl.nn.adapterframework.doc.target.simple";

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
	public void testNonInheritedMethodsAreRepeatedInMethodsWithInherited() {
		Class<?> clazz = InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.simple.ListenerParent");
		ModelBuilder.ElementSeed actual = new ModelBuilder.ElementSeed(clazz);
		Assert.assertEquals(2, actual.getMethods().size());
		Assert.assertTrue(actual.getMethodsWithInherited().size() >= 2);
		checkAttributeSeedsPresent(actual.getMethods(), "getParentAttribute", "setParentAttribute");
		checkAttributeSeedsPresent(actual.getMethodsWithInherited(), "getParentAttribute", "setParentAttribute");
		checkSameAttributeSeedWithAndWithoutInherited(actual, "getParentAttribute", "setParentAttribute");
		checkNameEqualsMapKey(actual, "getParentAttribute", "setParentAttribute");
		checkAttributeSeedsAreStringGetter(actual.getMethods().get("getParentAttribute"));
		checkAttributeSeedsAreStringSetters(actual.getMethods().get("setParentAttribute"));
		Assert.assertEquals("nl.nn.adapterframework.doc.target.simple.ListenerParent", actual.getFullName());
		Assert.assertEquals("ListenerParent", actual.getSimpleName());
	}

	private void checkAttributeSeedsPresent(final Map<String, AttributeSeed> attributeSeeds, String ...expectedItems) {
		for(String expectedItem: expectedItems) {
			Assert.assertEquals(expectedItem, attributeSeeds.containsKey(expectedItem) ? expectedItem : null);
		}
	}

	private void checkSameAttributeSeedWithAndWithoutInherited(ModelBuilder.ElementSeed actual, String ...methodNamesToCheck) {
		for(String methodName: methodNamesToCheck) {
			Assert.assertSame("AttributeSeeds with and without inherited should be the same for methodName: " + methodName,
					actual.getMethods().get(methodName),
					actual.getMethodsWithInherited().get(methodName));
		}
	}

	private void checkNameEqualsMapKey(ModelBuilder.ElementSeed actual, String ...methodNamesToCheck) {
		for(String methodName: methodNamesToCheck) {
			Assert.assertEquals(methodName, actual.getMethods().get(methodName).getName());
		}
	}

	private void checkAttributeSeedsAreStringGetter(ModelBuilder.AttributeSeed ...attributeSeeds) {
		for(AttributeSeed a: attributeSeeds) {
			Assert.assertEquals(0, a.getArgumentTypes().size());
			Assert.assertEquals(false, a.getReturnType().isPrimitive());
			Assert.assertEquals("java.lang.String", a.getReturnType().getName());
		}
	}

	private void checkAttributeSeedsAreStringSetters(ModelBuilder.AttributeSeed ...attributeSeeds) {
		for(AttributeSeed a: attributeSeeds) {
			Assert.assertEquals(1, a.getArgumentTypes().size());
			ModelBuilder.Type argType = a.getArgumentTypes().get(0);
			Assert.assertEquals(false, argType.isPrimitive());
			Assert.assertEquals("java.lang.String", argType.getName());
			Assert.assertNotNull(a.getReturnType());
			Assert.assertEquals(true, a.getReturnType().isPrimitive());
			Assert.assertEquals("void", a.getReturnType().getName());
		}
	}

	@Test
	public void testInhieritedMethodsAreOnlyInMethodsWithInherited() {
		Class<?> clazz = InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.simple.ListenerChild");
		ModelBuilder.ElementSeed actual = new ModelBuilder.ElementSeed(clazz);
		Assert.assertEquals(actual.getMethods().size(), 2);
		Assert.assertTrue(actual.getMethodsWithInherited().size() >= 4);
		checkAttributeSeedsPresent(actual.getMethods(), "getChildAttribute", "setChildAttribute");
		checkAttributeSeedsPresent(actual.getMethodsWithInherited(),
				"getParentAttribute", "setParentAttribute", "getChildAttribute", "setChildAttribute");
		checkSameAttributeSeedWithAndWithoutInherited(actual, "getChildAttribute", "setChildAttribute");
		checkNameEqualsMapKey(actual, "getChildAttribute", "setChildAttribute");
	}

	@Test
	public void testParseJavaMethodWithPrimitiveTypes() {
		Class<?> clazz = InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.reflect.ReflectTarget");
		ModelBuilder.ElementSeed elementSeed = new ModelBuilder.ElementSeed(clazz);
		Assert.assertTrue(elementSeed.getMethods().containsKey("methodWithPrimitiveTypes"));
		AttributeSeed actual = elementSeed.getMethods().get("methodWithPrimitiveTypes");
		ModelBuilder.Type actualReturnType = actual.getReturnType();
		Assert.assertEquals(true, actualReturnType.isPrimitive());
		Assert.assertEquals("int", actualReturnType.getName());
		List<ModelBuilder.Type> argumentTypes = actual.getArgumentTypes();
		Assert.assertEquals(2, argumentTypes.size());
		Assert.assertEquals(true, argumentTypes.get(0).isPrimitive());
		Assert.assertEquals("boolean", argumentTypes.get(0).getName());
		Assert.assertEquals(true, argumentTypes.get(1).isPrimitive());
		Assert.assertEquals("long", argumentTypes.get(1).getName());
	}

	@Test
	public void testGetSelfAndAncestorSeeds() {
		Class<?> clazz = InfoBuilderSource.getClass("nl.nn.adapterframework.doc.target.simple.ListenerChild");
		List<ModelBuilder.ElementSeed> actual = ModelBuilder.getSelfAndAncestorSeeds(clazz);
		Assert.assertEquals(3, actual.size());
		Assert.assertEquals("nl.nn.adapterframework.doc.target.simple.ListenerChild", actual.get(0).getFullName());
		Assert.assertEquals("nl.nn.adapterframework.doc.target.simple.ListenerParent", actual.get(1).getFullName());
		Assert.assertEquals("java.lang.Object", actual.get(2).getFullName());
	}

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.addGroup("Listeners");
		builder.addElementsToGroup(getSeedsForChild(), group);
		builder.addElementsToGroup(getSeedsForParent(), group);
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.addGroup("Listeners");
		builder.addElementsToGroup(getSeedsForParent(), group);
		builder.addElementsToGroup(getSeedsForChild(), group);
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	private List<ModelBuilder.ElementSeed> getSeedsForParent() {
		List<ModelBuilder.ElementSeed> result = new ArrayList<>();
		result.add(getElementSeedParent());
		result.add(getElementSeedObject());
		return result;
	}

	private ModelBuilder.ElementSeed getElementSeedObject() {
		ModelBuilder.ElementSeed seed = new ModelBuilder.ElementSeed("java.lang.Object");
		seed.setSimpleName("Object");
		seed.setMethods(new HashMap<>());
		seed.setMethodsWithInherited(new HashMap<>());
		return seed;
	}

	private ModelBuilder.ElementSeed getElementSeedParent() {
		ModelBuilder.ElementSeed seed = new ModelBuilder.ElementSeed("mypackage.Parent");
		seed.setSimpleName("Parent");
		Map<String, ModelBuilder.AttributeSeed> attributeSeeds = new HashMap<>();
		add(attributeSeeds, "getParentAttribute", ModelBuilderTest::makeGetter);
		add(attributeSeeds, "setParentAttribute", ModelBuilderTest::makeSetter);
		seed.setMethods(attributeSeeds);
		seed.setMethodsWithInherited(attributeSeeds);
		return seed;
	}

	private void add(Map<String, AttributeSeed> attributeSeeds, String name, Function<AttributeSeed, AttributeSeed> modifier) {
		AttributeSeed attributeSeed = new AttributeSeed(name);
		modifier.apply(attributeSeed);
		attributeSeeds.put(name, attributeSeed);
	}

	private static AttributeSeed makeSetter(AttributeSeed target) {
		target.setArgumentTypes(Arrays.asList(new Type[] {
				Type.typeString()}));
		target.setReturnType(Type.typeVoid());
		return target;
	}

	private static AttributeSeed makeGetter(AttributeSeed target) {
		target.setArgumentTypes(new ArrayList<>());
		target.setReturnType(Type.typeString());
		return target;
	}

	private ModelBuilder.ElementSeed getElementSeedChild() {
		ModelBuilder.ElementSeed seed = new ModelBuilder.ElementSeed("mypackage.Child");
		seed.setSimpleName("Child");
		Map<String, ModelBuilder.AttributeSeed> notInherited = new HashMap<>();
		add(notInherited, "getChildAttribute", ModelBuilderTest::makeGetter);
		add(notInherited, "setChildAttribute", ModelBuilderTest::makeSetter);
		seed.setMethods(notInherited);
		Map<String, ModelBuilder.AttributeSeed> inherited = new HashMap<>();
		inherited.putAll(notInherited);
		inherited.putAll(getElementSeedParent().getMethods());
		seed.setMethodsWithInherited(inherited);
		return seed;
	}

	private List<ModelBuilder.ElementSeed> getSeedsForChild() {
		List<ModelBuilder.ElementSeed> result = new ArrayList<>();
		result.add(getElementSeedChild());
		result.addAll(getSeedsForParent());
		return result;
	}

	private void checkModelAfterChildAndParentAdded(FrankDocModel model) {
		Assert.assertEquals(1, model.getGroups().size());
		FrankDocGroup actualGroup = model.getGroups().get(0);
		Map<String, FrankElement> actualAllElements = model.getAllElements();
		Assert.assertTrue(actualGroup.getElements().containsKey("mypackage.Parent"));
		Assert.assertTrue(actualGroup.getElements().containsKey("mypackage.Child"));
		for(String elementInGroup: actualGroup.getElements().keySet()) {
			FrankElement groupElement = actualGroup.getElements().get(elementInGroup);
			FrankElement allElement = actualAllElements.get(elementInGroup);
			Assert.assertSame(
					"Different objects for group element and all element for element name: " + elementInGroup,
					groupElement, allElement);
		}
		FrankElement actualParent = actualGroup.getElements().get("mypackage.Parent");
		Assert.assertEquals("mypackage.Parent", actualParent.getFullName());
		Assert.assertEquals("Parent", actualParent.getSimpleName());
		Assert.assertEquals(1, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = actualParent.getAttributes().get(0);
		Assert.assertEquals("parentAttribute", actualParentAttribute.getName());
		FrankElement actualChild = actualGroup.getElements().get("mypackage.Child");
		Assert.assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		Assert.assertEquals("childAttribute", actualChildAttribute.getName());
	}

	@Test
	public void whenSetterAndIsThenAttribute() {
		ElementSeed elementSeed = createElementSeed("mypackage.SomeClass", 
				makeSetter(new ModelBuilder.AttributeSeed("setAttribute")),
				makeIsser(new ModelBuilder.AttributeSeed("isAttribute")));
		List<FrankAttribute> actual = ModelBuilder.createAttributes(elementSeed);
		Assert.assertEquals(1, actual.size());
		FrankAttribute actualAttribute = actual.get(0);
		Assert.assertEquals("attribute", actualAttribute.getName());
	}

	@Test
	public void whenOnlySetterThenNotAttribute() {
		ElementSeed elementSeed = createElementSeed("mypackage.SomeClass", 
				makeSetter(new ModelBuilder.AttributeSeed("setAttribute")));
		List<FrankAttribute> actual = ModelBuilder.createAttributes(elementSeed);
		Assert.assertEquals(0, actual.size());
	}

	private static ElementSeed createElementSeed(String className, AttributeSeed ...attributeSeeds) {
		ModelBuilder.ElementSeed result = new ModelBuilder.ElementSeed(className);
		Map<String, ModelBuilder.AttributeSeed> attributeSeedMap = new HashMap<>();
		for(AttributeSeed a: attributeSeeds) {
			attributeSeedMap.put(a.getName(), a);
		}
		result.setMethods(attributeSeedMap);
		result.setMethodsWithInherited(attributeSeedMap);
		return result;
	}

	private static AttributeSeed makeIsser(AttributeSeed target) {
		target.setArgumentTypes(new ArrayList<>());
		target.setReturnType(Type.typeBoolean());
		return target;
	}

	@Test
	public void whenMethodsHaveWrongTypeThenNoAttribute() {
		ElementSeed elementSeed = createElementSeed("mypackage.SomeClass",
				modifySeedToSetter(new AttributeSeed("setAttribute"), typeList()),
				modifySeedToGetter(new AttributeSeed("getAttribute"), typeList()));
		List<FrankAttribute> actual = ModelBuilder.createAttributes(elementSeed);
		Assert.assertEquals(0, actual.size());
	}

	private static AttributeSeed modifySeedToSetter(AttributeSeed target, Type theType) {
		target.setArgumentTypes(Arrays.asList(new Type[] {theType}));
		target.setReturnType(Type.typeVoid());
		return target;
	}

	private static AttributeSeed modifySeedToGetter(AttributeSeed target, Type theType) {
		target.setArgumentTypes(new ArrayList<>());
		target.setReturnType(theType);
		return target;
	}

	private static Type typeList() {
		Type result = new Type();
		result.setPrimitive(false);
		result.setName("java.util.List");
		return result;
	}

	@Test
	public void whenAttributeNameMissesPrefixThenFilteredOutOfAttributes() {
		Map<String, AttributeSeed> in = new HashMap<>();
		add(in, "otherAttribute", ModelBuilderTest::makeSetter);
		Map<String, String> actual = ModelBuilder.getAttributeToMethodNameMap(in, "set");
		Assert.assertEquals(0, actual.size());
	}

	@Test
	public void whenAttributeNameEqualsPrefixThenFilteredOutOfAttributes() {
		Map<String, AttributeSeed> in = new HashMap<>();
		add(in, "set", ModelBuilderTest::makeSetter);
		Map<String, String> actual = ModelBuilder.getAttributeToMethodNameMap(in, "set");
		Assert.assertEquals(0, actual.size());		
	}

	@Test
	public void whenAttributeNameOkThenNotFilteredOut() {
		Map<String, AttributeSeed> in = new HashMap<>();
		add(in, "setX", ModelBuilderTest::makeSetter);
		Map<String, String> actual = ModelBuilder.getAttributeToMethodNameMap(in, "set");
		Assert.assertEquals(1, actual.size());		
	}
}