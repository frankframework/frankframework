package org.frankframework.management.web.spring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {WebTestConfig.class, SendJmsMessage.class})
public class SendJmsMessageTest extends FrankApiTestBase {

	@Autowired
	private SpringUnitTestLocalGateway<?> outputGateway;

	@Override
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		super.setUp();
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
			return mockResponseMessage(in, in::getPayload, 200, null);
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
