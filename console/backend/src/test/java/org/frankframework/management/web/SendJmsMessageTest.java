package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, SendJmsMessage.class})
public class SendJmsMessageTest extends FrankApiTestBase {

	@Test
	public void testWrongEncoding() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
						.part(getMultiPartParts())
						.part(new MockPart("encoding", "fakeEncoding".getBytes())))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testFileWrongEncoding() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("message", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
						.part(getMultiPartParts())
						.part(new MockPart("encoding", "fakeEncoding".getBytes())))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testWithMessage() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(this::getMockMessage);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}

	private Message<String> getMockMessage(InvocationOnMock i) {
		Message<String> in = i.getArgument(0);
		assertEquals("QUEUE", in.getHeaders().get("topic"));
		return mockResponseMessage(in, in::getPayload, 200, MediaType.TEXT_PLAIN);
	}

	@Test
	public void testWithFile() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(this::getMockMessage);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("file", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}

	@Test
	public void testWithZipFile() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(this::getMockMessage);

		URL zip = TestPipelineTest.class.getResource("/management/web/TestPipeline/stored.zip");

		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(createMockMultipartFile("file", "archive2.zip", zip.openStream().readAllBytes()))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	private MockPart[] getMultiPartParts() {
		return new MockPart[]{
				new MockPart("persistent", "false".getBytes()),
				new MockPart("synchronous", "true".getBytes()),
				new MockPart("lookupDestination", "false".getBytes()),
				new MockPart("synchronous", "true".getBytes()),
				new MockPart("destination", "some-queue".getBytes()),
				new MockPart("type", "type".getBytes()),
				new MockPart("connectionFactory", "qcf/connectionFactory".getBytes())
		};
	}
}
