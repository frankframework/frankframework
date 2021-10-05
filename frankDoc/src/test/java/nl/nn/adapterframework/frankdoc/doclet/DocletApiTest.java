package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;

public class DocletApiTest {
	private static ClassDoc fieldOwner;

	@BeforeClass
	public static void setUp() {
		ClassDoc[] classes = TestUtil.getClassDocs("nl.nn.adapterframework.javadoc.test");
		assertEquals(1, classes.length);
		fieldOwner = classes[0];
	}

	@Test
	public void whenFieldIsFinalAndGetsValueImmediatelyThenValueAvailable() {
		FieldDoc field = getField("finalFieldDirectlyInitialized");
		assertEquals("finalFieldDirectlyInitialized the value", field.constantValue());
	}

	@Test
	public void whenFieldIsFinalAndGetsValueInConstructorThenValueNotAvailable() {
		FieldDoc field = getField("finalFieldInitializedInConstructor");
		assertNull(field.constantValue());
	}

	@Test
	public void whenFieldIsNotFinalThenValueNotAvailable() {
		FieldDoc field = getField("nonFinalFieldInitialized");
		assertNull(field.constantValue());
	}

	FieldDoc getField(String name) {
		FieldDoc[] fields = fieldOwner.fields();
		for(FieldDoc field: fields) {
			if(field.name().equals(name)) {
				return field;
			}
		}
		throw new IllegalArgumentException(String.format("Class [%s] does not have field [%s]", fieldOwner.name(), name));
	}
}
