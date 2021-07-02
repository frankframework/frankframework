package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.frankdoc.testtarget.doclet.Java5Annotation;

public class FrankAnnotationTest extends TestBase{
	@Test
	public void whenArrayAnnotationValueProvidedAsScalarThenStillFetchable() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Parent");
		FrankMethod setter = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertEquals("setInherited", setter.getName());
		FrankAnnotation[] annotations = setter.getAnnotations();
		assertEquals(1, annotations.length);
		FrankAnnotation annotation = annotations[0];
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
		Object rawValue = annotation.getValue();
		String[] value = (String[]) rawValue;
		assertArrayEquals(new String[] {"50"}, value);
	}

	@Test
	public void whenArrayAnnotaionValueProvidedAsArrayThenFetchable() throws FrankDocException {
		FrankClass clazz = classRepository.findClass(PACKAGE + "DeprecatedChild");
		FrankMethod setter = TestUtil.getDeclaredMethodOf(clazz, "someSetter");
		assertEquals("someSetter", setter.getName());
		FrankAnnotation annotation = setter.getAnnotation(FrankDocletConstants.IBISDOC);
		assertEquals(FrankDocletConstants.IBISDOC, annotation.getName());
		assertArrayEquals(new String[] {"100", "Some description", "0"}, (String[]) annotation.getValue());
	}

	@Test
	public void whenAnnotationHasFieldNotNamedValueThenStillReadable() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Parent");
		FrankAnnotation annotation = clazz.getAnnotation(Java5Annotation.class.getName());
		assertNotNull(annotation);
		Object stringArrayRawValue = annotation.getValueOf("myStringArray");
		String[] stringArrayValue = (String[]) stringArrayRawValue;
		assertArrayEquals(new String[] {"first", "second"}, stringArrayValue);
	}

	@Test
	public void whenAnnotationHasStringFieldThenReadable() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "Parent");
		FrankAnnotation annotation = clazz.getAnnotation(Java5Annotation.class.getName());
		assertNotNull(annotation);
		Object stringRawValue = annotation.getValueOf("myString");
		String stringValue = (String) stringRawValue;
		assertEquals("A string", stringValue);
	}
}
