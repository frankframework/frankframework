package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationsEndpoint.class})
public class ConfigurationTest extends FrankApiTestBase {

	@Test
	void getConfiguration() throws Exception {
		testActionAndTopicHeaders("/configurations", "CONFIGURATION", "GET");
	}

	@Test
	void fullReload() throws Exception {
		String jsonInput = "{ \"action\":\"reload\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	void fullReloadError() throws Exception {
		String jsonInput = "{ \"action\":\"reloadasdfasdf\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void getConfigurationHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/health", "HEALTH", null);
	}

	@Test
	public void getConfigurationFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/flow", "FLOW", null);
	}

	@Test
	public void getConfigurationXML() throws Exception {
		testActionAndTopicHeaders("/configurations/naam", "CONFIGURATION", "GET");
	}

	@Test
	public void getConfigurationVersions() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/versions", "CONFIGURATION", "FIND");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"activate\":true}", "{\"autoreload\":true}"})
	void testManageConfigurations(String jsonInput) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/versions/{version}", "configName", "versionName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"CONFIGURATION\",\"action\":\"MANAGE\"}"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"activate\":\"notABoolean\"}", "{\"autoreload\":\"notABoolean\"}"})
	void testManageConfigurationsError(String jsonInput) throws Exception {
		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/versions/{version}", "configName", "versionName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is5xxServerError())
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}


	@Test
	void testUploadConfigurations() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.multipart("/configurations")
						.file(createMockMultipartFile("file", "file.txt", "contents".getBytes()))
						.part(
								new MockPart[] {
										new MockPart("datasource", "test".getBytes()),
										new MockPart("user", "username".getBytes()),
										new MockPart("multiple_configs", "true".getBytes()),
										new MockPart("activate_config", "false".getBytes()),
										new MockPart("automatic_reload", "true".getBytes())
								}
						)
						.characterEncoding("UTF-8")
						.accept(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"CONFIGURATION\",\"action\":\"UPLOAD\"}"));
	}

	@Test
	void testDeleteConfigurations() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.delete("/configurations/{configuration}/versions/{version}", "configName", "versionName")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	void testDownloadConfigurations() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/versions/versionName/download", "CONFIGURATION", "DOWNLOAD");
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
