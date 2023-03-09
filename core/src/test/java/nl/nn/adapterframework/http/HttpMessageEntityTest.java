package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

public class HttpMessageEntityTest {
	private static final String MESSAGE_CONTENT = "dummy content here";
	private static Message REPEATABLE_MESSAGE;
	private static Message NON_REPEATABLE_MESSAGE;
	private static Message REPEATABLE_TS_MESSAGE;

	@BeforeClass
	public static void setup() throws Exception {
		REPEATABLE_MESSAGE = Message.asMessage(new Message(MESSAGE_CONTENT).asByteArray());
		NON_REPEATABLE_MESSAGE = Message.asMessage(new FilterInputStream(REPEATABLE_MESSAGE.asInputStream()) {});
		URL file = HttpMessageEntityTest.class.getResource("/file.xml");
		assertNotNull("unable to find test [file.xml]", file);
		REPEATABLE_TS_MESSAGE = new FileMessage(new File(file.toURI()));
	}

	@Test
	public void testSize() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(REPEATABLE_MESSAGE.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(REPEATABLE_MESSAGE.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(REPEATABLE_MESSAGE);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(NON_REPEATABLE_MESSAGE);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(REPEATABLE_TS_MESSAGE);

		assertEquals(MESSAGE_CONTENT.length(), REPEATABLE_MESSAGE.size());
		assertEquals(MESSAGE_CONTENT.length(), bae.getContentLength());
		assertEquals(-1, ise.getContentLength());
		assertEquals(MESSAGE_CONTENT.length(), hmeRepeatable.getContentLength());
		assertEquals(-1, hmeNonRepeatable.getContentLength());
		assertEquals(33, hmeUrlRepeatable.getContentLength());
	}

	@Test
	public void testRepeatability() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(REPEATABLE_MESSAGE.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(REPEATABLE_MESSAGE.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(REPEATABLE_MESSAGE);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(NON_REPEATABLE_MESSAGE);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(REPEATABLE_TS_MESSAGE);

		assertEquals(true, REPEATABLE_MESSAGE.isRepeatable());
		assertEquals(true, bae.isRepeatable());
		assertEquals(false, ise.isRepeatable());
		assertEquals(true, hmeRepeatable.isRepeatable());
		assertEquals(false, hmeNonRepeatable.isRepeatable());
		assertEquals(true, hmeUrlRepeatable.isRepeatable());
	}

	@Test
	public void testStreaming() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(REPEATABLE_MESSAGE.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(REPEATABLE_MESSAGE.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(REPEATABLE_MESSAGE);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(NON_REPEATABLE_MESSAGE);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(REPEATABLE_TS_MESSAGE);

		assertEquals(false, REPEATABLE_MESSAGE.requiresStream());
		assertEquals(false, bae.isStreaming());
		assertEquals(true, ise.isStreaming());
		assertEquals(false, hmeRepeatable.isStreaming());
		assertEquals(true, hmeNonRepeatable.isStreaming());
		assertEquals(true, hmeUrlRepeatable.isStreaming());
	}

	@Test
	public void testCharsetDefault() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(REPEATABLE_MESSAGE.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(REPEATABLE_MESSAGE.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(REPEATABLE_MESSAGE);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(NON_REPEATABLE_MESSAGE);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(REPEATABLE_TS_MESSAGE);

		assertNull(REPEATABLE_MESSAGE.getCharset());
		assertNull(bae.getContentEncoding());
		assertNull(ise.getContentEncoding());
		assertNull(hmeRepeatable.getContentEncoding());
		assertNull(hmeNonRepeatable.getContentEncoding());
		assertNull(hmeUrlRepeatable.getContentEncoding());
	}

	@Test
	public void testContentTypeWithCharset() throws Exception {
		Message message = new Message(REPEATABLE_MESSAGE.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.TEXT_PLAIN);

		assertNotNull("entity should set charset when available", entity.getContentEncoding());
		assertEquals("ISO-8859-1", entity.getContentEncoding().getValue());
		assertEquals(ContentType.TEXT_PLAIN.toString(), entity.getContentType().getValue());
	}

	@Test
	public void testContentTypeWithoutCharset() throws Exception {
		Message message = new Message(REPEATABLE_MESSAGE.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));

		assertNotNull("entity should set charset when available", entity.getContentEncoding());
		assertEquals("UTF-8", entity.getContentEncoding().getValue());
		assertEquals("text/plain", entity.getContentType().getValue());
	}

	@Test
	public void testMessageWithCharsetButContentEncodingSetToNull() throws Exception {
		Message message = new Message(REPEATABLE_MESSAGE.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));
		entity.setContentEncoding((String)null);
		assertNull("should not be set", entity.getContentEncoding());

		assertEquals("text/plain", entity.getContentType().getValue());
		assertEquals(MESSAGE_CONTENT, StreamUtil.streamToString(entity.getContent()));
	}

	@Test
	public void testMessageWithCharsetButContentEncodingSetToEmpty() throws Exception {
		Message message = new Message(REPEATABLE_MESSAGE.asByteArray(), "UTF-8");
		HttpMessageEntity entity = new HttpMessageEntity(message, ContentType.parse("text/plain"));
		entity.setContentEncoding("");
		assertNotNull(entity.getContentEncoding());

		assertEquals("text/plain", entity.getContentType().getValue());
		assertEquals(MESSAGE_CONTENT, StreamUtil.streamToString(entity.getContent()));
	}

	@Test
	public void testWriteTo() throws Exception {
		ByteArrayEntity bae = new ByteArrayEntity(REPEATABLE_MESSAGE.asByteArray());
		InputStreamEntity ise = new InputStreamEntity(REPEATABLE_MESSAGE.asInputStream());
		HttpMessageEntity hmeRepeatable = new HttpMessageEntity(REPEATABLE_MESSAGE);
		HttpMessageEntity hmeNonRepeatable = new HttpMessageEntity(NON_REPEATABLE_MESSAGE);
		HttpMessageEntity hmeUrlRepeatable = new HttpMessageEntity(REPEATABLE_TS_MESSAGE);

		assertEquals(MESSAGE_CONTENT, toString(bae));
		assertEquals(MESSAGE_CONTENT, toString(ise));
		assertEquals(MESSAGE_CONTENT, toString(hmeRepeatable));
		assertEquals(MESSAGE_CONTENT, toString(hmeRepeatable)); //read twice to prove repeatability
		assertEquals(MESSAGE_CONTENT, toString(hmeNonRepeatable));
		assertEquals(REPEATABLE_TS_MESSAGE.asString(), toString(hmeUrlRepeatable));
	}

	private String toString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		entity.writeTo(boas);
		return boas.toString();
	}
}
