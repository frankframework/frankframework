package org.frankframework.management.web.spring;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, UpdateLoggingConfig.class})
public class UpdateLoggingConfigTest extends FrankApiTestBase {

	@Test
	public void updateLogIntermediaryResults() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"logIntermediaryResults\":\"true\"}"))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateMaxLength() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"maxMessageLength\":50}"))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateTesttoolEnabled() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"enableDebugger\":false}"))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void updateLogLevel() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders
						.put("/server/logging")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"loglevel\":\"dummy\"}"))
				.andDo(print())
				.andExpect(MockMvcResultMatchers.status().isOk());
	}
}
