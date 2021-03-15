package nl.nn.adapterframework.doc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankClassReflectTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.doclet.";
	private static final String DEPRECATED = "java.lang.Deprecated";
	private static final String OBJECT = "java.lang.Object";

	@Test
	public void testChildClass() {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "Child"));
		assertEquals(PACKAGE + "Child", instance.getName());
		assertTrue(instance.isPublic());
		assertEquals(0, instance.getAnnotations().length);
		assertNull(instance.getAnnotation(DEPRECATED));
		assertFalse(instance.isAbstract());
		assertEquals("Child", instance.getSimpleName());
		assertEquals("Parent", instance.getSuperclass().getSimpleName());
		assertFalse(instance.isInterface());
	}

	@Test
	public void whenClassIsPackagePrivateThenNotPublic() {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "PackagePrivateClass"));
		assertFalse(instance.isPublic());
	}

	@Test
	public void testClassAnnotations() {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "DeprecatedChild"));
		FrankAnnotation[] annotations = instance.getAnnotations();
		assertEquals(1, annotations.length);
		FrankAnnotation annotation = annotations[0];
		assertEquals(DEPRECATED, annotation.getName());
		annotation = instance.getAnnotation(DEPRECATED);
		assertNotNull(annotation);
		assertEquals(DEPRECATED, annotation.getName());
	}

	@Test
	public void classObjectHasSuperclassNull() {
		FrankClass instance = new FrankClassReflect(Utils.getClass(OBJECT));
		assertNull(instance.getSuperclass());
	}

	@Test
	public void interfaceCanGiveItsImplementations() throws DocletReflectiveOperationException {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "MyInterface"));
		List<FrankClass> implementations = instance.getInterfaceImplementations();
		assertEquals(1, implementations.size());
		assertEquals("Child", implementations.get(0).getSimpleName());
	}

	@Test(expected = DocletReflectiveOperationException.class)
	public void nonInterfaceCannotGiveItsImplementations() throws DocletReflectiveOperationException {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "Child"));
		instance.getInterfaceImplementations();
	}

	@Test
	public void getDeclaredMethodsDoesNotIncludeInheritedMethods() {
		FrankClass instance = new FrankClassReflect(Utils.getClass(PACKAGE + "Child"));
		FrankMethod[] declaredMethods = instance.getDeclaredMethods();
		final Set<String> actualMethodNames = new TreeSet<>();
		Arrays.asList(declaredMethods).forEach(m -> actualMethodNames.add(m.getName()));
		List<String> sortedActualMethodNames = new ArrayList<>(actualMethodNames);
		sortedActualMethodNames = sortedActualMethodNames.stream().filter(name -> ! name.contains("jacoco")).collect(Collectors.toList());
		assertArrayEquals(new String[] {"packagePrivateMethod", "setInherited"}, sortedActualMethodNames.toArray());
	}
}
