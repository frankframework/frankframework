package nl.nn.adapterframework.doc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder;

public class FrankDocGroupTest {
	public static final String SIMPLE = "nl.nn.adapterframework.doc.testtarget.simple";
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
		assertEquals(1, model.getGroups().size());
		FrankDocGroup actualGroup = model.getGroups().get(0);
		Map<String, FrankElement> actualAllElements = model.getAllElements();
		assertTrue(actualGroup.getElements().containsKey(PARENT));
		assertTrue(actualGroup.getElements().containsKey(CHILD));
		for(String elementInGroup: actualGroup.getElements().keySet()) {
			FrankElement groupElement = actualGroup.getElements().get(elementInGroup);
			FrankElement allElement = actualAllElements.get(elementInGroup);
			assertSame(
					"Different objects for group element and all element for element name: " + elementInGroup,
					groupElement, allElement);
		}
		FrankElement actualObject = model.getAllElements().get("java.lang.Object");
		assertNull(actualObject.getParent());
		FrankElement actualParent = actualGroup.getElements().get(PARENT);
		assertSame(actualObject, actualParent.getParent());
		assertEquals(PARENT, actualParent.getFullName());
		assertEquals("ListenerParent", actualParent.getSimpleName());
		assertEquals(1, actualParent.getAttributes().size());
		FrankAttribute actualParentAttribute = actualParent.getAttributes().get(0);
		assertEquals("parentAttribute", actualParentAttribute.getName());
		assertSame(actualParent, actualParentAttribute.getDescribingElement());
		FrankElement actualChild = actualGroup.getElements().get(CHILD);
		assertSame(actualParent, actualChild.getParent());
		assertEquals(CHILD, actualChild.getFullName());
		assertEquals("ListenerChild", actualChild.getSimpleName());
		assertEquals(1, actualChild.getAttributes().size());
		FrankAttribute actualChildAttribute = actualChild.getAttributes().get(0);
		assertEquals("childAttribute", actualChildAttribute.getName());
		assertSame(actualChild, actualChildAttribute.getDescribingElement());
	}
}
