package nl.nn.adapterframework.doc;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.FrankDocGroupTest;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilderTest {
	
	@Test
	public void testGetSpringBeans() {
		List<SpringBean> actual = ModelBuilder.getSpringBeans(FrankDocGroupTest.SIMPLE + ".IListener");
		actual.sort((b1, b2) -> b1.compareTo(b2));
		Assert.assertEquals(2, actual.size());
		for(SpringBean a: actual) {
			Assert.assertEquals(a.getClazz().getName(), a.getName());					
		}
		Iterator<SpringBean> it = actual.iterator();
		SpringBean first = it.next();
		Assert.assertEquals(FrankDocGroupTest.SIMPLE + ".ListenerChild", first.getName());
		SpringBean second = it.next();
		Assert.assertEquals(FrankDocGroupTest.SIMPLE + ".ListenerParent", second.getName());
	}
}