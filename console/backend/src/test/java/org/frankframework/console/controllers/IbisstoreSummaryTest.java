package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, IbisstoreSummary.class})
public class IbisstoreSummaryTest extends FrankApiTestBase {

	@Test
	public void getIbisStoreSummary() throws Exception {
		String query = "SELECT name FROM ibisstore";
		String datasource = "jdbc/test";

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			String queryValue = msg.getHeaders().get("meta-query", String.class);
			String datasourceValue = msg.getHeaders().get("meta-datasourceName", String.class);
			return mockResponseMessage(msg, () -> "{\"query\": " + queryValue + " , \"datasource\": " + datasourceValue + "}", 200, MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/summary")
						.content("{ \"query\": \"" + query + "\", \"datasource\": \"" + datasource + "\" }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("query").value(query))
				.andExpect(MockMvcResultMatchers.jsonPath("datasource").value(datasource));
	}
}
