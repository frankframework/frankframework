package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Logging.class})
class LoggingTest extends FrankApiTestBase {

	@Test
	public void getLoggingBasic() throws Exception {
		// gives 503 since Console is running without a FF worker instance
		mockMvc.perform(MockMvcRequestBuilders.get("/logging"))
				.andExpect(MockMvcResultMatchers.status().is(503))
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Service Unavailable"))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("Can't access logs without a running framework instance"));
	}
}
