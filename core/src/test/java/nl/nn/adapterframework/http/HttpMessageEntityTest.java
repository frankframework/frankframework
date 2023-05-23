package nl.nn.adapterframework.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.MessageTestUtils.MessageType;
import nl.nn.adapterframework.util.StreamUtil;

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
		assertEquals(-1, ise.getContentLength());
		assertEquals(repeatableMessage.size(), hmeRepeatable.getContentLength());
		assertEquals(-1, hmeNonRepeatable.getContentLength());
		assertEquals(26358, hmeUrlRepeatable.getContentLength());
	}

	@Test
	public void testRepeatability() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertEquals(true, repeatableMessage.isRepeatable());
		assertEquals(true, bae.isRepeatable());
		assertEquals(false, ise.isRepeatable());
		assertEquals(true, hmeRepeatable.isRepeatable());
		assertEquals(false, hmeNonRepeatable.isRepeatable());
		assertEquals(true, hmeUrlRepeatable.isRepeatable());
	}

	@Test
	public void testStreaming() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(repeatableMessage.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(repeatableMessage.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(repeatableMessage);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(nonRepeatableMessage);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(binaryMessage);

		assertEquals(false, repeatableMessage.requiresStream());
		assertEquals(false, bae.isStreaming());
		assertEquals(true, ise.isStreaming());
		assertEquals(false, hmeRepeatable.isStreaming());
		assertEquals(true, hmeNonRepeatable.isStreaming());
		assertEquals(true, hmeUrlRepeatable.isStreaming());
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
		entity.setContentEncoding((String)null);
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

	@ParameterizedTest
	@EnumSource(value = MessageType.class)
	public void testWriteToCharacterData(MessageType type) throws Exception {
		Message message = MessageTestUtils.getNonRepeatableMessage(type);
		message.preserve();

		HttpMessageEntity entity = new HttpMessageEntity(message);

		if(type.equals(MessageType.BINARY)) {
			assertNull(message.getCharset());
			assertNull(entity.getContentEncoding());
		} else {
			assertEquals(message.getCharset(), entity.getContentEncoding().getValue());
		}

		// Act
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);

		// Assert
		String boasString = type.equals(MessageType.BINARY) ? boas.toString(): boas.toString(message.getCharset());
		assertEquals(message.asString(), boasString);
	}

	private String toString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);
		return boas.toString().replace("\ufeff", ""); //remove BOM if present
	}
}
