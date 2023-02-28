package nl.nn.adapterframework.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.nn.adapterframework.core.IMessageBrowser.HideMethod;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * Misc Tester.
 *
 * @author <Sina Sen>
 */
public class MiscTest {

	@TempDir
	public static Path testFolder;

	private static Path file;

	@BeforeAll
	public static void setUp() throws IOException {
		file = Files.createFile(testFolder.resolve("lebron.txt"));
	}

	@AfterClass
	public static void cleanUp() {
		File f = new File("lebron.txt");
		f.delete();
	}

	private void writeToTestFile() throws IOException {
		Writer w = new FileWriter(file.toString());
		w.write("inside the lebron file");
		w.close();
	}

	@Test
	public void testStreamToString() throws IOException {
		String tekst = "dit is een string";
		ByteArrayInputStream bais = new ByteArrayInputStream(tekst.getBytes());

		CloseChecker closeChecker = new CloseChecker(bais);
		String actual = Misc.streamToString(closeChecker);

		assertEquals(tekst, actual);
		assertTrue(closeChecker.inputStreamClosed, "inputstream was not closed");
	}

	private class CloseChecker extends FilterInputStream {

		boolean inputStreamClosed;

		public CloseChecker(InputStream arg0) {
			super(arg0);
		}

		@Override
		public void close() throws IOException {
			inputStreamClosed = true;

			super.close();
		}
	}

	/**
	 * Method: fileToStream(String filename, OutputStream output)
	 */
	@Test
	public void testFileToStream() throws Exception {
		writeToTestFile();
		OutputStream os = new ByteArrayOutputStream();
		Misc.fileToStream(file.toString(), os);
		assertEquals("inside the lebron file", os.toString());
	}

	/**
	 * Method: streamToStream(InputStream input, OutputStream output)
	 */
	@Test
	public void testStreamToStreamForInputOutput() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		OutputStream baos = new ByteArrayOutputStream();
		Misc.streamToStream(bais, baos);
		assertEquals("test", baos.toString());
	}

	/**
	 * Method: streamToStream(InputStream input, OutputStream output, boolean
	 * closeInput)
	 */
	@Test
	public void testStreamToStreamForInputOutputCloseInput() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		OutputStream baos = new ByteArrayOutputStream();
		Misc.streamToStream(bais, baos);
		assertEquals("test", baos.toString());
	}

	/**
	 * Method: streamToFile(InputStream inputStream, File file)
	 */
	@Test
	public void testStreamToFile() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		Misc.streamToFile(bais, file.toFile());

		// to read from the file
		InputStream is = new FileInputStream(file.toString());
		BufferedReader buf = new BufferedReader(new InputStreamReader(is));

		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();

		while(line != null) {
			sb.append(line).append("\n");
			line = buf.readLine();
		}
		buf.close();

		String fileAsString = sb.toString();
		assertEquals("test\n", fileAsString);
	}

	/**
	 * Method: streamToBytes(InputStream inputStream)
	 */
	@Test
	public void testStreamToBytes() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		byte[] arr = Misc.streamToBytes(bais);
		assertEquals("test", new String(arr, StandardCharsets.UTF_8));
	}

	/**
	 * Method: readerToWriter(Reader reader, Writer writer)
	 */
	@Test
	public void testReaderToWriterForReaderWriter() throws Exception {
		Reader reader = new StringReader("test");
		Writer writer = new StringWriter();
		Misc.readerToWriter(reader, writer);
		assertEquals("test", writer.toString());
	}

	@Test
	public void testFileToStringFileNameEndLine() throws Exception {
		// Misc.resourceToString()
		writeToTestFile();
		assertEquals("inside the lebron file", Misc.fileToString(file.toString(), " the end"));
	}

	/**
	 * Method: readerToString(Reader reader, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testReaderToStringNoXMLEncode() throws Exception {
		Reader r = new StringReader("<root> \n" + "    <name>GeeksforGeeks</name> \n" + "    <address> \n" + "        <sector>142</sector> \n" + "        <location>Noida</location> \n" + "    </address> \n" + "</root> r");
		String s = Misc.readerToString(r, "23", false);
		assertEquals("<root> 23    <name>GeeksforGeeks</name> 23    <address> 23        <sector>142</sector> 23        <location>Noida</location> 23    </address> 23</root> r", s);
	}

	/**
	 * Method: readerToString(Reader reader, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testReaderToStringXMLEncodeWithEndOfLineString() throws Exception {
		Reader r = new StringReader("<root> \n" + "    <name>GeeksforGeeks</name> \n" + "    <address> \n" + "        <sector>142</sector> \n" + "        <location>Noida</location> \n" + "    </address> \n" + "</root> r");
		String s = Misc.readerToString(r, "23", true);
		assertEquals("&lt;root&gt; 23    &lt;name&gt;GeeksforGeeks&lt;/name&gt; 23    &lt;address&gt; 23        &lt;sector&gt;142&lt;/sector&gt; 23        &lt;location&gt;Noida&lt;/location&gt; 23    &lt;/address&gt; 23&lt;/root&gt; r", s);
	}

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
	 * Method: copyContext(String keys, Map<String,Object> from, Map<String,Object>
	 * to)
	 */
	@Test
	public void testCopyContext() throws Exception {
		Map<String, Object> from = new HashMap<>();
		PipeLineSession to = new PipeLineSession();
		String keys = "a,b";
		from.put("a", 15);
		from.put("b", 16);
		Misc.copyContext(keys, from, to, null);
		assertEquals(from,to);
	}

	@Test
	public void testCopyContextNullKeys() throws Exception {
		Map<String, Object> from = new HashMap<>();
		PipeLineSession to = new PipeLineSession();
		from.put("a", 15);
		from.put("b", 16);
		Misc.copyContext(null, from, to, null);
		assertEquals(from,to);
	}

	@Test
	public void testCopyContextLimitedKeys() throws Exception {
		Map<String, Object> from = new HashMap<>();
		PipeLineSession to = new PipeLineSession();
		String keys = "a";
		from.put("a", 15);
		from.put("b", 16);
		Misc.copyContext(keys, from, to, null);
		assertEquals(1,to.size());
	}

	@Test
	public void testCopyContextEmptyKeys() throws Exception {
		Map<String, Object> from = new HashMap<>();
		PipeLineSession to = new PipeLineSession();
		from.put("a", 15);
		from.put("b", 16);
		Misc.copyContext("", from, to, null);
		assertEquals(0,to.size());
	}
	/**
	 * Method: toFileSize(String value, long defaultValue)
	 */
	@Test
	public void testToFileSizeForValueDefaultValue() throws Exception {
		long res = Misc.toFileSize("14GB", 20);
		assertEquals("15032385536", Long.toString(res));
	}

	@Test
	public void testNegativeToFileSize() throws Exception {
		String size = Misc.toFileSize(-1);
		assertEquals("-1", size);
	}

	@Test
	public void testToFileSize1024() throws Exception {
		String size = Misc.toFileSize(1024);
		assertEquals("1KB", size);
	}

	@Test
	public void testNegativeToFileSize512MB() throws Exception {
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
	public void testAddItemsToList() throws Exception {
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
	public void testGetFileSystemTotalSpace() throws Exception {
		assertFalse(Misc.getFileSystemTotalSpace().isEmpty());
	}

	/**
	 * Method: getFileSystemFreeSpace()
	 */
	@Test
	public void testGetFileSystemFreeSpace() throws Exception {
		assertFalse(Misc.getFileSystemFreeSpace().isEmpty());
	}

	/**
	 * Method: getAge(long value)
	 */
	@Test
	public void testGetAge() throws Exception {
		assertFalse(Misc.getAge(1).isEmpty());
	}

	/**
	 * Method: getDurationInMs(long value)
	 */
	@Test
	public void testGetDurationInMs() throws Exception {
		assertFalse(Misc.getDurationInMs(14).isEmpty());
	}

	/**
	 * Method: parseAge(String value, long defaultValue)
	 */
	@Test
	public void testParseAge() throws Exception {
		long res = Misc.parseAge("2D", 100);
		assertEquals(172800000, res);
	}

	/**
	 * Method: cleanseMessage(String inputString, String hideRegex, String
	 * hideMethod)
	 */
	@Test
	public void testCleanseMessage() throws Exception {
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
		String s1 = Misc.resourceToString(resource);
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
		String s1 = Misc.resourceToString(resource, " newly added string ", true);
		assertEquals("&lt;!doctype txt&gt;this is a text file. newly added string new line in the text file.", s1);
	}

	@Test
	public void testResourceToStringForResourceEndOfLineString() throws Exception {
		URL resource = TestFileUtils.getTestFileURL("/Misc/test_file_for_resource_to_string_misc.txt");
		String s1 = Misc.resourceToString(resource, " newly added string ");
		assertEquals("<!doctype txt>this is a text file. newly added string new line in the text file.", s1);
	}

	@Test
	public void testPrettyJson() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/Misc/minified.json");
		String inputString = Misc.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/Misc/prettified.json");
		String expectedString = Misc.resourceToString(expected);
		TestAssertions.assertEqualsIgnoreCRLF(expectedString, Misc.jsonPretty(inputString));
	}

	@Test
	public void testPrettyJsonArray() throws IOException {
		URL input = TestFileUtils.getTestFileURL("/Misc/minifiedJsonArray.json");
		String inputString = Misc.resourceToString(input);
		URL expected = TestFileUtils.getTestFileURL("/Misc/prettifiedJsonArray.json");
		String expectedString = Misc.resourceToString(expected);
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

	@Test
	public void testIbmDescriptorResources() throws Exception {
		String descriptorPath = Misc.getApplicationDeploymentDescriptorPath();
		assertThat(descriptorPath, Matchers.endsWith("META-INF"));
		String applBindings = Misc.getDeployedApplicationBindings();
		assertNotNull(applBindings);
		String deploymentDescriptor = Misc.getApplicationDeploymentDescriptor();
		assertNotNull(deploymentDescriptor);
	}

	@Test
	public void testIbmConfigurationResources() throws Exception {
		String configurationResources = Misc.getConfigurationResources();
		assertThat(configurationResources, Matchers.startsWith("<dummy xml=\"file\" />"));
		String server = Misc.getConfigurationServer();
		assertThat(server, Matchers.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
	}
}
