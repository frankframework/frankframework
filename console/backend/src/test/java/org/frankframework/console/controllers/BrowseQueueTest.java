package org.frankframework.console.controllers;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseQueue.class})
public class BrowseQueueTest extends FrankApiTestBase {

	@Test
	public void getQueueConnectionFactories() throws Exception {
		testActionAndTopicHeaders("/jms", "QUEUE", "GET");
	}

	@Test
	public void browseQueue() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("testDestination", headers.get("meta-destination"));
			assertEquals("testtype", headers.get("meta-type"));

			return new StringMessage(msg.getPayload(), MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
					.content("{\"destination\": \"testDestination\", \"type\": \"testtype\"}")
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void browseQueueWithOptionalSettings() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("testDestination", headers.get("meta-destination"));
			assertEquals("testtype", headers.get("meta-type"));
			assertEquals(Boolean.TRUE, headers.get("meta-rowNumbersOnly"));
			assertEquals(Boolean.TRUE, headers.get("meta-lookupDestination"));
			assertEquals(Boolean.TRUE, headers.get("meta-showPayload"));

			return new StringMessage(msg.getPayload(), MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
						.content("{\"destination\": \"testDestination\", \"type\": \"testtype\", \"rowNumbersOnly\": true, \"payload\": true, \"lookupDestination\": true}")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@ParameterizedTest
	@CsvSource({"{\"type\": \"testtype\"}, No destination provided",
			"{\"destination\": \"testDestination\"}, No type provided"})
	public void testMissingParameters(String content, String response) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
						.content(content)
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value(response));
	}
}
