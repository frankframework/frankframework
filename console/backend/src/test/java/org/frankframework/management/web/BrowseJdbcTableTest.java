package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseJdbcTable.class})
public class BrowseJdbcTableTest extends FrankApiTestBase {

	@Test
	void testBrowseJdbcTable() throws Exception {
		String jsonInput = "{ \"datasource\":\"test\", \"table\":\"table\", \"where\": \"where\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.post("/jdbc/browse")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"topic\":\"JDBC\",\"action\":\"FIND\"}"));
	}

	@Test
	void testBrowseJdbcTableError() throws Exception {
		String jsonInput = "{\"datasource\":\"test\", \"where\": \"where\"}";

		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/jdbc/browse")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is4xxClientError())
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}
}
