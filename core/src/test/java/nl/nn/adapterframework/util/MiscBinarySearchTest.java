package nl.nn.adapterframework.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests of methods {@link Misc#addToSortedListNonUnique(List, Object)} and {@link Misc#addToSortedListUnique(List, Object)}.
 * @author martijn
 *
 */
public class MiscBinarySearchTest {

	public List<String> start;
	public String added;
	public List<String> afterAddNonUnique;
	public List<String> afterAddUnique;

	public static Collection<Object[]> data() {
		return asList(new Object[][] {
			{asList(), "A", asList("A"), asList("A")},
			{asList("A"), "A", asList("A", "A"), asList("A")},
			{asList("A", "C"), "B", asList("A", "B", "C"), asList("A", "B", "C")},
			{asList("A", "B", "C"), "B", asList("A", "B", "B", "C"), asList("A", "B", "C")}
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testNonUnique(List<String> start, String added, List<String> afterAddNonUnique, List<String> afterAddUnique) {
		List<String> input = new ArrayList<>(start);
		Misc.addToSortedListNonUnique(input, added);
		assertArrayEquals(afterAddNonUnique.toArray(), input.toArray());
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testUnique(List<String> start, String added, List<String> afterAddNonUnique, List<String> afterAddUnique) {
		List<String> input = new ArrayList<>(start);
		Misc.addToSortedListUnique(input, added);
		assertArrayEquals(afterAddUnique.toArray(), input.toArray());
	}
}
