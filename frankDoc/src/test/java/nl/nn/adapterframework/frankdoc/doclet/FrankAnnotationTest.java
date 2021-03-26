package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
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
}
