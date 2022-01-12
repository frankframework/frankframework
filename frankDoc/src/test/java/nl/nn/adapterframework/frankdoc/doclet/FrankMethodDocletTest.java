package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class FrankMethodDocletTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.";

	private FrankClassRepository repository;
	private FrankClass clazz;
	private FrankClass innerClass;

	@Before
	public void setUp() throws FrankDocException {
		repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		clazz = repository.findClass(PACKAGE + "Parent");
		innerClass = repository.findClass(PACKAGE + "Parent.InnerMyInterfaceImplementation");
	}

	@Test
	public void whenMethodHasJavadocThenReturnedByGetJavaDoc() {
		FrankMethod method = getMethodByName(clazz, "setInherited");
		assertEquals("This is the JavaDoc of method \"setInherited\".", method.getJavaDoc());
	}

	private FrankMethod getMethodByName(FrankClass c, String name) {
		return Arrays.asList(c.getDeclaredMethods()).stream()
				.filter(m -> m.getName().equals(name))
				.collect(Collectors.toList()).get(0);
	}

	@Test
	public void whenMethodHasJavadocThenReturnedByGetJavaDocIncludingInherited() throws FrankDocException {
		FrankMethod method = getMethodByName(clazz, "setInherited");
		assertEquals("This is the JavaDoc of method \"setInherited\".", method.getJavaDocIncludingInherited());		
	}

	@Test
	public void whenMethodInheritsJavadocThenNotReturnedByGetJavadoc() {
		FrankMethod method = getMethodByName(innerClass, "myAnnotatedMethod");
		assertNull(method.getJavaDoc());
	}

	@Test
	public void whenMethodInheritsJavadocThenReturnedByGetJavadocIncludingInherited() throws FrankDocException {
		FrankMethod method = getMethodByName(innerClass, "myAnnotatedMethod");
		assertEquals("This is the javadoc of \"myAnnotatedMethod\".", method.getJavaDocIncludingInherited());		
	}

	@Test
	public void whenMethodHasDefaultThenReturnedByGetDefaultValueFromJavadoc() {
		FrankMethod method = getMethodByName(clazz, "setInherited");
		assertEquals("DefaultValue", method.getJavaDocTag(TestUtil.JAVADOC_DEFAULT_VALUE_TAG));
	}

	@Test
	public void whenMethodInheritsDefaultThenReturnedByGetDefaultValueFromJavadocIncludingInherited() throws FrankDocException {
		FrankMethod method = getMethodByName(innerClass, "myAnnotatedMethod");
		assertEquals("InheritedDefault", method.getJavaDocTagIncludingInherited(TestUtil.JAVADOC_DEFAULT_VALUE_TAG));		
	}

	@Test
	public void whenMethodHasJavaDocTagWithoutArgumentThenEmptyStringReturned() {
		FrankMethod method = getMethodByName(clazz, "myMethod");
		String actual = method.getJavaDocTag(TestUtil.JAVADOC_DEFAULT_VALUE_TAG);
		assertEquals("", actual);
	}
}
