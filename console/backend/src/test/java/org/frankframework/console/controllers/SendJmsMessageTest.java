package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
						.part(getMultiPartParts())
						.part(new MockPart("encoding", "fakeEncoding".getBytes())))
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
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testWithMessage() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("inputMessage", in.getPayload());
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		});
		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("message", null, MediaType.TEXT_PLAIN_VALUE, "inputMessage".getBytes()))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}

	@Test
	public void testWithFile() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("inputMessage", in.getPayload());
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		});
		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("file", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("inputMessage"));
	}

	@Test
	public void testWithZipFile() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("inputMessage", in.getPayload());
			assertEquals("UPLOAD", in.getHeaders().get("action"));

			return in;
		});

		URL zip = TestPipelineTest.class.getResource("/management/web/TestPipeline/stored.zip");

		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(createMockMultipartFile("file", "archive2.zip", zip.openStream().readAllBytes()))
						.part(getMultiPartParts()))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	void testAsynchronousSend() throws Exception {
		ArgumentCaptor<Message<Object>> requestCapture = ArgumentCaptor.forClass(Message.class);
		doCallRealMethod().when(outputGateway).sendAsyncMessage(requestCapture.capture());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/jms/message")
						.file(new MockMultipartFile("file", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("inputMessage".getBytes())))
						.part(getMultiPartParts("false")))
				.andExpect(MockMvcResultMatchers.status().isOk());

		Message<Object> capturedRequest = Awaitility.await().atMost(1500, TimeUnit.MILLISECONDS).until(requestCapture::getValue, Objects::nonNull);
		assertEquals("inputMessage", capturedRequest.getPayload());
		assertEquals("UPLOAD", capturedRequest.getHeaders().get("action"));
		assertEquals(false, capturedRequest.getHeaders().get("meta-synchronous"));
	}

	private MockPart[] getMultiPartParts() {
		return getMultiPartParts("true");
	}

	private MockPart[] getMultiPartParts(String synchronous) {
		return new MockPart[]{
				new MockPart("persistent", "false".getBytes()),
				new MockPart("lookupDestination", "false".getBytes()),
				new MockPart("synchronous", synchronous.getBytes()),
				new MockPart("destination", "some-queue".getBytes()),
				new MockPart("type", "type".getBytes()),
				new MockPart("connectionFactory", "qcf/connectionFactory".getBytes())
		};
	}
}
