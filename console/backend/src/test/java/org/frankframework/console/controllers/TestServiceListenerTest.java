package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, TestServiceListener.class})
public class TestServiceListenerTest extends FrankApiTestBase {

	private static final String INPUT_MESSAGE = "inputMessage";

	private static final String TEST_SERVICE_LISTENER_ENDPOINT = "/test-servicelistener";

	@Test
	public void testWrongEncoding() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_SERVICE_LISTENER_ENDPOINT)
						.file(createMockMultipartFile("message", null, INPUT_MESSAGE.getBytes()))
						.part(getMockPart("service", "dummyService123"))
						.part(getMockPart("encoding", "fakeEncoding"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testFileWrongEncoding() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_SERVICE_LISTENER_ENDPOINT)
						.file(createMockMultipartFile("file", "script.xml", INPUT_MESSAGE.getBytes()))
						.part(getMockPart("service", "dummyService123"))
						.part(getMockPart("encoding", "fakeEncoding"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("unsupported file encoding [fakeEncoding]"));
	}

	@Test
	public void testMessageServiceListeners() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(INPUT_MESSAGE, in.getPayload());
			assertEquals("dummyService123", in.getHeaders().get("meta-service"));

			return new StringMessage(INPUT_MESSAGE);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_SERVICE_LISTENER_ENDPOINT)
						.file(createMockMultipartFile("message", null, INPUT_MESSAGE.getBytes()))
						.part(getMockPart("service", "dummyService123"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string(INPUT_MESSAGE));
	}

	@Test
	public void testFileServiceListeners() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals(INPUT_MESSAGE, in.getPayload());
			assertEquals("dummyService123", in.getHeaders().get("meta-service"));

			return new StringMessage(INPUT_MESSAGE);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart(TEST_SERVICE_LISTENER_ENDPOINT)
						.file(createMockMultipartFile("file", null, INPUT_MESSAGE.getBytes()))
						.part(getMockPart("service", "dummyService123"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string(INPUT_MESSAGE));
	}

	@Test
	public void testListServiceListeners() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(TEST_SERVICE_LISTENER_ENDPOINT))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"SERVICE_LISTENER\",\"action\":\"GET\"}"));
	}

	private MockPart getMockPart(String name, String value) {
		return new MockPart(name, value.getBytes());
	}
}
