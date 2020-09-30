package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder.AttributeSeed;
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
		System.out.println(actual.getMethods().values().stream()
				.map(m -> m.getName()).collect(Collectors.joining(", ")));
		Assert.assertEquals(2, actual.getMethods().size());
		Assert.assertTrue(actual.getMethodsWithInherited().size() >= 2);
		checkAttributeSeedsPresent(actual.getMethods(), "getParentAttribute", "setParentAttribute");
		checkAttributeSeedsPresent(actual.getMethodsWithInherited(), "getParentAttribute", "setParentAttribute");
		checkSameAttributeSeedWithAndWithoutInherited(actual, "getParentAttribute", "setParentAttribute");
		checkNameEqualsMapKey(actual, "getParentAttribute", "setParentAttribute");
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
		add(attributeSeeds, "getParentAttribute");
		add(attributeSeeds, "setParentAttribute");
		seed.setMethods(attributeSeeds);
		seed.setMethodsWithInherited(attributeSeeds);
		return seed;
	}

	private void add(Map<String, AttributeSeed> attributeSeeds, String name) {
		attributeSeeds.put(name, new AttributeSeed(name));
	}

	private ModelBuilder.ElementSeed getElementSeedChild() {
		ModelBuilder.ElementSeed seed = new ModelBuilder.ElementSeed("mypackage.Child");
		seed.setSimpleName("Child");
		Map<String, ModelBuilder.AttributeSeed> notInherited = new HashMap<>();
		add(notInherited, "getChildAttribute");
		add(notInherited, "setChildAttribute");
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
		Assert.assertEquals("ParentAttribute", actualParentAttribute.getName());
		FrankElement actualChild = actualGroup.getElements().get("mypackage.Child");
		Assert.assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		Assert.assertEquals("ChildAttribute", actualChildAttribute.getName());
	}
}