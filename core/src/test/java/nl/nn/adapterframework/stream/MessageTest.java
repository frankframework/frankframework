/*
   Copyright 2019, 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.SerializationTester;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageTest {

	private static final boolean TEST_CDATA = true;
	private static final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private static final String CDATA_END = TEST_CDATA ? "]]>" : "";

	protected String testString = "<root><sub>abc&amp;&lt;&gt;</sub><sub>" + CDATA_START + "<a>a&amp;b</a>" + CDATA_END + "</sub><data attr=\"één €\">één €</data></root>";
	protected String testStringFile = "/Message/testString.txt";

	private SerializationTester<Message> serializationTester = new SerializationTester<Message>();

	protected void testAsInputStream(Message adapter) throws IOException {
		InputStream result = adapter.asInputStream();
		String actual = StreamUtil.streamToString(result, null, "UTF-8");
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsReader(Message adapter) throws IOException {
		Reader result = adapter.asReader();
		String actual = StreamUtil.readerToString(result, null);
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsInputSource(Message adapter) throws IOException, SAXException {
		InputSource result = adapter.asInputSource();
		XmlWriter sink = new XmlWriter();
		XmlUtils.parseXml(result, sink);

		String actual = sink.toString();
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsString(Message message) throws IOException {
		String actual = message.asString();
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsByteArray(Message adapter) throws IOException {
		byte[] actual = adapter.asByteArray();
		byte[] expected = testString.getBytes("UTF-8");
		assertEquals("lengths differ", expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) {
			assertEquals("byte arrays differ at position [" + i + "]", expected[i], actual[i]);
		}
	}

	protected void testToString(Message adapter, Class<?> clazz) {
		testToString(adapter, clazz, null);
	}

	protected void testToString(Message adapter, Class<?> clazz, Class<?> wrapperClass) {
		String actual = adapter.toString();
		System.out.println("toString [" + actual + "] class typename [" + clazz.getSimpleName() + "]");
		// remove the toStringPrefix(), if it is present
		String valuePart = actual.contains("value:\n") ? actual.split("value:\n")[1] : actual;
		assertEquals(clazz.getSimpleName(), valuePart.substring(0, valuePart.indexOf(": ")));
		if (wrapperClass == null) {
			assertEquals(adapter.getRequestClass().getName(), clazz.getName());
		} else {
			assertEquals(adapter.getRequestClass().getName(), wrapperClass.getName());
		}
	}

	@Test
	public void testInputStreamAsInputStream() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testInputStreamAsReader() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testInputStreamWithCharsetAsReader() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source, "utf-8");
		testAsReader(adapter);
	}

	@Test
	public void testInputStreamAsInputSource() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testInputStreamAsByteArray() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testInputStreamAsString() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testInputStreamToString() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testToString(adapter, ByteArrayInputStream.class);
	}

	@Test
	public void testInputStreamAsInputStreamCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsInputStream(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
		testToString(adapter, ByteArrayInputStream.class);
	}

	@Test
	public void testInputStreamAsReaderCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsReader(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamWithCharsetAsReaderCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source, "utf-8");
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsReader(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamAsInputSourceCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsInputSource(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamAsByteArrayCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsByteArray(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamAsStringCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		testAsString(adapter);

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamClosedButCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		adapter.asInputStream().close();

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testInputStreamPreservedAndCaptured() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		ByteArrayOutputStream outputStream = adapter.captureBinaryStream();
		assertNotNull(outputStream);
		adapter.preserve();

		String captured = new String(outputStream.toByteArray(), "utf-8");
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderAsInputStream() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testReaderAsReader() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testReaderAsInputSource() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testReaderAsByteArray() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testReaderAsString() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testReaderToString() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testToString(adapter, StringReader.class);
	}

	@Test
	public void testReaderAsInputStreamCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		testAsInputStream(adapter);

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderAsReaderCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		testAsReader(adapter);

		String captured = writer.toString();
		assertEquals(testString, captured);
		testToString(adapter, StringReader.class);
	}

	@Test
	public void testReaderAsInputSourceCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		testAsInputSource(adapter);

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderAsByteArrayCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		testAsByteArray(adapter);

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderAsStringCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		testAsString(adapter);

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderClosedButCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		adapter.asReader().close();

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderPreservedAndCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);
		adapter.preserve();

		String captured = writer.toString();
		assertEquals(testString, captured);
	}

	@Test
	public void testReaderOnlyCaptured() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		StringWriter writer = adapter.captureCharacterStream();
		assertNotNull(writer);

		String captured = writer.toString();
		assertEquals("", captured); // input stream is not read, so nothing is captured. Writer could detect that it was not closed, though.
	}

	@Test
	public void testStringAsInputStream() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testStringAsReader() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testStringAsInputSource() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testStringAsByteArray() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testStringAsString() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testStringToString() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testToString(adapter, String.class);
	}

	@Test
	public void testByteArrayAsInputStream() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testByteArrayAsReader() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testByteArrayAsInputSource() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testByteArrayAsByteArray() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testByteArrayAsString() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testByteArrayToString() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testToString(adapter, byte[].class);
	}

	@Test
	public void testURLAsInputStream() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownURL() throws Exception {
		String unknownFile = "xxx.bestaat.niet.txt";
		URL source = new URL("file://" + unknownFile);
		Message adapter = new UrlMessage(source);
		Exception exception = assertThrows(Exception.class, () -> { adapter.asInputStream(); });
		assertThat(exception.getMessage(), containsString(unknownFile));
	}

	@Test
	public void testURLAsReader() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testURLAsInputSource() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testURLAsByteArray() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testURLAsString() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testURLToString() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		Message adapter = new UrlMessage(source);
		testToString(adapter, URL.class);
	}

	@Test
	public void testFileAsInputStream() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownFile() throws Exception {
		String unkownfilename = new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath() + "-bestaatniet";
		File source = new File(unkownfilename);
		Message adapter = new FileMessage(source);
		Exception exception = assertThrows(FileNotFoundException.class, () -> { adapter.asInputStream(); });
		assertThat(exception.getMessage(), containsString(unkownfilename));
	}

	@Test
	public void testFileAsReader() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testFileAsInputSource() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testFileAsByteArray() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testFileAsString() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testFileToString() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		Message adapter = new FileMessage(source);
		testToString(adapter, File.class);
	}

	@Test
	public void testPathAsInputStream() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownPath() throws Exception {
		String unkownfilename = new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath() + "-bestaatniet";
		Path source = Paths.get(unkownfilename);
		Message adapter = new PathMessage(source);
		Exception exception = assertThrows(NoSuchFileException.class, () -> { adapter.asInputStream(); });
		assertThat(exception.getMessage(), containsString(unkownfilename));
	}

	@Test
	public void testPathAsReader() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testPathAsInputSource() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testPathAsByteArray() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testPathAsString() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testPathToString() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		Message adapter = new PathMessage(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testDocumentAsInputStream() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testDocumentAsReader() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testDocumentAsInputSource() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testDocumentAsByteArray() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testDocumentAsString() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testDocumentToString() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		Message adapter = new Message(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testNodeAsInputStream() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testNodeAsReader() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testNodeAsInputSource() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testNodeAsByteArray() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testNodeAsString() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testNodeToString() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		Message adapter = new Message(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testSerializeWithString() throws Exception {
		String source = testString;
		Message in = new Message(source);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testSerializeWithByteArray() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message in = new Message(source);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testSerializeWithReader() throws Exception {
		Reader source = new StringReader(testString);
		Message in = new Message(source);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testSerializeWithInputStream() throws Exception {
		InputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message in = new Message(source);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testSerializeWithFile() throws Exception {
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();
		File source = folder.newFile();
		writeContentsToFile(source, testString);

		Message in = new FileMessage(source);
		byte[] wire = serializationTester.serialize(in);
		writeContentsToFile(source, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testSerializeWithURL() throws Exception {
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();
		File file = folder.newFile();
		writeContentsToFile(file, testString);
		URL source = file.toURI().toURL();

		Message in = new UrlMessage(source);
		byte[] wire = serializationTester.serialize(in);
		writeContentsToFile(file, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
	}

	private void writeContentsToFile(File file, String contents) throws IOException {
		try (Writer fw = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
			fw.write(contents);
		}
	}

	@Test
	public void testMessageSizeString() {
		Message message = Message.asMessage("string");
		assertEquals("size differs or could not be determined", 6, message.size());
	}

	@Test
	public void testMessageSizeByteArray() {
		Message message = Message.asMessage("string".getBytes());
		assertEquals("size differs or could not be determined", 6, message.size());
	}

	@Test
	public void testMessageSizeFileInputStream() throws Exception {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull("cannot find testfile", url);

		File file = new File(url.toURI());
		FileInputStream fis = new FileInputStream(file);
		Message message = Message.asMessage(fis);
		assertEquals("size differs or could not be determined", 33, message.size());
	}

	@Test
	public void testMessageSizeFile() throws Exception {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull("cannot find testfile", url);

		File file = new File(url.toURI());
		Message message = Message.asMessage(file);
		assertEquals("size differs or could not be determined", 33, message.size());
	}

	@Test
	public void testMessageSizeURL() {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull("cannot find testfile", url);

		Message message = Message.asMessage(url);
		assertEquals("size differs or could not be determined", -1, message.size());
	}

	@Test
	public void testNullMessageSize() {
		Message message = Message.nullMessage();
		assertEquals(0, message.size());
	}

	@Test
	public void testMessageSizeExternalURL() throws Exception {
		URL url = new URL("http://www.file.xml");
		assertNotNull("cannot find testfile", url);

		Message message = Message.asMessage(url);
		assertEquals(-1, message.size());
	}

	@Test
	public void testMessageSizeReader() throws Exception {
		Message message = new Message(new StringReader("string"));
		assertEquals("size differs or could not be determined", -1, message.size());
	}

	@Test
	public void testMessageIsEmpty() {
		Message message = Message.nullMessage();
		assertTrue(message.isEmpty());
		assertTrue(Message.isEmpty(message));
	}

	@Test
	public void testNullMessageIsEmpty() {
		Message message = null;
		assertTrue(Message.isEmpty(message));
	}
}
