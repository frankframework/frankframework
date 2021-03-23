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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class FrankClassReflectTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.doclet.";
	private static final String OBJECT = "java.lang.Object";

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = FrankClassRepository.getReflectInstance();
	}

	@Test
	public void testChildClass() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		assertEquals(PACKAGE + "Child", instance.getName());
		assertTrue(instance.isPublic());
		assertEquals(0, instance.getAnnotations().length);
		assertNull(instance.getAnnotation(FrankDocletConstants.DEPRECATED));
		assertFalse(instance.isAbstract());
		assertEquals("Child", instance.getSimpleName());
		assertEquals("Parent", instance.getSuperclass().getSimpleName());
		assertFalse(instance.isInterface());
	}

	@Test
	public void whenClassIsPackagePrivateThenNotPublic() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "PackagePrivateClass");
		assertFalse(instance.isPublic());
	}

	@Test
	public void testClassAnnotations() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "DeprecatedChild");
		FrankAnnotation[] annotations = instance.getAnnotations();
		assertEquals(1, annotations.length);
		FrankAnnotation annotation = annotations[0];
		assertEquals(FrankDocletConstants.DEPRECATED, annotation.getName());
		annotation = instance.getAnnotation(FrankDocletConstants.DEPRECATED);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.DEPRECATED, annotation.getName());
	}

	@Test
	public void classObjectHasSuperclassNull() throws FrankDocException {
		FrankClass instance = classRepository.findClass(OBJECT);
		assertNull(instance.getSuperclass());
	}

	@Test
	public void interfaceCanGiveItsImplementations() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterface");
		List<FrankClass> implementations = instance.getInterfaceImplementations();
		assertEquals(1, implementations.size());
		assertEquals("Child", implementations.get(0).getSimpleName());
	}

	@Test(expected = FrankDocException.class)
	public void nonInterfaceCannotGiveItsImplementations() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		instance.getInterfaceImplementations();
	}

	@Test
	public void getDeclaredMethodsDoesNotIncludeInheritedMethods() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		FrankMethod[] declaredMethods = instance.getDeclaredMethods();
		final Set<String> actualMethodNames = new TreeSet<>();
		Arrays.asList(declaredMethods).forEach(m -> actualMethodNames.add(m.getName()));
		List<String> sortedActualMethodNames = new ArrayList<>(actualMethodNames);
		sortedActualMethodNames = sortedActualMethodNames.stream().filter(name -> ! name.contains("jacoco")).collect(Collectors.toList());
		assertArrayEquals(new String[] {"packagePrivateMethod", "setInherited"}, sortedActualMethodNames.toArray());
	}

	/**
	 * The following is tested:
	 * <ul>
	 * <li> Inherited methods are included.
	 * <li> Overridden methods are not duplicated.
	 * <li> Only public methods are included.
	 * </ul>
	 */
	@Test
	public void testGetDeclaredAndInheritedMethods() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		FrankMethod[] methods = instance.getDeclaredAndInheritedMethods();
		final Set<String> methodNames = new TreeSet<>();
		Arrays.asList(methods).stream()
			.map(FrankMethod::getName)
			.filter(name -> ! name.contains("jacoco"))
			.forEach(name -> methodNames.add(name));
		assertArrayEquals(new String[] {"equals", "getClass", "getInherited", "hashCode", "notify", "notifyAll", "setInherited", "toString", "wait"}, new ArrayList<>(methodNames).toArray());
		// Test we have no duplicates
		Map<String, List<FrankMethod>> methodsByName = Arrays.asList(methods).stream()
				.filter(m -> methodNames.contains(m.getName()))
				.collect(Collectors.groupingBy(FrankMethod::getName));
		for(String name: new String[] {"getInherited", "setInherited"}) {
			assertEquals(String.format("Duplicate method name [%s]", name), 1, methodsByName.get(name).size());
		}
	}

	@Test
	public void whenInterfaceDoesNotExtendOthersThenGetInterfacesReturnsEmptyArray() throws Exception{
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterfaceParent");
		assertEquals(0, instance.getInterfaces().length);
	}

	@Test
	public void whenInterfaceExtendsOtherInterfaceThenReturnedByGetInterfaces() throws Exception {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterface");
		FrankClass[] implementedInterfaces = instance.getInterfaces();
		assertEquals(1, implementedInterfaces.length);
		assertEquals("MyInterfaceParent", implementedInterfaces[0].getSimpleName());
	}
}
