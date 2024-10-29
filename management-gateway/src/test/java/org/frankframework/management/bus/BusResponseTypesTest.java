package org.frankframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.util.StreamUtil;

public class BusResponseTypesTest {

	@Test
	public void testBytes() throws Exception {
		BinaryMessage message = new BinaryMessage("binary".getBytes());
		message.setStatus(300);
		String result = StreamUtil.streamToString(message.getPayload());
		assertEquals("binary", result);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY));
		assertEquals(300, BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testInputStream() throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream("binary".getBytes());
		BinaryMessage message = new BinaryMessage(stream);
		message.setStatus(503);
		String result = StreamUtil.streamToString(message.getPayload());
		assertEquals("binary", result);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY));
		assertEquals(503, BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testString() {
		StringMessage message = new StringMessage("json");
		assertEquals("json", message.getPayload());
		assertEquals("text/plain", BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY));
		assertEquals(200, BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testJson() {
		JsonMessage message = new JsonMessage("json");
		assertEquals("\"json\"", message.getPayload());
		assertEquals("application/json", BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY));
		assertEquals(200, BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testUnableToConvertPayloadToJson() {
		ByteArrayInputStream stream = new ByteArrayInputStream("binary".getBytes());

		BusException e = assertThrows(BusException.class, () -> new JsonMessage(stream));
		assertThat(e.getMessage(), Matchers.startsWith("unable to convert response to JSON: (InvalidDefinitionException) No serializer found for class java.io.ByteArrayInputStream"));
	}

	@Test
	public void testJsonStructure() {
		JsonObjectBuilder json = Json.createObjectBuilder();
		json.add("key", "value");
		JsonMessage message = new JsonMessage(json.build());

		assertEquals("{\"key\":\"value\"}", message.getPayload().replaceAll("\s", "").replaceAll("\n", ""));
		assertEquals("application/json", BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY));
		assertEquals(200, BusMessageUtils.getIntHeader(message, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testInvalidStatusCode() {
		StringMessage message = new StringMessage("dummy input");
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, ()->message.setStatus(600)),
			() -> assertThrows(IllegalArgumentException.class, ()->message.setStatus(100))
		);
	}

	@Test
	public void testContentDisposition() {
		StringMessage message = new StringMessage("dummy input");
		message.setFilename("testnaam");
		message.setHeader("test123", "dummy");
		assertEquals("dummy", BusMessageUtils.getHeader(message, "test123"));
		assertEquals("attachment; filename=\"testnaam\"", BusMessageUtils.getHeader(message, MessageBase.CONTENT_DISPOSITION_KEY));
	}

	@Test
	public void testEmptyResponseMessage() {
		EmptyMessage created = EmptyMessage.created();
		assertEquals(201, BusMessageUtils.getIntHeader(created, MessageBase.STATUS_KEY, 0));
		EmptyMessage accepted = EmptyMessage.accepted();
		assertEquals(202, BusMessageUtils.getIntHeader(accepted, MessageBase.STATUS_KEY, 0));
		EmptyMessage noContent = EmptyMessage.noContent();
		assertEquals(204, BusMessageUtils.getIntHeader(noContent, MessageBase.STATUS_KEY, 0));
	}
}
