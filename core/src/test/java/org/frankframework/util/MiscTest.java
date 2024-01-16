package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.frankframework.core.IMessageBrowser.HideMethod;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.junit.jupiter.api.Test;

/**
 * Misc Tester.
 *
 * @author <Sina Sen>
 */
public class MiscTest {

	/**
	 * Method: byteArrayToString(byte[] input, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testByteArrayToString() throws Exception {
		String s = "test";
		byte[] arr = s.getBytes();
		String res = Misc.byteArrayToString(arr, "", true);
		assertEquals(s, res);
	}

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
		assertEquals("1KB", size);
	}

	@Test
	public void testNegativeToFileSize512MB() {
		String size = Misc.toFileSize(1024*1024*5);
		assertEquals("5MB", size);
	}

	/**
	 * Method: toFileSize(long value)
	 */
	@Test
	public void testToFileSizeValue() throws Exception {
		String kb = Misc.toFileSize(150000, false, true);
		String mb = Misc.toFileSize(15000000, true);
		String gb = Misc.toFileSize(Long.parseLong("3221225472"));
		assertEquals("3GB", gb);
		assertEquals("14 MB", mb);
		assertEquals("146KB", kb);
	}

	@Test
	public void testListToStringWithStringList() {
		List<String> list = new ArrayList<>();
		list.add("bailar");
		list.add("besos");
		String res = Misc.listToString(list);
		assertEquals("bailarbesos", res);
	}

	/**
	 * Method: addItemsToList(Collection<String> collection, String list, String
	 * collectionDescription, boolean lowercase)
	 */
	@Test
	public void testAddItemsToList() {
		List<String> stringCollection = new ArrayList<>();
		String list = "a,b,C";
		String collectionDescription = "First 3 letters of the alphabet";
		Misc.addItemsToList(stringCollection, list, collectionDescription, true);
		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add("a");
		arrayList.add("b");
		arrayList.add("c");
		assertEquals(3, stringCollection.size());
		assertEquals("c", stringCollection.get(stringCollection.size() - 1));
	}

	/**
	 * Method: getFileSystemTotalSpace()
	 */
	@Test
	public void testGetFileSystemTotalSpace() {
		assertFalse(Misc.getFileSystemTotalSpace().isEmpty());
	}

	/**
	 * Method: getFileSystemFreeSpace()
	 */
	@Test
	public void testGetFileSystemFreeSpace() {
		assertFalse(Misc.getFileSystemFreeSpace().isEmpty());
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
	public void testParseAge() {
		long res = Misc.parseAge("2D", 100);
		assertEquals(172800000, res);
	}

	/**
	 * Method: cleanseMessage(String inputString, String hideRegex, String
	 * hideMethod)
	 */
	@Test
	public void testCleanseMessage() {
		String s = "Donald Duck 23  Hey hey  14  Wooo";
		String regex = "\\d";
		String res = Misc.cleanseMessage(s, regex, HideMethod.ALL);
		assertEquals("Donald Duck **  Hey hey  **  Wooo", res);
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

	@Test
	public void testAuthorityInUrlString1() {
		String username="user";
		String password="password";
		String url="http://aa:bb@host.nl";
		String expected = "http://user:password@host.nl";
		assertEquals(expected, Misc.insertAuthorityInUrlString(url, null, username, password));
	}

	@Test
	public void testAuthorityInUrlString2() {
		String username="user";
		String password="password";
		String url="http://host.nl";
		String expected = "http://user:password@host.nl";
		assertEquals(expected, Misc.insertAuthorityInUrlString(url, null, username, password));
	}

	@Test
	public void testAuthorityInUrlString3() {
		String username=null;
		String password=null;
		String url="http://aa:bb@host.nl";
		String expected = "http://aa:bb@host.nl";
		assertEquals(expected, Misc.insertAuthorityInUrlString(url, null, username, password));
	}

	@Test
	public void testAuthorityInUrlString4() {
		String username="user";
		String password="password";
		String url="aa:bb@host.nl";
		String expected = "user:password@host.nl";
		assertEquals(expected, Misc.insertAuthorityInUrlString(url, null, username, password));
	}

	@Test
	public void testAuthorityInUrlString5() {
		String username="user";
		String password="password";
		String url="host.nl";
		String expected = "user:password@host.nl";
		assertEquals(expected, Misc.insertAuthorityInUrlString(url, null, username, password));
	}

}
