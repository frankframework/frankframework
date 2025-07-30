package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class StreamUtilTest {

	private static final String UTF8_EXPECTED = "ABC één euro: €1,00";
	private static final String OTHER_EXPECTED = "ABC néé hè";

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

	@Test
	public void testResourceToStringResource() throws Exception {
		URL resource = getClass().getResource("/StreamUtil/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource);
		assertEquals("<!doctype txt>this is a text file.\nnew line in the text file.", s1);
		assertFalse(s1.isEmpty());
	}

	@Test
	public void testResourceToStringForResourceEndOfLineStringXmlEncode() throws Exception {
		URL resource = getClass().getResource("/StreamUtil/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource, " newly added string ", true);
		assertEquals("&lt;!doctype txt&gt;this is a text file. newly added string new line in the text file.", s1);
	}

	@Test
	public void testResourceToStringForResourceEndOfLineString() throws Exception {
		URL resource = getClass().getResource("/StreamUtil/test_file_for_resource_to_string_misc.txt");
		String s1 = StreamUtil.resourceToString(resource, " newly added string ");
		assertEquals("<!doctype txt>this is a text file. newly added string new line in the text file.", s1);
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

	@Test
	public void testStreamToStreamForInputOutputEof() throws Exception {
		String test = "test";
		ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
		OutputStream baos = new ByteArrayOutputStream();
		StreamUtil.streamToStream(bais, baos, "\n".getBytes());
		assertEquals("test\n", baos.toString());
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
