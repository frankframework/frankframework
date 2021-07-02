package nl.nn.adapterframework.frankdoc.doclet;

import static nl.nn.adapterframework.frankdoc.doclet.TestUtil.JAVADOC_GROUP_ANNOTATION;
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
public class FrankClassJava5AnnotationTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.doclet.interfaces.java5.annotation.";

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"ClassWithJava5Annotation", "ClassGroup"},
			{"ClassWithoutJava5Annotation", null},
			{"InheriterFromParent", "ClassGroup"},
			{"ParentOverrider", "ParentOverriderGroup"},
			{"InheriterFromChildInterface", "InterfaceGroup"},
			{"InheriterFromGrandparent", "ClassGroup"}
		});
	}

	@Parameter(0)
	public String queriedClass;

	@Parameter(1)
	public String expectedValue;

	@Test
	public void testJavaAnnotationValue() throws Exception {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		FrankClass instance = repository.findClass(PACKAGE + queriedClass);
		// TODO: Will rename this method and give it Java annotation class name as argument.
		// This has to be done for similar methods in FrankMethod as well.
		FrankAnnotation actualGroupAnnotation = instance.getAnnotationIncludingInherited(JAVADOC_GROUP_ANNOTATION);
		if(expectedValue == null) {
			assertNull(actualGroupAnnotation);
		} else {
			assertEquals(expectedValue, (String) actualGroupAnnotation.getValueOf("name"));
		}
	}
}
