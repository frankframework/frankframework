package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class StreamUtilTest {

	@TempDir
	public static Path testFolder;

	private static Path file;
	private final String UTF8_EXPECTED = "ABC één euro: €1,00";
	private final String OTHER_EXPECTED = "ABC néé hè";

	@BeforeAll
	public static void setUp() throws IOException {
		file = Files.createFile(testFolder.resolve("lebron.txt"));
	}

	@AfterAll
	public static void cleanUp() {
		File f = new File("lebron.txt");
		f.delete();
	}

	public void testReader(String inputFile, String expected) throws IOException {
		testReader(inputFile, expected, null);
	}

	public void testReader(String inputFile, String expected, String defaultCharset) throws IOException {
		URL input = getClass().getResource(inputFile);

		int i;
		InputStream inputStream = input.openStream();
		while ((i = inputStream.read()) >= 0) {
			log.debug(Integer.toHexString(i) + " ");
		}

		Reader reader;
		if (defaultCharset == null) {
			reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream());
		} else {
			reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream(), defaultCharset);
		}

		String actual = StreamUtil.readerToString(reader, null);

		assertEquals(expected, actual);
		reader.close();
	}

	@Test
	public void testInputStreamReaderPlainUTF8() throws IOException {
		testReader("/StreamUtil/inUTF8noBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF8withBOM() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF16LEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16LEwithBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderUTF16BEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16BEwithBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderAnsi() throws IOException {
		testReader("/StreamUtil/inISO8859-1.bin", OTHER_EXPECTED, "ISO8859-1");
	}

	@Test
	public void testInputStreamReaderUTF8withBOMWrongDefaultCharset() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED, "ISO8859-1");
	}


	public void testStreamToByteArray(String inputFile, boolean skipBOM, String expected, boolean expectBOM) throws IOException {
		URL input = getClass().getResource(inputFile);

		byte[] byteArray = StreamUtil.streamToByteArray(input.openStream(), skipBOM);

		String actual;
		if (expectBOM) {
			assertEquals((byte) 0xEF, byteArray[0]);
			assertEquals((byte) 0xBB, byteArray[1]);
			assertEquals((byte) 0xBF, byteArray[2]);
			actual = new String(byteArray, 3, byteArray.length - 3, StandardCharsets.UTF_8);
		} else {
			actual = new String(byteArray, StandardCharsets.UTF_8);
		}

		assertEquals(expected, actual);
	}

	@Test
	public void testStreamToByteArrayUTF8noBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", false, UTF8_EXPECTED, false);
	}

	@Test
	public void testStreamToByteArrayUTF8withBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", false, UTF8_EXPECTED, true);
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
		String actual = StreamUtil.streamToString(closeChecker);

		assertEquals(tekst, actual);
		assertTrue(closeChecker.inputStreamClosed, "inputstream was not closed");
	}

	/**
	 * Method: fileToStream(String filename, OutputStream output)
	 */
	@Test
	public void testFileToStream() throws Exception {
		writeToTestFile();
		OutputStream os = new ByteArrayOutputStream();
		StreamUtil.fileToStream(file.toString(), os);
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
		StreamUtil.streamToStream(bais, baos);
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
		StreamUtil.streamToStream(bais, baos);
		assertEquals("test", baos.toString());
	}

	/**
	 * Method: streamToFile(InputStream inputStream, File file)
	 */
	@Test
	public void testStreamToFile() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		StreamUtil.streamToFile(bais, file.toFile());

		// to read from the file
		InputStream is = new FileInputStream(file.toString());
		BufferedReader buf = new BufferedReader(new InputStreamReader(is));

		String line = buf.readLine();
		StringBuilder sb = new StringBuilder();

		while (line != null) {
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
		byte[] arr = StreamUtil.streamToBytes(bais);
		assertEquals("test", new String(arr, StandardCharsets.UTF_8));
	}

	/**
	 * Method: readerToWriter(Reader reader, Writer writer)
	 */
	@Test
	public void testReaderToWriterForReaderWriter() throws Exception {
		Reader reader = new StringReader("test");
		Writer writer = new StringWriter();
		StreamUtil.readerToWriter(reader, writer);
		assertEquals("test", writer.toString());
	}

	/**
	 * Method: readerToString(Reader reader, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testReaderToStringNoXMLEncode() throws Exception {
		Reader r = new StringReader("<root> \n" + "    <name>GeeksforGeeks</name> \n" + "    <address> \n" + "        <sector>142</sector> \n" + "        <location>Noida</location> \n" + "    </address> \n" + "</root> r");
		String s = StreamUtil.readerToString(r, "23", false);
		assertEquals("<root> 23    <name>GeeksforGeeks</name> 23    <address> 23        <sector>142</sector> 23        <location>Noida</location> 23    </address> 23</root> r", s);
	}

	/**
	 * Method: readerToString(Reader reader, String endOfLineString, boolean
	 * xmlEncode)
	 */
	@Test
	public void testReaderToStringXMLEncodeWithEndOfLineString() throws Exception {
		Reader r = new StringReader("<root> \n" + "    <name>GeeksforGeeks</name> \n" + "    <address> \n" + "        <sector>142</sector> \n" + "        <location>Noida</location> \n" + "    </address> \n" + "</root> r");
		String s = StreamUtil.readerToString(r, "23", true);
		assertEquals("&lt;root&gt; 23    &lt;name&gt;GeeksforGeeks&lt;/name&gt; 23    &lt;address&gt; 23        &lt;sector&gt;142&lt;/sector&gt; 23        &lt;location&gt;Noida&lt;/location&gt; 23    &lt;/address&gt; 23&lt;/root&gt; r", s);
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
}
