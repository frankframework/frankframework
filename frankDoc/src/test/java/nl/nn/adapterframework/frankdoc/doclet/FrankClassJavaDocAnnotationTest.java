package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FrankClassJavaDocAnnotationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.interfaces.classtag.";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"ClassWithJavaDocTag", "ClassGroup"},
			{"ClassWithoutJavaDocTag", null},
			{"InheriterFromParent", "ClassGroup"},
			{"ParentOverrider", "ParentOverriderGroup"},
			{"InheriterFromChildInterface", "InterfaceGroup"},
			{"InheriterFromGrandparent", "ClassGroup"}
		});
	}

	@Parameter(0)
	public String queriedClass;

	@Parameter(1)
	public String expectedJavaDocTagValue;

	@Test
	public void testJavaDocTagValue() throws Exception {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		FrankClass instance = repository.findClass(PACKAGE + queriedClass);
		// TODO: Will rename this method and give it the JavaDoc tag name as argument.
		// This has to be done for similar methods in FrankMethod as well.
		FrankAnnotation actualGroupAnnotation = instance.getGroupAnnotation();
		if(expectedJavaDocTagValue == null) {
			assertNull(actualGroupAnnotation);
		} else {
			assertEquals(expectedJavaDocTagValue, ((String[]) actualGroupAnnotation.getValue())[0]);
		}
	}
}
