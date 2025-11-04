package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ServerDetails.class})
public class ServerDetailsTest extends FrankApiTestBase {

	@Test
	public void testServerInformation() throws Exception {
		// instance name set to Unavailable since Console is running without a FF worker instance
		mockMvc.perform(MockMvcRequestBuilders.get("/server/info"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("instance.name").value("Unavailable"));
	}

	@Test
	public void testAllConfigurations() throws Exception {
		this.testActionAndTopicHeaders("/server/configurations", "CONFIGURATION", "FIND");
	}

	@Test
	public void testServerWarnings() throws Exception {
		this.testActionAndTopicHeaders("/server/warnings", "APPLICATION", "WARNINGS");
	}

	@Test
	public void testFrankHealth() throws Exception {
		// gives 503 since Console is running without a FF worker instance
		mockMvc.perform(MockMvcRequestBuilders.get("/server/health"))
				.andExpect(MockMvcResultMatchers.status().is(503))
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Service Unavailable"))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("No cluster members available"));
	}
}
