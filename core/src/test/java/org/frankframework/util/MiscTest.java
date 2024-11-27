package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;

/**
 * Misc Tester.
 *
 * @author <Sina Sen>
 */
public class MiscTest {

	/**
	 * Method: toFileSize(String value, long defaultValue)
	 */
	@Test
	public void testToFileSizeForValueDefaultValue() {
		long res = Misc.toFileSize("14GB", 20);
		assertEquals("15032385536", Long.toString(res));
	}

	@Test
	public void testNegativeToFileSize() {
		String size = Misc.toFileSize(-1);
		assertEquals("-1", size);
	}

	@Test
	public void testToFileSize1024() {
		String size = Misc.toFileSize(1024);
		assertEquals("1KiB", size);
	}

	@Test
	public void testNegativeToFileSize512MB() {
		String size = Misc.toFileSize(1024*1024*5);
		assertEquals("5MiB", size);
	}

	/**
	 * Method: toFileSize(long value)
	 */
	@Test
	public void testToFileSizeValue() throws Exception {
		String kbIecUnits = Misc.toFileSize(150000, false, false);
		String mbIecUnits = Misc.toFileSize(15000000, true);
		String gbIecUnits = Misc.toFileSize(Long.parseLong("3221225472"));

		String kbSiUnits = Misc.toFileSize(150000, false, true);
		String mbSiUnits = Misc.toFileSize(15000000, true, true);
		String gbSiUnits = Misc.toFileSize(Long.parseLong("3221225472"), false, true);

		assertEquals("146KiB", kbIecUnits);
		assertEquals("14 MiB", mbIecUnits);
		assertEquals("3GiB", gbIecUnits);

		assertEquals("150kB", kbSiUnits);
		assertEquals("15 MB", mbSiUnits);
		assertEquals("3GB", gbSiUnits);
	}

	/**
	 * Method: getAge(long value)
	 */
	@Test
	public void testGetAge() {
		assertFalse(Misc.getAge(1).isEmpty());
	}

	/**
	 * Method: getDurationInMs(long value)
	 */
	@Test
	public void testGetDurationInMs() {
		assertFalse(Misc.getDurationInMs(14).isEmpty());
	}

	/**
	 * Method: parseAge(String value, long defaultValue)
	 */
	@Test
	public void testParseAgeD() {
		long res = Misc.parseAge("2D", 100);
		assertEquals(172800000, res);
	}

	@Test
	public void testParseAgeS() {
		long res = Misc.parseAge("2S", 0);
		assertEquals(2000, res);
	}

	/**
	 * Method: resourceToString(URL resource)
	 */
	@Test
	public void testResourceToStringResource() throws Exception {
		URL resource = TestFileUtils.getTestFileURL("/Misc/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource);
		TestAssertions.assertEqualsIgnoreWhitespaces("<!doctype txt>this is a text file.\nnew line in the text file.", s1);
		assertFalse(s1.isEmpty());
	}

	/**
	 * Method: resourceToString(URL resource, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testResourceToStringForResourceEndOfLineStringXmlEncode() throws Exception {
		URL resource = TestFileUtils.getTestFileURL("/Misc/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource, " newly added string ", true);
		assertEquals("&lt;!doctype txt&gt;this is a text file. newly added string new line in the text file.", s1);
	}

	@Test
	public void testResourceToStringForResourceEndOfLineString() throws Exception {
		URL resource = TestFileUtils.getTestFileURL("/Misc/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource, " newly added string ");
		assertEquals("<!doctype txt>this is a text file. newly added string new line in the text file.", s1);
	}

	@Test
	public void testPrettyJson() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/Misc/minified.json");
		String inputString = StreamUtil.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/Misc/prettified.json");
		String expectedString = StreamUtil.resourceToString(expected);
		TestAssertions.assertEqualsIgnoreCRLF(expectedString, Misc.jsonPretty(inputString));
	}

	@Test
	public void testPrettyJsonArray() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/Misc/minifiedJsonArray.json");
		String inputString = StreamUtil.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/Misc/prettifiedJsonArray.json");
		String expectedString = StreamUtil.resourceToString(expected);
		TestAssertions.assertEqualsIgnoreCRLF(expectedString, Misc.jsonPretty(inputString));
	}
}
