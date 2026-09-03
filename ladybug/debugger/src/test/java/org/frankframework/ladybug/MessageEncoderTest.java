package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MessageEncoder.ToStringResult;

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
}
