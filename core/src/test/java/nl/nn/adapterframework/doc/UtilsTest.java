package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.Utils.isConfigChildSetter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import nl.nn.adapterframework.doc.objects.SpringBean;
 
public class UtilsTest {
	private static final String SIMPLE = "nl.nn.adapterframework.doc.testtarget.simple";

	@Test
	public void testGetSpringBeans() throws ReflectiveOperationException {
		List<SpringBean> actual = Utils.getSpringBeans(SIMPLE + ".IListener");
		actual.sort((b1, b2) -> b1.compareTo(b2));
		assertEquals(2, actual.size());
		for(SpringBean a: actual) {
			assertEquals(a.getClazz().getName(), a.getName());					
		}
		Iterator<SpringBean> it = actual.iterator();
		SpringBean first = it.next();
		assertEquals(SIMPLE + ".ListenerChild", first.getName());
		SpringBean second = it.next();
		assertEquals(SIMPLE + ".ListenerParent", second.getName());
	}

	@Test
	public void whenMethodIsConfigChildSetterThenRecognized() {
		assertTrue(isConfigChildSetter(getTestMethod("setListener")));
	}

	private Method getTestMethod(String name) {
		Class<?> listenerChildClass = Utils.getClass(SIMPLE + ".ListenerChild");
		for(Method m: listenerChildClass.getMethods()) {
			if(m.getName().equals(name)) {
				return m;
			}
		}
		throw new RuntimeException("No method in ListenerChild for method name: " + name);
	}

	@Test
	public void whenAttributeSetterThenNotConfigChildSetter() {
		assertFalse(isConfigChildSetter(getTestMethod("setChildAttribute")));
	}

	@Test
	public void whenMethodHasTwoArgsThenNotConfigChildSetter() {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterTwoArgs")));
	}

	@Test
	public void whenMethodReturnsPrimitiveThenNotConfigChildSetter() {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsInt")));
	}

	@Test
	public void whenMethodReturnsStringThenNotConfigChildSetter() {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsString")));
	}
}