package org.frankframework.management.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Configuration.class})
public class ConfigurationTest extends FrankApiTestBase {

	@Test
	public void getConfigurationXML() throws Exception {
		testActionAndTopicHeaders("/configurations/naam", "CONFIGURATION", "GET");
	}

	@Test
	public void reloadConfigurationNoValidAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/naam")
				.content("{\"action\": \"abc\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void reloadConfiguration() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/naam")
				.content("{\"action\": \"reload\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isAccepted())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

}
