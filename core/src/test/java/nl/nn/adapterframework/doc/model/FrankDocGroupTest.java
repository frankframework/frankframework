package nl.nn.adapterframework.doc.model;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder;

public class FrankDocGroupTest {
	public static final String SIMPLE = "nl.nn.adapterframework.doc.target.simple";
	private static final String PARENT = SIMPLE + ".ListenerParent";
	private static final String CHILD = SIMPLE + ".ListenerChild";

	@Test
	public void whenChildElementAddedBeforeParentThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.getModel().addGroup("Listeners");
		group.addAsElement(ModelBuilder.getClass(CHILD), builder.getModel());
		group.addAsElement(ModelBuilder.getClass(PARENT), builder.getModel());
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.getModel().addGroup("Listeners");
		group.addAsElement(ModelBuilder.getClass(PARENT), builder.getModel());
		group.addAsElement(ModelBuilder.getClass(CHILD), builder.getModel());
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
		FrankElement actualObject = model.getAllElements().get("java.lang.Object");
		Assert.assertNull(actualObject.getParent());
		FrankElement actualParent = actualGroup.getElements().get(PARENT);
		Assert.assertSame(actualObject, actualParent.getParent());
		Assert.assertEquals(PARENT, actualParent.getFullName());
		Assert.assertEquals("ListenerParent", actualParent.getSimpleName());
		Assert.assertEquals(1, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = actualParent.getAttributes().get(0);
		Assert.assertEquals("parentAttribute", actualParentAttribute.getName());
		Assert.assertSame(actualParent, actualParentAttribute.getDescribingElement());
		FrankElement actualChild = actualGroup.getElements().get(CHILD);
		Assert.assertSame(actualParent, actualChild.getParent());
		Assert.assertEquals(CHILD, actualChild.getFullName());
		Assert.assertEquals("ListenerChild", actualChild.getSimpleName());
		Assert.assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		Assert.assertEquals("childAttribute", actualChildAttribute.getName());
		Assert.assertSame(actualChild, actualChildAttribute.getDescribingElement());
	}
}
