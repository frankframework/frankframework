package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.doc.model.FrankElement;

public class DocWriterNewTest {
	@Test
	public void whenFrankElementsHaveUniqueSimpleNamesThenSyntax2NamesAreSimpleNames() {
		List<FrankElement> elements = Arrays.asList(
				new FrankElement("com.X", "X"));
		Map<String, String> result = DocWriterNew.chooseSyntax2Names(elements);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("com.X"));
		assertEquals("X", result.get("com.X"));
	}

	@Test
	public void whenFrankElementsHaveNonUniqueSimpleNamesThenSyntax2NameAddsPackageComponents() {
		List<FrankElement> elements = Arrays.asList(
				new FrankElement("com.my.MyX", "MyX"),
				new FrankElement("org.yours.MyX", "MyX"));
		Map<String, String> result = DocWriterNew.chooseSyntax2Names(elements);
		assertEquals(2, result.size());
		assertTrue(result.containsKey("com.my.MyX"));
		assertTrue(result.containsKey("org.yours.MyX"));
		assertEquals("MyMyX", result.get("com.my.MyX"));
		assertEquals("YoursMyX", result.get("org.yours.MyX"));
	}
}
