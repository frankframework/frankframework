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
package nl.nn.adapterframework.frankdoc;

import static nl.nn.adapterframework.frankdoc.Utils.isConfigChildSetter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;
 
public class UtilsTest {
	private static final String SIMPLE = "nl.nn.adapterframework.frankdoc.testtarget.simple";
	private FrankClassRepository repository;

	@Before
	public void setUp() {
		repository = TestUtil.getFrankClassRepositoryDoclet(SIMPLE);
	}

	@Test
	public void testGetSpringBeans() throws FrankDocException {
		List<FrankClass> actual = repository.findClass(SIMPLE + ".IListener").getInterfaceImplementations();
		Collections.sort(actual, Comparator.comparing(FrankClass::getName));
		assertEquals(4, actual.size());
		Iterator<FrankClass> it = actual.iterator();
		FrankClass first = it.next();
		assertEquals(SIMPLE + ".ListenerChild", first.getName());
		FrankClass second = it.next();
		assertEquals(SIMPLE + ".ListenerGrandChild", second.getName());
		FrankClass third = it.next();
		assertEquals(SIMPLE + ".ListenerParent", third.getName());
	}

	@Test
	public void whenMethodIsConfigChildSetterThenRecognized() throws FrankDocException {
		assertTrue(isConfigChildSetter(getTestMethod("setListener")));
	}

	private FrankMethod getTestMethod(String name) throws FrankDocException {
		FrankClass listenerChildClass = repository.findClass(SIMPLE + ".ListenerChild");
		for(FrankMethod m: listenerChildClass.getDeclaredAndInheritedMethods()) {
			if(m.getName().equals(name)) {
				return m;
			}
		}
		throw new RuntimeException("No method in ListenerChild for method name: " + name);
	}

	@Test
	public void whenAttributeSetterThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("setChildAttribute")));
	}

	@Test
	public void whenMethodHasTwoArgsThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterTwoArgs")));
	}

	@Test
	public void whenMethodReturnsPrimitiveThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsInt")));
	}

	@Test
	public void whenMethodReturnsStringThenNotConfigChildSetter()  throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("invalidConfigChildSetterReturnsString")));
	}

	@Test
	public void whenMethodStartsWithRegisterAndTakesStringThenTextConfigChildSetter() throws FrankDocException {
		assertTrue(isConfigChildSetter(getTestMethod("registerTextConfigChild")));
	}

	@Test
	public void whenMethodStartsWithSetAndTakesStringThenNotConfigChildSetter() throws FrankDocException {
		assertFalse(isConfigChildSetter(getTestMethod("setNotTextConfigChildButAttribute")));
	}
}