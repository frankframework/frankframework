package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MessageEncoder.ToStringResult;

import org.frankframework.http.rest.ApiListener;
import org.frankframework.stream.Message;

public class MessageEncoderTest {

	@Test
	void testToObject() {
		Checkpoint cpt = new Checkpoint();
		cpt.setMessage("stubbed message");

		Object originalValue = "original message";
		MessageEncoder encoder = new MessageEncoder(1024);
		Object object = encoder.toObject(cpt, originalValue);
		assertEquals("stubbed message", object);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testBoolean(boolean input) {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString(input, null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("" + input, result.getString());
		assertEquals("java.lang.Boolean", result.getMessageClassName());
	}

	@ParameterizedTest
	@EnumSource(ApiListener.HttpMethod.class)
	void testBoolean(ApiListener.HttpMethod input) {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString(input, null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("" + input, result.getString());
		assertEquals("org.frankframework.http.rest.ApiListener$HttpMethod", result.getMessageClassName());
	}

	@Test
	void testNull() {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString(null, null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertNull(result.getString());
		assertNull(result.getMessageClassName());
	}

	@Test
	void testNullMessage() {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString(Message.nullMessage(), null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertNull(result.getString());
		assertNull(result.getMessageClassName());
	}

	@Test
	void testEmpty() {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString("", null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("", result.getString());
		assertNull(result.getMessageClassName());
	}

	@Test
	void testEmptyMessage() {
		MessageEncoder encoder = new MessageEncoder(1024);

		ToStringResult result = encoder.toString(new Message(""), null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("", result.getString());
		assertEquals("String", result.getMessageClassName());
	}

	@Test
	void testCharacterToString() {
		MessageEncoder encoder = new MessageEncoder(1024);
		Message message = new Message("contents");

		ToStringResult result = encoder.toString(message, null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("contents", result.getString());
		assertEquals("String", result.getMessageClassName());
	}

	@Test
	void testBinaryToString() {
		MessageEncoder encoder = new MessageEncoder(1024);
		byte[] bytes = "contents".getBytes(StandardCharsets.UTF_8);
		Message message = new Message(bytes);

		ToStringResult result = encoder.toString(message, null);

		assertNotNull(result);
		assertEquals("UTF-8", result.getEncoding());
		assertEquals("contents", result.getString());
		assertEquals("byte[]", result.getMessageClassName());
	}

	@Test
	void testMaxMessageLength() {
		MessageEncoder encoder = new MessageEncoder(13);
		Message message = new Message("contents that's more then 10 characters");

		ToStringResult result = encoder.toString(message, null);

		assertNotNull(result);
		assertNull(result.getEncoding());
		assertEquals("contents that", result.getString());
		assertEquals("String", result.getMessageClassName());
	}

	@Test
	void testNonSerializable() {
		MessageEncoder encoder = new MessageEncoder(1024);
		Object input = new Object() {};

		ToStringResult result = encoder.toString(input, null);

		assertNotNull(result);
		assertEquals("toString()", result.getEncoding());
		assertNotNull(result.getString());
	}
}
