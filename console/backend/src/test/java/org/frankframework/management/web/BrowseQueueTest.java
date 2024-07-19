package org.frankframework.management.web;


import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseQueue.class})
public class BrowseQueueTest extends FrankApiTestBase {

	@Test
	public void getQueueConnectionFactories() throws Exception {
		testActionAndTopicHeaders("/jms", "QUEUE", "GET");
	}

	@Test
	public void browseQueue() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
					.content("{\"destination\": \"testDestination\", \"type\": \"testtype\"}")
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value("QUEUE"))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value("FIND"));
	}

	@Test
	public void browseQueueWithoutDestination() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
						.content("{\"type\": \"testtype\"}")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("No destination provided"));
	}

	@Test
	public void browseQueueWithoutType() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jms/browse")
						.content("{\"destination\": \"testDestination\"}")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("No type provided"));
	}

}
