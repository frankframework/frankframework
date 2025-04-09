package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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
	 * Method: {@link StringUtil#hideAll(String string, Collection regexes})
	 */
	@Test
	public void testHideAll() {
		// Arrange
		String input = "<t1>test1</t1><t2>test2</t2><t3>test3</t3><t1>test1 again</t1>";
		Pattern t1 = Pattern.compile("(?<=<t1>)(.*?)(?=</t1>)");
		Pattern t2 = Pattern.compile("<t2>.*?.<t2>"); // Pattern won't match
		Pattern t3 = Pattern.compile("(?<=<t3>)(.*?)(?=</t3>)");

		// Act
		String res = StringUtil.hideAll(input, List.of(t1, t2, t3));

		// Assert
		assertEquals("<t1>*****</t1><t2>test2</t2><t3>*****</t3><t1>***********</t1>", res);
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
	public void testHideAllMode1() {
		String s = "1 Donald Duck 123  Hey hey  45  Wooo  6789 and 12345";
		String regex = "\\d+";
		String res = StringUtil.hideAll(s, regex, 1);
		assertEquals("* Donald Duck **3  Hey hey  *5  Wooo  **89 and ***45", res);
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

	@ParameterizedTest
	@CsvSource({
			", 0",
			"z, 0",
			"abcxyzdefxyz, 2",
			"abc xyz def xy, 1",
			"xyz, 1"
	})
	public void testCountSubstring(String input, int expectedCount) {
		int actualCount = StringUtil.countSubstring(input, "xyz");
		assertEquals(expectedCount, actualCount);
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
				arguments("a,b", List.of("a", "b")),
				arguments(",a,,b,", List.of("a", "b")),
				arguments("  a  , b ", List.of("a", "b")),
				arguments(null, List.of()),
				arguments("", List.of()),
				arguments("      ", List.of()),
				arguments("   ,   ", List.of()),
				arguments(",,,,,", List.of()),
				arguments(",,      ,,,", List.of()),
				// Oversized input to stress the regex-engine, catch any potential runaway regex searching.
				arguments(StringUtils.repeat("," + StringUtils.repeat(" ", 10_000), 1_000), Collections.emptyList())
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSplitStringDefaultDelimiter(String input, Iterable<String> expected) {
		// Act
		List<String> result = StringUtil.split(input);

		// Assert
		assertIterableEquals(expected, result, () -> "With input [" + escapeUnprintable(input) + "] and default delimiter [,] expected result [" + String.join("|", expected) + "] but instead got [" + String.join("|", result) + "]");
	}

	public static Stream<Arguments> testSplitStringCustomDelimiters() {
		return Stream.of(
				arguments(null, "\\/", List.of()),
				arguments("", "\\/", List.of()),
				arguments("a,b; c    ", ",", List.of("a", "b; c")),
				arguments("a,b;c", ";,", List.of("a", "b", "c")),
				arguments("a,b;c d", ";,", List.of("a", "b", "c d")),
				arguments("a b c", " ", List.of("a", "b", "c")),
				arguments("a,b c", " ", List.of("a,b", "c")),
				arguments("a, b c", ", ", List.of("a", "b", "c")),
				arguments(";a,b;,c,", ";,", List.of("a", "b", "c")),
				arguments(" a , ;  b;,c ; ", ";,", List.of("a", "b", "c")),
				arguments(" a , ;  b  c  ", " ;,", List.of("a", "b", "c")),
				arguments("filename.txt", ".", List.of("filename", "txt")), // Check that the dot character works as separator in the regexes
				arguments(" a , b  c\t d\re  \n f  \f  g", ", \t\r\n\f", List.of("a", "b", "c", "d", "e", "f", "g")),
				// Oversized inputs to stress the regex-engine, catch any potential regex performance issues.
				arguments("                                             ", " ", List.of()), // Short-ish input with only whitespace and whitespace as separator
				arguments("                   :                 ", ":", List.of()), // Short-ish input with spaces and single separator
				arguments(StringUtils.repeat("                             ", 1_000_000), " ", List.of()), // Long empty string
				arguments(";" + StringUtils.repeat("                             ", 1_000_000), ";", List.of()), // Long nearly empty string with single separator
				arguments(StringUtils.repeat(";" + StringUtils.repeat(" ", 10_000), 1_000), ";,|", List.of()) // Very long string with mostly spaces and many separators
		);
	}

	@ParameterizedTest
	@MethodSource
	void testSplitStringCustomDelimiters(String input, String delimiters, Iterable<String> expected) {
		// Act
		List<String> result = StringUtil.split(input, delimiters);

		LOG.debug("input: [{}]", () -> escapeUnprintable(input));
		LOG.debug("result [{}]", () -> String.join("|", result));

		// Assert
		assertIterableEquals(expected, result, () -> "With input [" + escapeUnprintable(input) + "] and custom delimiters [" + escapeUnprintable(delimiters) + "] expected result [" + String.join("|", expected) + "] but instead got [" + String.join("|", result) + "]");
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
				return Character.toString((char) chr);
		}
	}

	@Test
	public void testReflectionToString() {
		String startsWithStr = "StringUtilTest.ToStringTestClass[field1=tralala,field2=lalala,field3=false,"
				+ "hoofdletterPassword=*************,password=**********,props={";

		ToStringTestClass testClass = new ToStringTestClass();
		int hashcode = testClass.props.hashCode();
		String toStringResult = StringUtil.reflectionToString(testClass);
		assertTrue(toStringResult.startsWith(startsWithStr));
		assertTrue(toStringResult.contains("no-string-password=***hidden***"));
		assertTrue(toStringResult.contains("com.tibco.tibjms.factory.username=tipko"));
		assertTrue(toStringResult.contains("com.tibco.tibjms.factory.password=*************"));
		assertTrue(toStringResult.endsWith("}]"));
		assertEquals(hashcode, testClass.props.hashCode());
	}

	@Test
	public void testReflectionToStringNull() {
		assertEquals("<null>", StringUtil.reflectionToString(null));
	}

	@SuppressWarnings("unused")
	private static class ToStringTestClass {
		private final String field1 = "tralala";
		private final String field2 = "lalala";
		private final boolean field3 = false;
		private final String password = "top-secret";
		private final String hoofdletterPassword = "bottom-secret";
		private final Properties props = new Properties();

		public ToStringTestClass() {
			props.put("no-string-password", Collections.singletonList("something"));
			props.setProperty("com.tibco.tibjms.factory.username", "tipko");
			props.setProperty("com.tibco.tibjms.factory.password", "not-so-secret");
		}
	}
}
