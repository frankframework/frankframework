package org.frankframework.console.controllers;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseJdbcTable.class})
public class BrowseJdbcTableTest extends FrankApiTestBase {

	@Test
	public void browseJdbcTable() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameter actually gets sent to the outputGateway
			assertEquals("testTable", headers.get("meta-table"));

			return msg;
		});

		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/browse")
					.content("{\"datasource\": \"testDatasource\", \"table\": \"testTable\"}")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void browseJdbcTableWithoutTable() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/browse")
					.content("{\"datasource\": \"testDatasource\"}")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("tableName not defined."));
	}
}
