package nl.nn.adapterframework.frankdoc.doclet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class FrankClassAllTagsTest extends TestBase {
	@Test
	public void whenTagAppearsMultipleTimesThenAllFound() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "TestTags");
		List<String> tags = clazz.getAllJavaDocTagsOf("@second");
		assertArrayEquals(new String[] {"My second - first value", "My second - second value"}, tags.toArray(new String[] {}));
	}

	@Test
	public void whenTagAppearsOnceThenOneFound() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "TestTags");
		List<String> tags = clazz.getAllJavaDocTagsOf("@first");
		assertArrayEquals(new String[] {"My first value"}, tags.toArray(new String[] {}));
	}

	@Test
	public void whenTagAppearsOnceWithoutArgsThenOneEmptyFound() throws Exception {
		FrankClass clazz = classRepository.findClass(PACKAGE + "TestTags");
		List<String> tags = clazz.getAllJavaDocTagsOf("@third");
		assertEquals(1, tags.size());
		assertTrue(tags.get(0).isEmpty());
	}
}
