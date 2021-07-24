package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

public class FrankMethodTest extends TestBase {
	@Test
	public void testMethod() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Parent");
		FrankMethod setter = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertEquals("setInherited", setter.getName());
		assertTrue(setter.isPublic());
		FrankAnnotation[] annotations = setter.getAnnotations();
		assertEquals(1, annotations.length);
		FrankAnnotation annotation = annotations[0];
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
		annotation = setter.getAnnotation(FrankDocletConstants.IBISDOC);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
		FrankType returnType = setter.getReturnType();
		assertNotNull(returnType);
		assertTrue(returnType.isPrimitive());
		assertEquals("void", returnType.getName());
		assertEquals(1, setter.getParameterCount());
		FrankType[] parameters = setter.getParameterTypes();
		assertEquals(1, parameters.length);
		FrankType parameter = parameters[0];
		assertFalse(parameter.isPrimitive());
		assertEquals(FrankDocletConstants.STRING, parameter.getName());
		annotation = setter.getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
	}

	@Test
	public void whenMethodIsPackagePrivateThenNotFound() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "packagePrivateMethod");
		assertNull(method);
	}

	@Test
	public void whenNoAnnotationsThenNullReturned() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "methodWithoutAnnotations");
		assertEquals(0, method.getAnnotations().length);
		assertNull(method.getAnnotation(FrankDocletConstants.IBISDOC));
	}

	@Test
	public void whenInheritedAnnotationsRequestedThenInheritedAnnotationsIncluded() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertNotNull(method);
		FrankAnnotation annotation = method.getAnnotation(FrankDocletConstants.IBISDOC);
		assertNull(annotation);
		annotation = method.getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
	}

	@Test
	public void overriddenMethodHasOverriderAsDeclaringClass() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod setter = getMethodFromDeclaredAndInheritedMethods(clazz, "setInherited");
		assertEquals("Child", setter.getDeclaringClass().getSimpleName());
	}

	@Test
	public void inheritedMethodHasAncestorAsDeclaringClass() throws Exception{
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod getter = getMethodFromDeclaredAndInheritedMethods(clazz, "getInherited");
		assertEquals("Parent", getter.getDeclaringClass().getSimpleName());
	}

	private FrankMethod getMethodFromDeclaredAndInheritedMethods(FrankClass clazz, String methodName) {
		FrankMethod[] methods = clazz.getDeclaredAndInheritedMethods();
		List<FrankMethod> getters = Arrays.asList(methods).stream()
				.filter(m -> m.getName().equals(methodName))
				.collect(Collectors.toList());
		assertEquals(1, getters.size());
		return getters.get(0);
	}

	@Test
	public void whenMethodHasVarargsThenIsVarargs() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "setVarargMethod");
		assertTrue(method.isVarargs());
		assertEquals(1, method.getParameterCount());
		FrankType parameter = method.getParameterTypes()[0];
		// We use the parameter type to check whether we have a setter or not.
		// If a method has a vararg argument, then it is not a setter.
		// In this case the exact type of the parameter is not needed.
		Set<String> stringOrStringArray = new HashSet<>(Arrays.asList(FrankDocletConstants.STRING, "[L" + FrankDocletConstants.STRING + ";"));
		assertTrue(stringOrStringArray.contains(parameter.getName()));
	}

	@Test
	public void whenMethodDoesNotHaveVarargsThenNotVarargs() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertFalse(method.isVarargs());
		assertEquals(1, method.getParameterCount());
		FrankType parameter = method.getParameterTypes()[0];
		assertEquals(FrankDocletConstants.STRING, parameter.getName());		
	}

	@Test
	public void annotationCanBeInheritedFromImplementedInterface() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "myAnnotatedMethod");
		FrankAnnotation annotation = method.getAnnotationInludingInherited(FrankDocletConstants.DEPRECATED);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.DEPRECATED, annotation.getName());
	}

	@Test
	public void testToString() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "myAnnotatedMethod");
		assertEquals("Child.myAnnotatedMethod", method.toString());
	}
}
