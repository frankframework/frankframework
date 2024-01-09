package org.frankframework.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StringUtilTest {
	private static final Logger LOG = LogManager.getLogger(StringUtil.class);

	/**
	 * Method: concatStrings(String part1, String separator, String part2)
	 */
	@Test
	public void testConcatStrings() {
		String a = "LeBron";
		String b = "James";
		String separator = "//";
		String res = StringUtil.concatStrings(a, separator, b);
		assertEquals("LeBron//James", res);
	}

	@Test
	public void testConcatStringsFirstEmpty() {
		String a = "";
		String b = "James";
		String seperator = "//";
		String res = StringUtil.concatStrings(a, seperator, b);
		assertEquals("James", res);
	}

	@Test
	public void testConcatStringsSecondEmpty() {
		String a = "LeBron";
		String b = "";
		String seperator = "//";
		String res = StringUtil.concatStrings(a, seperator, b);
		assertEquals("LeBron", res);
	}

	@Test
	public void testConcat() {
		String seperator = "|";
		String res = StringUtil.concat(seperator, null, "a", "b", null, "c", null);
		assertEquals("a|b|c", res);
	}

	/**
	 * Method: hide(String string)
	 */
	@Test
	public void testHideString() {
		String a = "test";
		String res = StringUtil.hide(a);
		assertEquals("****", res);
	}

	/**
	 * Method: hide(String string, int mode)
	 */
	@Test
	public void testHideForStringMode() {
		String a = "test";
		String res = StringUtil.hide(a, 1);
		assertEquals("t**t", res);
	}

	/**
	 * Method: hideFirstHalf(String inputString, String regex)
	 */
	@Test
	public void testHideFirstHalf() {
		String s = "Donald Duck     Hey hey     Wooo";
		String hideRegex = "[^\\s*].*[^\\s*]";
		String res = StringUtil.hideFirstHalf(s, hideRegex);
		assertEquals("****************Hey hey     Wooo", res);
	}

	/**
	 * Method: countRegex(String string, String regex)
	 */
	@Test
	public void testCountRegex() {
		String s = "12ab34";
		String regex = "\\d";
		int regexCount = StringUtil.countRegex(s, regex);
		assertEquals(4, regexCount);
	}

	@Test
	public void testUcFirst() {
		assertEquals("Test123", StringUtil.ucFirst("test123"));
		assertEquals("TEST123", StringUtil.ucFirst("tEST123"));
		assertEquals("TEST123", StringUtil.ucFirst("TEST123"));
		assertEquals("123test", StringUtil.ucFirst("123test"));
	}

	@Test
	public void testLcFirst() {
		assertEquals("test123", StringUtil.lcFirst("Test123"));
		assertEquals("tEST123", StringUtil.lcFirst("TEST123"));
		assertEquals("tEST123", StringUtil.lcFirst("TEST123"));
		assertEquals("123test", StringUtil.lcFirst("123test"));
	}

	@Test
	public void testSafeCollectionToString() {
		// Arrange
		Collection<Object> c = new ArrayList<>();
		c.add("A");
		c.add("B");
		c.add(3);
		c.add(4);

		// Act / Assert
		assertEquals("A, B, 3, 4", StringUtil.safeCollectionToString(c));
	}

	@Test
	public void testSafeCollectionToStringWithException() {
		// Arrange
		Collection<Object> c = new ArrayList<>() {
			@Override
			@Nonnull
			public Iterator<Object> iterator() {
				final Iterator<Object> delegate = super.iterator();
				return new Iterator<>() {
					@Override
					public boolean hasNext() {
						return true;
					}

					@Override
					public Object next() {
						if (delegate.hasNext()) return delegate.next();
						// For the test, after exhaustion throw exception
						throw new ConcurrentModificationException();
					}
				};
			}
		};
		c.add("A");
		c.add("B");
		c.add(3);
		c.add(4);

		// Act / Assert
		assertEquals("A, B, 3, 4 ...more", StringUtil.safeCollectionToString(c));
	}

	public static Stream<Arguments> testSplitStringDefaultDelimiter() {
		return Stream.of(
				arguments("a,b", asList("a", "b")),
				arguments(",a,,b,", asList("a", "b")),
				arguments(" a , b ", asList("a", "b")),
				arguments(null, Collections.emptyList()),
				arguments("", Collections.emptyList())
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSplitStringDefaultDelimiter(String input, Iterable<String> expected) {
		// Act
		List<String> result = StringUtil.split(input);

		// Assert
		assertIterableEquals(expected, result);
	}

	public static Stream<Arguments> testSplitStringCustomDelimiters() {
		return Stream.of(
				arguments(null, "\\/", Collections.emptyList()),
				arguments("", "\\/", Collections.emptyList()),
				arguments("a,b;c", ";,", asList("a", "b", "c")),
				arguments(";a,b;,c,", ";,", asList("a", "b", "c")),
				arguments(" a , ;  b;,c ; ", ";,", asList("a", "b", "c")),
				arguments(" a , ;  b  c  ", " ;,", asList("a", "b", "c")),
				arguments(" a , b  c\t d\re  \n f  \f  g", ", \t\r\n\f", asList("a", "b", "c", "d", "e", "f", "g"))
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSplitStringCustomDelimiters(String input, String delimiters, Iterable<String> expected) {
		// Act
		List<String> result = StringUtil.split(input, delimiters);

		LOG.debug("input: [{}]", ()->escapeUnprintable(input));
		LOG.debug("result [{}]", ()->String.join("|", result));

		// Assert
		assertIterableEquals(expected, result);
	}

	static String escapeUnprintable(String input) {
		if (input == null) {
			return "null";
		}
		return input.chars()
				.mapToObj(StringUtilTest::mapChar)
				.collect(Collectors.joining());
	}

	static String mapChar(int chr) {
		switch (chr) {
			case '\t':
				return "\\t";
			case '\r':
				return "\\r";
			case '\n':
				return "\\n";
			case '\f':
				return "\\f";
			default:
				return Character.toString((char)chr);
		}
	}
}
