package org.frankframework.management.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ExecuteJdbcQuery.class})
public class ExecuteJdbcQueryTest extends FrankApiTestBase {

	@Test
	public void getJdbcDataSources() throws Exception {
		testActionAndTopicHeaders("/jdbc", "JDBC", "GET");
	}

	@Test
	public void executeJdbcQueryBadRequest() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/query")
				.content("{\"datasource\": \"test\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isBadRequest())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.jsonPath("error").value("Missing data, datasource, resultType and query are expected."))
			.andExpect(MockMvcResultMatchers.jsonPath("status").value("Bad Request"));
	}

	@Test
	public void executeJdbcQuery() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/query")
				.content("""
						{
							\"datasource\": \"test\",
							\"query\": \"DELETE * WHERE 1\",
							\"resultType\": \"XML\"
						}
						""")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void executeJdbcQueryWithQueryTypeAuto() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/jdbc/query")
						.content("""
						{
							\"datasource\": \"test\",
							\"query\": \"DELETE * WHERE 1\",
							\"queryType\": \"AUTO\",
							\"resultType\": \"XML\"
						}
						""")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

}
