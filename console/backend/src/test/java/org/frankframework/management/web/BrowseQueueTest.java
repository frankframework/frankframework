package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseQueue.class})
public class BrowseQueueTest extends FrankApiTestBase {

	@Test
	public void testBrowseQueues() throws Exception {
		testActionAndTopicHeaders("/jms", "QUEUE", "GET");
	}

	@Test
	void testBrowseSingleQueue() throws Exception {
		String jsonInput = "{\"destination\":\"test\", \"type\":\"type\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.post("/jms/browse")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"QUEUE\",\"action\":\"FIND\"}"));
	}

	@Test
	void testBrowseSingleQueueError() throws Exception {
		String jsonInput = "{\"destination\":\"\", \"type\":\"type\"}";

		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/jms/browse")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is5xxServerError())
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}
}
