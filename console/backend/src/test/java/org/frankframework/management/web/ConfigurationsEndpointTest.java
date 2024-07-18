package org.frankframework.management.web;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationsEndpoint.class})
public class ConfigurationsEndpointTest extends FrankApiTestBase {

	@Test
	public void getConfigurationXML() throws Exception {
		testActionAndTopicHeaders("/configurations/name", "CONFIGURATION", "GET");
	}

	@Test
	public void reloadConfigurationNoValidAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name")
				.content("{\"action\": \"abc\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void reloadConfiguration() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name")
				.content("{\"action\": \"reload\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isAccepted())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void fullReloadNoValidAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations")
					.content("{\"action\": \"abc\"}")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void fullReload() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations")
					.content("{\"action\": \"reload\"}")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	public void getConfigurationByName() throws Exception {
		testActionAndTopicHeaders("/configurations/name", "CONFIGURATION", "GET");
	}

	@Test
	public void getConfigurationHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/name/health", "HEALTH", null);
	}

	@Test
	public void getConfigurationFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/name/flow", "FLOW", null);
	}

	@Test
	public void getConfigurationDetailsByName() throws Exception {
		testActionAndTopicHeaders("/configurations/name/versions", "CONFIGURATION", "FIND");
	}

	@Test
	public void manageConfigurationWithActivate() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name/versions/1.0.0")
						.content("{\"activate\": true}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void manageConfigurationWithInvalidActivate() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name/versions/1.0.0")
						.content("{\"activate\": \"false\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError());
	}

	@Test
	public void manageConfigurationWithAutoreload() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name/versions/1.0.0")
						.content("{\"autoreload\": true}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void manageConfigurationWithInvalidAutoreload() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name/versions/1.0.0")
						.content("{\"autoreload\": \"false\"}")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError());
	}

	public void uploadConfiguration() {
		// TODO
	}

	@Test
	public void downloadConfiguration() throws Exception {
		testActionAndTopicHeaders("/configurations/name/versions/1.0.0/download", "CONFIGURATION", "DOWNLOAD");
	}

	@Test
	public void deleteConfiguration() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/name/versions/1.0.0"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

}
