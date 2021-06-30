package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class FrankClassDocletTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.";

	private final String EXPECTED_CLASSDOC =
			"This is test class \"Child\". We use this comment to see how\n" + 
			" JavaDoc text is treated by the Doclet API.";

	private FrankClass instance;

	@Before
	public void setUp() throws FrankDocException {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		instance = repository.findClass(PACKAGE + "Child");
	}

	@Test
	public void testGetJavaDoc() {
		assertEquals(EXPECTED_CLASSDOC, instance.getJavaDoc());
	}

	@Test
	public void testMethodSequenceIsPreserved() {
		List<String> actualMethodNames = Arrays.asList(instance.getDeclaredMethods()).stream()
				.map(FrankMethod::getName)
				.collect(Collectors.toList());
		String[] expectedMethodNames = new String[] {"setInherited", "setVarargMethod", "getMyInnerEnum", "myAnnotatedMethod", "methodWithoutAnnotations"};
		assertArrayEquals(expectedMethodNames, actualMethodNames.toArray(new String[] {}));
	}
}
