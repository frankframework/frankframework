package nl.nn.adapterframework.frankdoc.doclet;

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

import org.junit.Test;

public class FrankClassTest extends TestBase {
	private static final String JAVA_5_ANNOTATION = "nl.nn.adapterframework.frankdoc.testtarget.doclet.Java5Annotation";
	private static final String JAVADOC_TAG = "@ff.myTag";

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
		assertEquals(FrankClassRepository.removeTrailingDot(PACKAGE), instance.getPackageName());
		assertFalse(instance.isEnum());
	}

	@Test
	public void whenClassIsPackagePrivateThenNotFound() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "PackagePrivateClass");
		assertNull(instance);
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
		FrankClass instance = classRepository.findClass(FrankDocletConstants.OBJECT);
		assertNull(instance.getSuperclass());
	}

	@Test
	public void interfaceCanGiveItsImplementations() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterface");
		List<FrankClass> implementations = instance.getInterfaceImplementations();
		assertEquals(3, implementations.size());
		// We test abstract classes are omitted. With reflection and Spring
		// this happens automatically, so it should also work like that
		// within a doclet.
		//
		// We also test that inner classes are omitted as interface implementations.
		checkInterfaceImplementations(implementations);
	}

	private void checkInterfaceImplementations(List<FrankClass> implementations) {
		List<String> expectedImplementationNames = Arrays.asList(new String[] {"Child", "GrandChild", "GrandGrandChild"});
		List<String> actualImplementationNames = implementations.stream().map(FrankClass::getSimpleName).sorted().collect(Collectors.toList());
		assertArrayEquals(expectedImplementationNames.toArray(new String[] {}), actualImplementationNames.toArray(new String[] {}));
	}

	@Test
	public void superInterfaceHasImplementationsOfChildInterfaces() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterfaceParent");
		List<FrankClass> implementations = instance.getInterfaceImplementations();
		checkInterfaceImplementations(implementations);
	}

	@Test
	public void superSuperInterfaceHasImplementationsOfChildInterfaces() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterfaceGrandParent");
		List<FrankClass> implementations = instance.getInterfaceImplementations();
		checkInterfaceImplementations(implementations);
	}

	@Test(expected = FrankDocException.class)
	public void nonInterfaceCannotGiveItsImplementations() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		instance.getInterfaceImplementations();
	}

	@Test
	public void nonInterfaceCanGiveItsSuperInterfaces() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		FrankClass[] result = instance.getInterfaces();
		assertEquals(1, result.length);
		assertEquals("MyInterface", result[0].getSimpleName());
	}

	@Test
	public void getDeclaredMethodsDoesNotIncludeInheritedMethods() throws FrankDocException {
		FrankClass instance = classRepository.findClass(PACKAGE + "Child");
		FrankMethod[] declaredMethods = instance.getDeclaredMethods();
		final Set<String> actualMethodNames = new TreeSet<>();
		Arrays.asList(declaredMethods).forEach(m -> actualMethodNames.add(m.getName()));
		List<String> sortedActualMethodNames = new ArrayList<>(actualMethodNames);
		sortedActualMethodNames = sortedActualMethodNames.stream().filter(name -> ! name.contains("jacoco")).collect(Collectors.toList());
		assertArrayEquals(new String[] {"getMyInnerEnum", "methodWithoutAnnotations", "myAnnotatedMethod", "setInherited", "setVarargMethod"}, sortedActualMethodNames.toArray());
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
		assertArrayEquals(new String[] {"equals", "getClass", "getInherited", "getMyInnerEnum", "hashCode", "methodWithoutAnnotations", "myAnnotatedMethod", "myMethod", "notify", "notifyAll", "setInherited", "setVarargMethod", "toString", "wait"}, new ArrayList<>(methodNames).toArray());
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
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterfaceGrandParent");
		assertEquals(0, instance.getInterfaces().length);
	}

	@Test
	public void whenInterfaceExtendsOtherInterfaceThenReturnedByGetInterfaces() throws Exception {
		FrankClass instance = classRepository.findClass(PACKAGE + "MyInterface");
		FrankClass[] implementedInterfaces = instance.getInterfaces();
		assertEquals(1, implementedInterfaces.length);
		assertEquals("MyInterfaceParent", implementedInterfaces[0].getSimpleName());
	}

	@Test
	public void testGetEnumConstants() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "MyEnum");
		assertTrue(clazz.isEnum());
		FrankEnumConstant[] actual = clazz.getEnumConstants();
		String[] actualNames = Arrays.asList(actual).stream()
				.map(c -> c.getName())
				.collect(Collectors.toList()).toArray(new String[] {});
		assertArrayEquals(new String[] {"ONE", "TWO", "THREE"}, actualNames);
		FrankEnumConstant aConstant = actual[0];
		assertTrue(aConstant.isPublic());
		assertNull(aConstant.getAnnotation(JAVA_5_ANNOTATION));
		assertEquals("", aConstant.getJavaDoc());
	}

	@Test
	public void testGetEnumConstantsInnerEnum() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child" + ".MyInnerEnum");
		assertTrue(clazz.isEnum());
		FrankEnumConstant[] actual = clazz.getEnumConstants();
		String[] actualNames = Arrays.asList(actual).stream()
				.map(c -> c.getName())
				.collect(Collectors.toList()).toArray(new String[] {});
		assertArrayEquals(new String[] {"INNER_FIRST", "INNER_SECOND"}, actualNames);
		FrankEnumConstant aConstant = actual[0];
		assertTrue(aConstant.isPublic());
		assertNull(aConstant.getAnnotation(JAVA_5_ANNOTATION));
		assertEquals("", aConstant.getJavaDoc());
		aConstant = actual[1];
		assertEquals("Description of INNER_SECOND", aConstant.getJavaDoc());
		FrankAnnotation anAnnotation = aConstant.getAnnotation(JAVA_5_ANNOTATION);
		assertNotNull(anAnnotation);
		assertArrayEquals(new String[] {"a", "b"}, (String[]) anAnnotation.getValueOf("myStringArray"));
		assertEquals("s", anAnnotation.getValueOf("myString"));
		assertEquals(4, anAnnotation.getValueOf("myInt"));
		clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod enumGetter = TestUtil.getDeclaredMethodOf(clazz, "getMyInnerEnum");
		FrankType returnType = enumGetter.getReturnType();
		assertTrue(returnType.isEnum());
	}

	@Test
	public void simpleNameOfInnerClassDoesNotContainOuterClassName() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child" + ".MyInnerEnum");
		assertEquals("MyInnerEnum", clazz.getSimpleName());
	}

	@Test
	public void testToString() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child" + ".MyInnerEnum");
		// This method is just for logging. There is no point in ensuring we get a "." instead of a "$".
		String actual = clazz.toString().replace("$", ".");
		assertEquals(PACKAGE + "Child" + ".MyInnerEnum", actual);
	}

	@Test
	public void whenJavaDocTagHasArgumentThenTrimmedArgumentFound() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		String actual = clazz.getJavaDocTag(JAVADOC_TAG);
		assertEquals("This is the tag argument.", actual);
	}

	@Test
	public void whenJavaDocTagHasNoArgumentThenEmptyStringFound() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Parent");
		String actual = clazz.getJavaDocTag(JAVADOC_TAG);
		assertEquals("", actual);
	}

	@Test
	public void whenJavaDocTagIsMissingThenNullReturned() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "GrandChild");
		String actual = clazz.getJavaDocTag(JAVADOC_TAG);
		assertNull(actual);
	}
}
