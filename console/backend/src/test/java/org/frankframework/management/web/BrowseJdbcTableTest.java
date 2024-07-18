package org.frankframework.management.web;


import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, BrowseJdbcTable.class})
public class BrowseJdbcTableTest extends FrankApiTestBase {

	@Test
	public void browseJdbcTable() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/browse")
					.content("{\"datasource\": \"testDatasource\", \"table\": \"testTable\"}")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value("JDBC"))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value("FIND"));
	}

	@Test
	public void browseJdbcTableWithoutTable() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/browse")
					.content("{\"datasource\": \"testDatasource\"}")
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("tableName not defined."));
	}

}
