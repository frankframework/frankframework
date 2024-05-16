package org.frankframework.management.web.spring;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfig.class, SendJmsMessage.class})
public class SendJmsMessageTest extends FrankApiTestBase {
	@Test
	public void testWrongEncoding() throws Exception {
		mockMvc.perform(
						MockMvcRequestBuilders
								.multipart("/jms/message")
								.file(createMockMultipartFile("message", null, "<dummy-message />".getBytes()))
								.part(
										new MockPart("persistent", "false".getBytes()),
										new MockPart("synchronous", "true".getBytes()),
										new MockPart("lookupDestination", "false".getBytes()),
										new MockPart("destination", "some-queue".getBytes()),
										new MockPart("type", "type".getBytes()),
										new MockPart("connectionFactory", "qcf/connectionFactory".getBytes()),
										new MockPart("encoding", "fakeEncoding".getBytes())
								)
								.characterEncoding("UTF-8")
				).andDo(print())
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
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
