package nl.nn.adapterframework.doc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class FrankMethodReflectTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.doclet.";
	private static final String STRING = "java.lang.String";

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = FrankClassRepository.getReflectInstance();
	}

	@Test
	public void testMethod() throws DocletReflectiveOperationException {
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
		assertEquals(STRING, parameter.getName());
		annotation = setter.getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
	}

	@Test
	public void whenMethodIsPackagePrivateThenNotPublic() throws DocletReflectiveOperationException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "packagePrivateMethod");
		assertNotNull(method);
		assertFalse(method.isPublic());
	}

	@Test
	public void whenNoAnnotationsThenNullReturned() throws DocletReflectiveOperationException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "packagePrivateMethod");
		assertEquals(0, method.getAnnotations().length);
		assertNull(method.getAnnotation(FrankDocletConstants.IBISDOC));
	}

	@Test
	public void whenInheritedAnnotationsRequestedThenInheritedAnnotationsIncluded() throws DocletReflectiveOperationException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Child");
		FrankMethod method = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertNotNull(method);
		FrankAnnotation annotation = method.getAnnotation(FrankDocletConstants.IBISDOC);
		assertNull(annotation);
		annotation = method.getAnnotationInludingInherited(FrankDocletConstants.IBISDOC);
		assertNotNull(annotation);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
	}
}
