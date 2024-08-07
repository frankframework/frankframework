package org.frankframework.management.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, UpdateLoggingConfig.class})
public class UpdateLoggingConfigTest extends FrankApiTestBase {

	@Test
	void testGetLogConfiguration() throws Exception {
		this.testActionAndTopicHeaders("/server/logging", "LOG_CONFIGURATION", "GET");
	}

	@Test
	void testGetLogDefinitions() throws Exception {
		this.testActionAndTopicHeaders("/server/logging/settings", "LOG_DEFINITIONS", "GET");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"level\":\"WARN\"}", "{\"logger\":\"package\"}", "{\"reconfigure\":\"true\"}"})
	void testUpdateLogDefinition(String content) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging/settings")
						.contentType(MediaType.APPLICATION_JSON)
						.content(content))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateLogIntermediaryResults() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"logIntermediaryResults\":\"true\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateMaxLength() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"maxMessageLength\":50}"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateTesttoolEnabled() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"enableDebugger\":false}"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateLogLevel() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loglevel\":\"dummy\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}
}
