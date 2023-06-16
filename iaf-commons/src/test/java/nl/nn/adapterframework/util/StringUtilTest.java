package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

class StringUtilTest {

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
		Collection<Object> c = new ArrayList<Object>() {
			@Override
			public Iterator<Object> iterator() {
				final Iterator<Object> delegate = super.iterator();
				return new Iterator<Object>() {
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
}
