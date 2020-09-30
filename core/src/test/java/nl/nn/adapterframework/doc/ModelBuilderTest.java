package nl.nn.adapterframework.doc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.ModelBuilder.AttributeSeed;
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
		Assert.assertEquals(actual.getMethods().size(), 2);
		Assert.assertTrue(actual.getMethodsWithInherited().size() >= 2);
		checkAttributeSeedsPresent(actual.getMethods(), "getParentAttribute", "setParentAttribute");
		checkAttributeSeedsPresent(actual.getMethodsWithInherited(), "getParentAttribute", "setParentAttribute");
		checkSameAttributeSeedWithAndWithoutInherited(actual, "getParentAttribute", "setParentAttribute");
		checkNameEqualsMapKey(actual, "getParentAttribute", "setParentAttribute");
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
}