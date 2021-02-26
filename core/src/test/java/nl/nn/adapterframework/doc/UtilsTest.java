/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/
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
		assertEquals(4, actual.size());
		for(SpringBean a: actual) {
			assertEquals(a.getClazz().getName(), a.getName());					
		}
		Iterator<SpringBean> it = actual.iterator();
		SpringBean first = it.next();
		assertEquals(SIMPLE + ".ListenerChild", first.getName());
		SpringBean second = it.next();
		assertEquals(SIMPLE + ".ListenerGrandChild", second.getName());
		SpringBean third = it.next();
		assertEquals(SIMPLE + ".ListenerParent", third.getName());
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