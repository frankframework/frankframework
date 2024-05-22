package org.frankframework.management.web.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, SendJmsMessage.class})
public class SendJmsMessageTest extends FrankApiTestBase {

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

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertEquals("QUEUE", in.getHeaders().get("topic"));
			return mockResponseMessage(in, in::getPayload, 200, MediaType.TEXT_PLAIN);
		});

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
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}

	@Test
	public void testWithFile() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertEquals("QUEUE", in.getHeaders().get("topic"));
			return mockResponseMessage(in, in::getPayload, 200, MediaType.TEXT_PLAIN);
		});

		mockMvc.perform(
						MockMvcRequestBuilders
								.multipart("/jms/message")
								.file(new MockMultipartFile("message", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
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
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}
}
