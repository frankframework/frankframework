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
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("DELETE * WHERE 1", headers.get("meta-query"));
			assertEquals("test", headers.get("meta-datasourceName"));
			assertEquals("XML", headers.get("meta-resultType"));

			return msg;
		});

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
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("DELETE * WHERE 1", headers.get("meta-query"));
			assertEquals("test", headers.get("meta-datasourceName"));
			assertEquals("XML", headers.get("meta-resultType"));
			assertEquals("other", headers.get("meta-queryType"));

			return msg;
		});

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
