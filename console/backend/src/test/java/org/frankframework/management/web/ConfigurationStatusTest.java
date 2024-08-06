package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationStatus.class})
public class ConfigurationStatusTest extends FrankApiTestBase {

	@Test
	void testGetAdapters() throws Exception {
		testActionAndTopicHeaders("/adapters", "ADAPTER", "GET");
	}

	@Test
	void testGetAdapter() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/adapters/adapterName", "ADAPTER", "FIND");
	}

	@Test
	void testGetAdapterHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/adapters/adapterName/health", "HEALTH", null);
	}

	@Test
	void testStartStopMultipleAdapters() throws Exception {
		String jsonInput = "{ \"action\":\"start\", \"adapters\":[\"een\", \"twee\"]}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/adapters")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	void testStartStopMultipleAdaptersError() throws Exception {
		String jsonInput = "{ \"action\":\"\", \"adapters\":[\"een\", \"twee\"]}";

		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.put("/adapters")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is4xxClientError())
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}

	@Test
	void testStartStopAdapter() throws Exception {
		String jsonInput = "{\"action\":\"start\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/adapters/{adapter}", "configName", "adapterName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"status\":\"ok\"}"));
	}

	@Test
	void testStartStopAdapterError() throws Exception {
		String jsonInput = "{\"action\":\"\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/adapters/{adapter}", "configName", "adapterName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	void testUpdateReceiver() throws Exception {
		String jsonInput = "{\"action\":\"start\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/adapters/{adapter}/receivers/{receiver}", "configName", "adapterName", "receiverName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string("{\"status\":\"ok\"}"));
	}

	@Test
	void testUpdateReceiverError() throws Exception {
		String jsonInput = "{\"action\":\"\"}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/adapters/{adapter}/receivers/{receiver}", "configName", "adapterName", "receiverName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	void testGetAdapterFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/configurationName/adapters/adapterName/flow", "FLOW", null);
	}
}
