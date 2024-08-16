package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// Added ConfigurationStatus to make sure we hit some more branches in Init (because @Relation is used in ConfigurationStatus).
@ContextConfiguration(classes = {WebTestConfiguration.class, Init.class, ConfigurationStatus.class})
public class InitTest extends FrankApiTestBase {

	@Test
	public void getAllResourcesJson() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("links").exists())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void getAllResourcesHal() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/?hateoas=hal"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("_links").exists())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}
}
