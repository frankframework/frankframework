package org.frankframework.management.bus.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

public class RequestMessageBuilderTest {

	@Test
	void testSimpleMessage() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG);
		builder.addHeader("test1", 1);
		builder.addHeader("test2", true);
		builder.addHeader("test3", "str");
		Message<?> message = builder.build(null);

		assertNotNull(message.getPayload());
		assertNotNull(message.getHeaders());

		assertEquals("DEBUG", message.getHeaders().get("topic"));
		assertEquals("GET", message.getHeaders().get("action"));
		assertNull(message.getHeaders().get("target"));
		assertEquals(1, message.getHeaders().get("meta-test1"));
		assertEquals(true, message.getHeaders().get("meta-test2"));
		assertEquals("str", message.getHeaders().get("meta-test3"));
		assertEquals(204, message.getHeaders().get("meta-status"));
		assertEquals("text/plain", message.getHeaders().get("meta-type"));
	}

	@Test
	void testTextMessage() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG);
		builder.setPayload("test");
		Message<?> message = builder.build(null);

		assertNotNull(message.getPayload());
		assertEquals("test", message.getPayload());
		assertNotNull(message.getHeaders());

		assertEquals("DEBUG", message.getHeaders().get("topic"));
		assertEquals("GET", message.getHeaders().get("action"));
		assertNull(message.getHeaders().get("target"));
		assertEquals(200, message.getHeaders().get("meta-status"));
		assertEquals("text/plain", message.getHeaders().get("meta-type"));
	}

	@Test
	void testJsonPayload() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG, BusAction.MANAGE);
		builder.setJsonPayload(new TestObject("one", true));
		Message<?> message = builder.build(UUID.fromString("9d8f5715-2e7c-4e64-8e34-35f510c12e66"));

		assertNotNull(message.getPayload());
		assertEquals("{\"one\":\"one\",\"two\":true}", message.getPayload());
		assertNotNull(message.getHeaders());

		assertEquals("DEBUG", message.getHeaders().get("topic"));
		assertEquals("MANAGE", message.getHeaders().get("action"));
		UUID target = assertInstanceOf(UUID.class, message.getHeaders().get("target"));
		assertEquals("9d8f5715-2e7c-4e64-8e34-35f510c12e66", ""+target);
		assertEquals(200, message.getHeaders().get("meta-status"));
		assertEquals("application/json", message.getHeaders().get("meta-type"));
	}

	@Test
	void testBinaryPayload() throws IOException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG, BusAction.GET);
		builder.setPayload(new ByteArrayInputStream("test".getBytes(Charset.defaultCharset())));
		Message<?> message = builder.build(UUID.fromString("9d8f5715-2e7c-4e64-8e34-35f510c12e66"));

		InputStream input = assertInstanceOf(InputStream.class, message.getPayload());
		assertEquals("test", new String(input.readAllBytes()));
		assertNotNull(message.getHeaders());

		assertEquals("DEBUG", message.getHeaders().get("topic"));
		assertEquals("GET", message.getHeaders().get("action"));
		UUID target = assertInstanceOf(UUID.class, message.getHeaders().get("target"));
		assertEquals("9d8f5715-2e7c-4e64-8e34-35f510c12e66", ""+target);
		assertEquals(200, message.getHeaders().get("meta-status"));
		assertEquals("application/octet-stream", message.getHeaders().get("meta-type"));
	}

	@Test
	void unableToSetTopicAsHeader() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG);
		assertThrows(IllegalStateException.class, () -> builder.addHeader("topic", "aaa"));
	}

	public record TestObject(String one, boolean two) {};
}
