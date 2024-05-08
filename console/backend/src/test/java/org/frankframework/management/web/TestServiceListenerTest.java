package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.frankframework.management.bus.message.MessageBase;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class TestServiceListenerTest extends FrankApiTestBase<TestServiceListener>{

	@Override
	public TestServiceListener createJaxRsResource() {
		return new TestServiceListener();
	}

	@Test
	public void testWrongEncoding() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("service", "dummyService123"));
		attachments.add(new StringAttachment("encoding", "fakeEncoding"));
		attachments.add(new StringAttachment("message", "inputMessage"));

		ApiException e = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/test-servicelistener", attachments));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());
	}

	@Test
	public void testFileWrongEncoding() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("service", "dummyService123"));
		attachments.add(new StringAttachment("encoding", "fakeEncoding"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		ApiException e = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/test-servicelistener", attachments));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());
	}

	@Test
	public void testMessageServiceListeners() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("service", "dummyService123"));
		attachments.add(new StringAttachment("message", "inputMessage"));

		doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("SERVICE_LISTENER", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-servicelistener", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}

	@Test
	public void testFileServiceListeners() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("service", "dummyService123"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("SERVICE_LISTENER", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/test-servicelistener", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}
}
