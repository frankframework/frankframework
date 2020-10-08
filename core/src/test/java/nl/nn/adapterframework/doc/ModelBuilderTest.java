package nl.nn.adapterframework.doc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		builder.addElementsToGroup(ModelBuilder.getClass(CHILD), group);
		builder.addElementsToGroup(ModelBuilder.getClass(PARENT), group);
		checkModelAfterChildAndParentAdded(builder.getModel());
	}

	@Test
	public void whenParentElementAddedBeforeChildThenCorrectModel() {
		ModelBuilder builder = new ModelBuilder();
		FrankDocGroup group = builder.addGroup("Listeners");
		builder.addElementsToGroup(ModelBuilder.getClass(PARENT), group);
		builder.addElementsToGroup(ModelBuilder.getClass(CHILD), group);
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