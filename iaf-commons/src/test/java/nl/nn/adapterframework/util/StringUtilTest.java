package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringUtilTest {

	/**
	 * Method: replace(String source, String from, String to)
	 */
	@Test
	public void testReplace() {
		String a = "Kobe";
		String res = StringUtil.replace(a, "Ko", "Phoe");
		assertEquals("Phoebe", res);
	}

	/**
	 * Method: concatStrings(String part1, String separator, String part2)
	 */
	@Test
	public void testConcatStrings() {
		String a = "LeBron";
		String b = "James";
		String seperator = "//";
		String res = StringUtil.concatStrings(a, seperator, b);
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

}
