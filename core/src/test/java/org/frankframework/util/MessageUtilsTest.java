package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MimeType;

import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.LargeStructuredMockData;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public class MessageUtilsTest {

	public static final String JSON_TEST_INPUT = "{\"GUID\": \"ABC\"}";

	@Test
	public void testCharset() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCharacterEncoding("tja"); //overridden by content-type header
		request.setContentType("application/xml;  charset=utf-8"); //2 spaces
		request.setContent("request".getBytes());
		MessageContext context = MessageUtils.getContext(request);

		assertEquals("UTF-8", context.get(MessageContext.METADATA_CHARSET));
		assertEquals((long) 7, context.get(MessageContext.METADATA_SIZE));
		assertEquals(MimeType.valueOf("application/xml; charset=UTF-8"), context.get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testCharsetUtf8() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCharacterEncoding("tja"); //overridden by content-type header
		request.setContentType("application/xml;  charset=utf-16"); //2 spaces
		request.setContent("request".getBytes());
		MessageContext context = MessageUtils.getContext(request);

		assertEquals("UTF-16", context.get(MessageContext.METADATA_CHARSET));
		assertEquals((long) 7, context.get(MessageContext.METADATA_SIZE));
		assertEquals(MimeType.valueOf("application/xml; charset=UTF-16"), context.get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testMd5Hash() {
		Message message = new Message("test message");

		String hash = MessageUtils.generateMD5Hash(message);

		assertNotNull(hash);
		assertEquals(MessageUtils.generateMD5Hash(message), hash, "hash should be the same");
		assertEquals("c72b9698fa1927e1dd12d3cf26ed84b2", hash);
	}

	@Test
	public void testCRC32Checksum() {
		Message message = new Message("test message");

		Long checksum = MessageUtils.generateCRC32(message);

		assertNotNull(checksum);
		assertEquals(MessageUtils.generateCRC32(message), checksum, "Checksum should be the same");
		assertEquals(529295243, checksum);
	}

	@Test
	public void testCalculateSize() throws Exception {
		// getNonRepeatableMessage turns this into a reader, thus requiring charset decoding, the result is stored as UTF8
		Message message = Mockito.spy(MessageTestUtils.getNonRepeatableMessage(MessageTestUtils.MessageType.CHARACTER_ISO88591));

		// Act
		Long size = MessageUtils.computeSize(message);

		// Assert
		verify(message, times(1)).isRepeatable();
		verify(message, times(1)).preserve();
		assertEquals(1095, size);
	}

	@ParameterizedTest
	@CsvSource({"utf8-with-bom,32", "utf8-without-bom,28", "iso-8859-1,1095"})
	public void testCalculateSize(String resource, long size) throws Exception {
		Message message = MessageTestUtils.getMessage("/Util/MessageUtils/"+resource+".txt");

		// Act
		Long calculatedSize = MessageUtils.computeSize(message);

		// Assert
		assertEquals(size, calculatedSize);
		assertEquals(MessageUtils.computeSize(message), size, "computing twice should have the same result");
		assertEquals(message.getContext().get(MessageContext.METADATA_SIZE), size, "(correct) size should be in the message context");
	}

	@Test
	public void testComputeMimeTypeWithISO8559Charset() {
		URL url = ClassLoaderUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=ISO-8859-1"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testComputeMimeTypeBinaryContent() {
		URL url = ClassLoaderUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("application/pdf"), "Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]");
		assertNull(mimeType.getParameter("charset"), "Content-Type header ["+mimeType.toString()+"] may not contain a charset");
	}

	@Test
	public void testComputeMimeTypeBinaryContentTwice() {
		URL url = ClassLoaderUtils.getResourceURL("/Logging/pdf-parsed-with-wrong-charset.pdf");
		Message message = new UrlMessage(url);
		MimeType mimeType = MessageUtils.computeMimeType(message);
		MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("application/pdf"), "Content-Type header ["+mimeType.toString()+"] does not contain [application/pdf]");
		assertNull(mimeType.getParameter("charset"), "Content-Type header ["+mimeType.toString()+"] may not contain a charset");
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetAuto() {
		URL url = ClassLoaderUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "auto");

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=ISO-8859-1"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testComputeMimeTypeWithISO8559CharsetUTF8() {
		URL url = ClassLoaderUtils.getResourceURL("/Util/MessageUtils/iso-8859-1.txt");
		Message message = new UrlMessage(url);
		message.getContext().put(MessageContext.METADATA_CHARSET, "utf-8"); // Is wrong, but it's been set, so it must be used...

		MimeType mimeType = MessageUtils.computeMimeType(message);
		assertTrue(mimeType.toString().contains("text/plain"), "Content-Type header ["+mimeType.toString()+"] does not contain [text/plain]");
		assertTrue(mimeType.toString().contains("charset=UTF-8"), "Content-Type header ["+mimeType.toString()+"] does not contain correct [charset]");
	}

	@Test
	public void testMessageDataSource() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		Message message = new UrlMessage(url);
		try (MessageDataSource ds = new MessageDataSource(message)) {
			assertEquals("file.xml", ds.getName(), "filename should be the same");
			assertEquals("application/xml", ds.getContentType(),"content-type should be the same"); //determined from file extension
			assertEquals(StreamUtil.streamToString(url.openStream()), StreamUtil.streamToString(ds.getInputStream()), "contents should be the same");
			assertEquals(StreamUtil.streamToString(url.openStream()), StreamUtil.streamToString(ds.getInputStream()), "should be able to read the content twice");
		}
	}

	@Test
	public void testMessageDataSourceFromStringDataWithoutXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/file.xml");
		assertNotNull(url);
		try (Message message = new UrlMessage(url)) {
			MessageDataSource ds = new MessageDataSource(new Message(message.asString()));
			assertNull(ds.getName(), "filename is unknown");
			assertEquals("application/xml", ds.getContentType(), "content-type cannot be determined");
			assertEquals(StreamUtil.streamToString(url.openStream()), StreamUtil.streamToString(ds.getInputStream()), "contents should be the same");
			assertEquals(StreamUtil.streamToString(url.openStream()), StreamUtil.streamToString(ds.getInputStream()), "should be able to read the content twice");
		}
	}

	@Test
	public void testMessageDataSourceFromStringDataWithXmlDeclaration() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/log4j4ibis.xml");
		assertNotNull(url);
		try (Message message = new UrlMessage(url)) {
			MessageDataSource ds = new MessageDataSource(new Message(message.asString()));
			assertNull(ds.getName(), "filename is unknown");
			assertEquals("application/xml", ds.getContentType(), "content-type cannot be determined");
			assertEquals(StreamUtil.streamToString(ds.getInputStream()), StreamUtil.streamToString(url.openStream()), "contents should be the same");
			assertEquals(StreamUtil.streamToString(url.openStream()), StreamUtil.streamToString(ds.getInputStream()), "should be able to read the content twice");
		}
	}

	@Test
	public void testXmlMessage() {
		Message json = new Message("<root><child>value</child></root>");
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/xml", mimeType.toString());
	}

	@Test
	public void testJsonMessage() throws IOException {
		Message json = new Message(JSON_TEST_INPUT);
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString());
		assertEquals(JSON_TEST_INPUT, json.asString());
	}

	@Test
	public void testReaderJsonMessage() throws IOException {
		Message json = new Message(new StringReader(JSON_TEST_INPUT));
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString());
		assertEquals(JSON_TEST_INPUT, json.asString());
	}

	@Test
	public void testReaderHugeJsonMessage() throws IOException {
		Message json = new Message(LargeStructuredMockData.getLargeJsonDataReader(500_000_000L));
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString());

		String expectedPeek = LargeStructuredMockData.DEFAULT_JSON_OPENING_BLOCK + LargeStructuredMockData.DEFAULT_JSON_REPEATED_BLOCK;
		String actualPeek = json.peek(expectedPeek.length());

		assertEquals(expectedPeek, actualPeek);
	}

	@Test
	public void testInputStreamJsonMessage() throws IOException {
		Message json = new Message(new ByteArrayInputStream(JSON_TEST_INPUT.getBytes()));
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString());
		assertEquals(JSON_TEST_INPUT, json.asString());
	}

	@Test
	public void testInputStreamHugeJsonMessage() throws IOException {
		Message json = new Message(LargeStructuredMockData.getLargeJsonDataInputStream(500_000_000L, StandardCharsets.UTF_8));
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString());

		String expectedPeek = LargeStructuredMockData.DEFAULT_JSON_OPENING_BLOCK + LargeStructuredMockData.DEFAULT_JSON_REPEATED_BLOCK;
		String actualPeek = json.peek(expectedPeek.length());

		assertEquals(expectedPeek, actualPeek);
	}

	@Test
	public void testJsonMessageWithName() throws IOException {
		Message json = new Message(JSON_TEST_INPUT, new MessageContext().withName("foo.json"));
		MimeType mimeType = MessageUtils.computeMimeType(json);
		assertNotNull(mimeType);
		assertEquals("application/json", mimeType.toString()); //mime-type can be determined
		assertEquals(JSON_TEST_INPUT, json.asString());
	}

	private String getMessageHeaders(Message message) {
		return message.getContext()
				.entrySet()
				.stream()
				.map(Map.Entry::getKey)
				.filter(e -> e.startsWith(MessageContext.HEADER_PREFIX))
				.toList()
				.toString();
	}

	@Test
	public void testParseGETContent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
		request.addHeader("User-Agent", "double-o-seven");
		Message message = MessageUtils.parseContentAsMessage(request);

		assertTrue(Message.isEmpty(message));
		assertTrue(Message.isNull(message));
		assertEquals(0L, message.size());
		assertEquals("[Header.User-Agent]", getMessageHeaders(message));
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void testParseNullPOSTContent(String content) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
		request.addHeader("User-Agent", "double-o-seven");
		request.setContent(content != null ? content.getBytes() : null);
		Message message = MessageUtils.parseContentAsMessage(request);

		assertTrue(Message.isEmpty(message));
		assertTrue(Message.isNull(message));
		assertEquals(0L, message.size());
		assertEquals("[Header.User-Agent]", getMessageHeaders(message));
	}

	@Test
	public void testParsePostWithContent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
		request.setContent("data".getBytes());
		request.addHeader("User-Agent", "double-o-seven");
		Message message = MessageUtils.parseContentAsMessage(request);

		assertFalse(Message.isEmpty(message));
		assertEquals("data", message.asString());
		assertEquals(4L, message.size());
		assertEquals("[Header.User-Agent]", getMessageHeaders(message));
	}

	@Test
	public void testParsePostWithTransferEncoding() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
		request.addHeader("Transfer-Encoding", "chunked");
		request.addHeader("User-Agent", "double-o-seven");
		Message message = MessageUtils.parseContentAsMessage(request);

		assertFalse(Message.isEmpty(message));
		assertEquals("", message.asString());
		assertEquals(0L, message.size());
		assertEquals("[Header.Transfer-Encoding, Header.User-Agent]", getMessageHeaders(message));
	}

	@Test
	void testMessageAsStringDoesNotCloseMessage() throws IOException {
		// Arrange: make it an object, so method can do instanceof check
		Object msg = new Message(new StringReader("text"));

		// Act
		String content = MessageUtils.asString(msg);

		// Assert
		Message message = (Message) msg;
		assertEquals("text", message.asString());
		assertEquals("text", content);
		message.close();
	}

	@Test
	void testMessageAsStringDoesNotCloseMessageWrapper() throws IOException {
		// Arrange: make it an object, so method can do instanceof check
		Message msg = new Message(new StringReader("text"));
		Object wrapper = new MessageWrapper<Message>(msg, null, null);

		// Act
		String content = MessageUtils.asString(wrapper);

		// Assert
		MessageWrapper<Message> messageWrapper = (MessageWrapper) wrapper;
		assertEquals("text", messageWrapper.getMessage().asString());
		assertEquals("text", content);
		msg.close();
	}

	@Test
	void testAsString() throws IOException {
		assertNull(MessageUtils.asString(null));

		String expected = "testString";
		assertEquals(expected, MessageUtils.asString(expected));

		assertEquals(expected, MessageUtils.asString(new StringReader(expected)));

	}
}
