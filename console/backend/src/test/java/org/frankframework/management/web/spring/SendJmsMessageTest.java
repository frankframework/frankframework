package org.frankframework.management.web.spring;


import org.frankframework.management.bus.message.MessageBase;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ContextConfiguration(classes = {WebTestConfig.class, SendJmsMessage.class})
public class SendJmsMessageTest extends FrankApiTestBase {
	@Test
	public void testWrongEncoding() throws Exception {
//		List<Attachment> attachments = new ArrayList<>();
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("connectionFactory", "qcf/connectionFactory"));
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("destination", "some-queue"));
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("type", "type"));
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("synchronous", "true"));
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("encoding", "fakeEncoding"));
//		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("message", "inputMessage"));

//		ApiException e = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments));
		mockMvc.perform(
				MockMvcRequestBuilders
						.multipart("/jms/message")
						.file(createMockMultipartFile("message", null, "inputMessage".getBytes()))
						.content(asJsonString(new SendJmsMessage.JmsMessageMultiPartBody(
								false,
								true,
								false,
								"some-queue",
								null,
								null,
								"type",
								"qcf/connectionFactory",
								"fakeEncoding",
								null,
								null
						)))).andDo(print());
		/*ApiException e = assertThrows(ApiException.class, () -> mockMvc.perform(
				MockMvcRequestBuilders
						.multipart("/jms/message")
						.file(createMockMultipartFile("message", null, "inputMessage".getBytes()))
						.content(asJsonString(new SendJmsMessage.JmsMessageMultiPartBody(
								false,
								true,
								false,
								"some-queue",
								null,
								null,
								"type",
								"qcf/connectionFactory",
								"fakeEncoding",
								null,
								null
						)))
		));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());*/
	}

	/*@Test
	public void testFileWrongEncoding() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("destination", "some-queue"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("type", "type"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("synchronous", "true"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("encoding", "fakeEncoding"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		org.frankframework.management.web.ApiException e = assertThrows(ApiException.class, () -> dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments));
		assertEquals("unsupported file encoding [fakeEncoding]", e.getMessage());
	}

	@Test
	public void testWithMessage() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("destination", "some-queue"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("type", "type"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("synchronous", "true"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("message", "inputMessage"));

		doAnswer(i -> {
			org.frankframework.management.web.RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("QUEUE", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(org.frankframework.management.web.RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}

	@Test
	public void testWithFile() {
		List<Attachment> attachments = new ArrayList<>();
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("connectionFactory", "qcf/connectionFactory"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("destination", "some-queue"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("type", "type"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.StringAttachment("synchronous", "true"));
		attachments.add(new org.frankframework.management.web.FrankApiTestBase.FileAttachment("file", new ByteArrayInputStream("inputMessage".getBytes()), "script.xml"));

		doAnswer(i -> {
			org.frankframework.management.web.RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("QUEUE", headers.get("topic"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/jms/message", attachments);
		assertEquals("\"inputMessage\"", response.getEntity().toString());
	}*/
}
