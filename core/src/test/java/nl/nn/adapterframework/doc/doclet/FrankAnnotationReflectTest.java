package nl.nn.adapterframework.doc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.doc.Utils;

public class FrankAnnotationReflectTest {
	private static final String PACKAGE = "nl.nn.adapterframework.doc.testtarget.doclet.";
	private static final String IBISDOC = "nl.nn.adapterframework.doc.IbisDoc";

	@Test
	public void whenArrayAnnotationValueProvidedAsScalarThenStillFetchable() throws DocletReflectiveOperationException {
		FrankClass clazz = new FrankClassReflect(Utils.getClass(PACKAGE + "Parent"));
		FrankMethod setter = TestUtil.getDeclaredMethodOf(clazz, "setInherited");
		assertEquals("setInherited", setter.getName());
		FrankAnnotation[] annotations = setter.getAnnotations();
		assertEquals(1, annotations.length);
		FrankAnnotation annotation = annotations[0];
		assertEquals(IBISDOC, annotation.getName());
		Object rawValue = annotation.getValue();
		String[] value = (String[]) rawValue;
		assertArrayEquals(new String[] {"50"}, value);
	}

	@Test
	public void whenArrayAnnotaionValueProvidedAsArrayThenFetchable() throws DocletReflectiveOperationException {
		FrankClass clazz = new FrankClassReflect(Utils.getClass(PACKAGE + "DeprecatedChild"));
		FrankMethod setter = TestUtil.getDeclaredMethodOf(clazz, "someSetter");
		assertEquals("someSetter", setter.getName());
		FrankAnnotation annotation = setter.getAnnotation(IBISDOC);
		assertEquals(IBISDOC, annotation.getName());
		assertArrayEquals(new String[] {"100", "Some description", "0"}, (String[]) annotation.getValue());
	}
}
