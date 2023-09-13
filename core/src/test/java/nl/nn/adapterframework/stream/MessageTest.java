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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.SerializationTester;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;
import nl.nn.credentialprovider.util.Misc;

public class MessageTest {
	protected Logger log = LogUtil.getLogger(this);

	private static final boolean TEST_CDATA = true;
	private static final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private static final String CDATA_END = TEST_CDATA ? "]]>" : "";

	public static String testString = "<root><sub>abc&amp;&lt;&gt;</sub><sub>" + CDATA_START + "<a>a&amp;b</a>" + CDATA_END + "</sub><data attr=\"één €\">één €</data></root>";
	public static String testStringFile = "/Message/testString.txt";

	private String characterWire76 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078";
	private String binaryWire76 =    "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078";

	private String characterWire77 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078";
	private String binaryWire77 =    "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078";

	private String[][] characterWires = {
		{ "7.6", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078" },
		{ "7.7", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078" },
		// between 2021-12-07 and 2022-04-06 the serialVersionUID of Message was removed, causing unsolvable deserialization problems.
		//{ "7.7 2021-12-07 2022-04-06", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765979c61c930446c0e0300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078" },
		{ "7.7 2021-06-04", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e707078" },
		{ "7.7 2021-02-02", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300024c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78" },
		{ "7.8 2021-04-20", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7400106a6176612e6c616e672e537472696e6778" },
	};
	private String[][] binaryWires = {
			{ "7.6", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078" },
			{ "7.7", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078" },
			// between 2021-12-07 and 2022-04-06 the serialVersionUID of Message was removed, causing unsolvable deserialization problems.
//			{ "7.7 2021-12-07 2022-04-06", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765979c61c930446c0e0300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078" },
			{ "7.7 2021-06-04", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e707078" },
			{ "7.7 2021-02-02", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300024c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78" },
			{ "7.8 2021-04-20", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d78" },
		};

	private SerializationTester<Message> serializationTester=new SerializationTester<Message>();

	protected void testAsInputStream(Message message) throws IOException {
		byte[] header = message.getMagic(6);
		assertEquals("<root>", new String(header));

		InputStream result = message.asInputStream();
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
		byte[] header = message.getMagic(6);
		assertEquals("<root>", new String(header));

		String actual = message.asString();
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsByteArray(Message message) throws IOException {
		byte[] header = message.getMagic(6);
		assertEquals("<root>", new String(header));

		byte[] actual = message.asByteArray();
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
		valuePart = valuePart.replaceAll("\\sMessage\\[[a-fA-F0-9]+\\]", "");
		assertEquals(clazz.getSimpleName(), valuePart.substring(0, valuePart.indexOf(": ")));
		if (wrapperClass == null) {
			assertEquals(clazz.getSimpleName(), adapter.getRequestClass());
		} else {
			assertEquals(wrapperClass.getSimpleName(), adapter.getRequestClass());
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
		Exception exception = assertThrows(NoSuchFileException.class, () -> { adapter.asInputStream(); });
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

	public static void writeContentsToFile(File file, String contents) throws IOException {
		try (Writer fw = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
			fw.write(contents);
		}
	}

	@Test
	public void testDeserialization76CompatibilityWithString() throws Exception {
//		String source = testString;
//		Message in = new Message(source);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire76);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString,out.asString());
	}

	@Test
	public void testDeserialization76CompatibilityWithByteArray() throws Exception {
//		byte[] source = testString.getBytes("utf-8");
//		Message in = new Message(source, "UTF-8");
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire76);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals("UTF-8", out.getCharset());
		assertEquals(testString,out.asString());
	}

	@Test
	public void testDeserialization77CompatibilityWithString() throws Exception {
//		String source = testString;
//		Message in = new Message(source);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire77);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString,out.asString());
	}

	@Test
	public void testDeserialization77CompatibilityWithByteArray() throws Exception {
//		byte[] source = testString.getBytes("utf-8");
//		Message in = new Message(source, "UTF-8");
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire77);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals("UTF-8", out.getCharset());
		assertEquals(testString,out.asString());
	}

	@Test
	public void testDeserializationCompatibilityWithString() throws Exception {

		for (int i=0; i< characterWires.length; i++) {
			String label = characterWires[i][0];
			log.debug("testDeserializationCompatibilityWithString() "+label);
			byte[] wire = Hex.decodeHex(characterWires[i][1]);
			Message out = serializationTester.deserialize(wire);

			assertFalse(label, out.isBinary());
			assertEquals(label, testString,out.asString());
		}
	}

	@Test
	public void testDeserializationCompatibilityWithByteArray() throws Exception {

		for (int i=0; i< binaryWires.length; i++) {
			String label = binaryWires[i][0];
			log.debug("testDeserializationCompatibilityWithByteArray() "+label);
			byte[] wire = Hex.decodeHex(binaryWires[i][1]);
			Message out = serializationTester.deserialize(wire);

			assertTrue(label, out.isBinary());
			assertEquals(label, "UTF-8", out.getCharset());
			assertEquals(label, testString,out.asString());
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

	@Test
	public void testMessageDefaultCharset() throws Exception {
		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		ByteArrayInputStream source = new ByteArrayInputStream(utf8Input.getBytes("utf-8"));
		Message binaryMessage = new Message(source); //non-repeatable stream, no provided charset

		assertEquals(utf8Input, binaryMessage.asString()); //Default must be used

		Message characterMessage = new Message(utf8Input);

		assertEquals(utf8Input, characterMessage.asString("ISO-8859-1")); //This should not be used as there is no binary conversion
	}

	@Test
	public void testMessageDetectCharset() throws Exception {
		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		ByteArrayInputStream source = new ByteArrayInputStream(utf8Input.getBytes("utf-8"));
		Message message = new Message(source, "auto"); //Set the MessageContext charset

		String stringResult = message.asString("ISO-8859-ik-besta-niet"); //use MessageContext charset
		assertEquals(utf8Input, stringResult);
	}

	@Test
	public void testMessageDetectCharsetISO8859() throws Exception {
		URL isoInputFile = TestFileUtils.getTestFileURL("/Util/MessageUtils/iso-8859-1.txt");
		assertNotNull("unable to find isoInputFile", isoInputFile);

		Message message = new UrlMessage(isoInputFile); //repeatable stream, detect charset

		String stringResult = message.asString("auto"); //detect when reading
		assertEquals(Misc.streamToString(isoInputFile.openStream(), "ISO-8859-1"), stringResult);
	}

	@Test
	public void testCharsetDeterminationAndFallbackToDefault() throws Exception {
		Message messageNullCharset = new Message((byte[]) null) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return null;
			};
		};
		Message messageAutoCharset = new Message((byte[]) null) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return "AUTO";
			};
		};

		// getCharset()==null && defaultDecodingCharset==AUTO ==> decodingCharset = UTF-8
		assertEquals("UTF-8", messageNullCharset.computeDecodingCharset("AUTO"));

		// getCharset()==AUTO && defaultDecodingCharset==xyz ==> decodingCharset = xyz
		assertEquals("ISO-8559-15", messageAutoCharset.computeDecodingCharset("ISO-8559-15"));

		// getCharset()==AUTO && defaultDecodingCharset==AUTO ==> decodingCharset = UTF-8
		assertEquals("UTF-8", messageAutoCharset.computeDecodingCharset("AUTO"));

		// getCharset()==AUTO && defaultDecodingCharset==null ==> decodingCharset = UTF-8
		assertEquals("UTF-8", messageAutoCharset.computeDecodingCharset(null));
	}

	@Test
	public void shouldOnlyDetectCharsetOnce() throws Exception {
		Message message = new Message("’•†™".getBytes("cp-1252")) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return "AUTO";
			};
		};

		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%m").build();
		TestAppender.addToRootLogger(appender);

		try {
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();

			int i = 0;
			for (String log : appender.getLogLines()) {
				if(log.contains("unable to detect charset for message")) {
					i++;
				}
			}
			assertEquals("charset should be determined only once", 1, i);
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testCopyMessage1() throws IOException {
		// Arrange
		Message msg1 = Message.asMessage("a");

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertNull(msg1.asObject());
		Assertions.assertNotNull(msg2.asObject());
		Assertions.assertEquals("a", msg2.asString());
	}

	@Test
	public void testCopyMessage2() throws IOException {
		// Arrange
		Message msg1 = Message.asMessage(new StringReader("á"));

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertNull(msg1.asObject());
		Assertions.assertNotNull(msg2.asObject());
		Assertions.assertEquals("á", msg2.asString());
	}

	@Test
	public void testCopyMessage3() throws IOException {
		// Arrange
		Message msg1 = Message.asMessage(new ByteArrayInputStream("á".getBytes()));

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertNull(msg1.asObject());
		Assertions.assertNotNull(msg2.asObject());
		Assertions.assertEquals("á", msg2.asString());
	}
}
