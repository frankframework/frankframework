package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FrankAnnotationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> result = new ArrayList<>();
		Arrays.asList(Environment.values()).forEach(v -> result.add(new Object[] {v}));
		return result;
	}

	@Parameter
	public Environment environment;

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = environment.getRepository(PACKAGE);
	}

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
