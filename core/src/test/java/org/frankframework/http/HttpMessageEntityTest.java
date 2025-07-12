package org.frankframework.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;
import org.frankframework.util.StreamUtil;

public class HttpMessageEntityTest {
	private String messageContent;
	private Message repeatableMessage;
	private Message nonRepeatableMessage;
	private Message binaryMessage;

	@BeforeEach
	public void setup() throws Exception {
		messageContent = MessageTestUtils.getMessage(MessageType.CHARACTER_UTF8).asString();
		repeatableMessage = MessageTestUtils.getMessage(MessageType.CHARACTER_UTF8);
		nonRepeatableMessage = MessageTestUtils.getNonRepeatableMessage(MessageType.CHARACTER_UTF8);
		binaryMessage = MessageTestUtils.getMessage(MessageType.BINARY);
	}

	@Test
	public void testSize() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertEquals(repeatableMessage.size(), bae.getContentLength());
		assertEquals(-1L, ise.getContentLength());
		assertEquals(repeatableMessage.size(), hmeRepeatable.getContentLength());
		assertEquals(-1L, hmeNonRepeatable.getContentLength());
		assertEquals(26358, hmeUrlRepeatable.getContentLength());
	}

	@Test
	public void testRepeatability() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertTrue(bae.isRepeatable());
		assertFalse(ise.isRepeatable());
		assertTrue(hmeRepeatable.isRepeatable());
		assertTrue(hmeNonRepeatable.isRepeatable());
		assertTrue(hmeUrlRepeatable.isRepeatable());
	}

	@Test
	public void testStreaming() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertTrue(repeatableMessage.requiresStream());
		assertFalse(bae.isStreaming());
		assertTrue(ise.isStreaming());
		assertTrue(hmeRepeatable.isStreaming());
		assertTrue(hmeNonRepeatable.isStreaming());
		assertTrue(hmeUrlRepeatable.isStreaming());
	}

	@Test
	public void testCharsetDefault() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertNull(repeatableMessage.getCharset());
		assertNull(bae.getContentEncoding());
		assertNull(ise.getContentEncoding());
		assertNull(hmeRepeatable.getContentEncoding());
		assertNotNull(hmeNonRepeatable.getContentEncoding()); //Message is already read asReader with DETECT charset
		assertNull(hmeUrlRepeatable.getContentEncoding());
	}

	@Test
	public void testContentTypeWithCharset() throws Exception {
		Message message = new Message(repeatableMessage.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.TEXT_PLAIN);

		assertNotNull(entity.getContentEncoding(), "entity should set charset when available");
		assertEquals("ISO-8859-1", entity.getContentEncoding().getValue());
		assertEquals(ContentType.TEXT_PLAIN.toString(), entity.getContentType().getValue());
	}

	@Test
	public void testContentTypeWithoutCharset() throws Exception {
		Message message = new Message(repeatableMessage.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));

		assertNotNull(entity.getContentEncoding(), "entity should set charset when available");
		assertEquals("UTF-8", entity.getContentEncoding().getValue());
		assertEquals("text/plain", entity.getContentType().getValue());
	}

	@Test
	public void testMessageWithCharsetButContentEncodingSetToNull() throws Exception {
		Message message = new Message(repeatableMessage.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));
		entity.setContentEncoding((String) null);
		assertNull(entity.getContentEncoding(), "should not be set");

		assertEquals("text/plain", entity.getContentType().getValue());
		assertEquals(messageContent, StreamUtil.streamToString(entity.getContent()));
	}

	@Test
	public void testMessageWithCharsetButContentEncodingSetToEmpty() throws Exception {
		Message message = new Message(repeatableMessage.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));
		entity.setContentEncoding("");
		assertNotNull(entity.getContentEncoding());

		assertEquals("text/plain", entity.getContentType().getValue());
		assertEquals(messageContent, StreamUtil.streamToString(entity.getContent()));
	}

	@Test
	public void testWriteTo() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertEquals(messageContent, toString(bae));
		assertEquals(messageContent, toString(ise));
		assertEquals(messageContent, toString(hmeRepeatable));
		assertEquals(messageContent, toString(hmeRepeatable)); //read twice to prove repeatability
		assertEquals(messageContent, toString(hmeNonRepeatable));
		assertEquals(binaryMessage.asString(), toString(hmeUrlRepeatable));
	}

	public static Stream<Arguments> testWriteToCharacterData() {
		return Stream.of(
				Arguments.of(MessageType.CHARACTER_UTF8),
				Arguments.of(MessageType.CHARACTER_ISO88591),
				Arguments.of(MessageType.BINARY)
			);
	}

	// see MessageContentBody
	@ParameterizedTest
	@MethodSource
	public void testWriteToCharacterData(MessageType type) throws Exception {
		Message message = MessageTestUtils.getNonRepeatableMessage(type);

		HttpMessageEntity entity = new HttpMessageEntity(message);

		if(type == MessageType.BINARY) {
			assertNull(message.getCharset());
			assertNull(entity.getContentEncoding());
		} else {
			assertNotNull(message.getCharset());
			assertEquals(message.getCharset(), entity.getContentEncoding().getValue());
		}

		// Act
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);

		// Assert
		if(type == MessageType.BINARY) {
			assertEquals(entity.getContentLength(), boas.toByteArray().length);
		} else {
			// Check again after reading, expect to be now known
			assertNotEquals(-1L, entity.getContentLength());
		}
		String boasString = type == MessageType.BINARY ? boas.toString(): boas.toString(message.getCharset());
		assertEquals(type.getMessage().asString(StreamUtil.AUTO_DETECT_CHARSET), boasString);
	}

	private String toString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);
		return boas.toString().replace("\ufeff", ""); //remove BOM if present
	}
}
