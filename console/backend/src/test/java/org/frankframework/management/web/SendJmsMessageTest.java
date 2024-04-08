package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.frankframework.management.bus.message.MessageBase;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public class SendJmsMessageTest extends FrankApiTestBase<SendJmsMessage>{

	@Override
	public SendJmsMessage createJaxRsResource() {
		return new SendJmsMessage();
	}

	@Test
	public void testWrongEncoding() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new StringAttachment("destination", "some-queue"));
		attachments.add(new StringAttachment("type", "type"));
		attachments.add(new StringAttachment("synchronous", "true"));
		attachments.add(new StringAttachment("encoding", "fakeEncoding"));
		attachments.add(new StringAttachment("message", "inputMessage"));

		ApiException e = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());
	}

	@Test
	public void testFileWrongEncoding() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new StringAttachment("destination", "some-queue"));
		attachments.add(new StringAttachment("type", "type"));
		attachments.add(new StringAttachment("synchronous", "true"));
		attachments.add(new StringAttachment("encoding", "fakeEncoding"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		ApiException e = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());
	}

	@Test
	public void testWithMessage() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new StringAttachment("destination", "some-queue"));
		attachments.add(new StringAttachment("type", "type"));
		attachments.add(new StringAttachment("synchronous", "true"));
		attachments.add(new StringAttachment("message", "inputMessage"));

		doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("QUEUE", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}

	@Test
	public void testWithFile() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new StringAttachment("destination", "some-queue"));
		attachments.add(new StringAttachment("type", "type"));
		attachments.add(new StringAttachment("synchronous", "true"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("QUEUE", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}
}
