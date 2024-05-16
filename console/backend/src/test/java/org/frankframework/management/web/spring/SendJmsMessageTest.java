package org.frankframework.management.web.spring;

import org.frankframework.management.bus.message.MessageBase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ContextConfiguration(classes = WebTestConfig.class)
public class SendJmsMessageTest extends FrankApiTestBase {

	@InjectMocks
	private SendJmsMessage sendJmsMessage;

	@Override
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(sendJmsMessage).build();
	}

	@Test
	public void testWrongEncoding() throws Exception {
		mockMvc.perform(
						MockMvcRequestBuilders
								.multipart("/jms/message")
								.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
								.part(
										new MockPart("persistent", "false".getBytes()),
										new MockPart("synchronous", "true".getBytes()),
										new MockPart("lookupDestination", "false".getBytes()),
										new MockPart("destination", "some-queue".getBytes()),
										new MockPart("type", "type".getBytes()),
										new MockPart("connectionFactory", "qcf/connectionFactory".getBytes()),
										new MockPart("encoding", "fakeEncoding".getBytes())
								)
				)
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testFileWrongEncoding() throws Exception {
		mockMvc.perform(
						MockMvcRequestBuilders
								.multipart("/jms/message")
								.file(new MockMultipartFile("message", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
								.part(
										new MockPart("persistent", "false".getBytes()),
										new MockPart("synchronous", "true".getBytes()),
										new MockPart("lookupDestination", "false".getBytes()),
										new MockPart("destination", "some-queue".getBytes()),
										new MockPart("type", "type".getBytes()),
										new MockPart("connectionFactory", "qcf/connectionFactory".getBytes()),
										new MockPart("encoding", "fakeEncoding".getBytes())
								)
				)
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testWithMessage() throws Exception {
		Mockito.doAnswer(i -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(MessageBase.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("QUEUE", headers.get("topic"));

			return msg;
		}).when(sendJmsMessage).sendSyncMessage(Mockito.any(RequestMessageBuilder.class));

		mockMvc.perform(
						MockMvcRequestBuilders
								.multipart("/jms/message")
								.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
								.part(
										new MockPart("persistent", "false".getBytes()),
										new MockPart("synchronous", "true".getBytes()),
										new MockPart("lookupDestination", "false".getBytes()),
										new MockPart("synchronous", "true".getBytes()),
										new MockPart("destination", "some-queue".getBytes()),
										new MockPart("type", "type".getBytes()),
										new MockPart("connectionFactory", "qcf/connectionFactory".getBytes())
								)
				)
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));

		/*List<Attachment> attachments = new ArrayList<>();
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
		assertEquals("\"inputMessage\"", response.getEntity().toString());*/
	}

	/*@Test
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
