package org.frankframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.util.StreamUtil;

public class BusResponseTypesTest {

	@Test
	public void testBytes() throws Exception {
		BinaryResponseMessage message = new BinaryResponseMessage("binary".getBytes());
		message.setStatus(300);
		String result = StreamUtil.streamToString(message.getPayload());
		assertEquals("binary", result);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(message, ResponseMessageBase.MIMETYPE_KEY));
		assertEquals(300, BusMessageUtils.getIntHeader(message, ResponseMessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testInputStream() throws Exception {
		ByteArrayInputStream stream = new ByteArrayInputStream("binary".getBytes());
		BinaryResponseMessage message = new BinaryResponseMessage(stream);
		message.setStatus(503);
		String result = StreamUtil.streamToString(message.getPayload());
		assertEquals("binary", result);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(message, ResponseMessageBase.MIMETYPE_KEY));
		assertEquals(503, BusMessageUtils.getIntHeader(message, ResponseMessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testString() {
		StringResponseMessage message = new StringResponseMessage("json");
		assertEquals("json", message.getPayload());
		assertEquals("text/plain", BusMessageUtils.getHeader(message, ResponseMessageBase.MIMETYPE_KEY));
		assertEquals(200, BusMessageUtils.getIntHeader(message, ResponseMessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testJson() {
		JsonResponseMessage message = new JsonResponseMessage("json");
		assertEquals("\"json\"", message.getPayload());
		assertEquals("application/json", BusMessageUtils.getHeader(message, ResponseMessageBase.MIMETYPE_KEY));
		assertEquals(200, BusMessageUtils.getIntHeader(message, ResponseMessageBase.STATUS_KEY, 0));
	}

	@Test
	public void testUnableToConvertPayloadToJson() {
		ByteArrayInputStream stream = new ByteArrayInputStream("binary".getBytes());

		BusException e = assertThrows(BusException.class, () -> new JsonResponseMessage(stream));
		assertThat(e.getMessage(), Matchers.startsWith("unable to convert response to JSON: (InvalidDefinitionException) No serializer found for class java.io.ByteArrayInputStream"));
	}

	@Test
	public void testInvalidStatusCode() {
		StringResponseMessage message = new StringResponseMessage("dummy input");
		assertAll(
			() -> assertThrows(IllegalArgumentException.class, ()->message.setStatus(600)),
			() -> assertThrows(IllegalArgumentException.class, ()->message.setStatus(100))
		);
	}

	@Test
	public void testContentDisposition() {
		StringResponseMessage message = new StringResponseMessage("dummy input");
		message.setFilename("testnaam");
		message.setHeader("test123", "dummy");
		assertEquals("dummy", BusMessageUtils.getHeader(message, "test123"));
		assertEquals("attachment; filename=\"testnaam\"", BusMessageUtils.getHeader(message, ResponseMessageBase.CONTENT_DISPOSITION_KEY));
	}

	@Test
	public void testEmptyResponseMessage() {
		EmptyResponseMessage created = EmptyResponseMessage.created();
		assertEquals(201, BusMessageUtils.getIntHeader(created, ResponseMessageBase.STATUS_KEY, 0));
		EmptyResponseMessage accepted = EmptyResponseMessage.accepted();
		assertEquals(202, BusMessageUtils.getIntHeader(accepted, ResponseMessageBase.STATUS_KEY, 0));
		EmptyResponseMessage noContent = EmptyResponseMessage.noContent();
		assertEquals(204, BusMessageUtils.getIntHeader(noContent, ResponseMessageBase.STATUS_KEY, 0));
	}
}
