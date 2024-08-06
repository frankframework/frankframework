package org.frankframework.management.web;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationStatus.class})
public class ConfigurationStatusTest extends FrankApiTestBase {

	@Test
	public void getAdapters() throws Exception {
		testActionAndTopicHeaders("/adapters", "ADAPTER", "GET");
	}

	@Test
	public void getAdapter() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/name", "ADAPTER", "FIND");
	}

	@Test
	public void getAdapterHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/name/health", "HEALTH", null);
	}

	@Test
	public void updateAdapters() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"start\", \"adapters\": [\"adapter1\", \"adapter2\"] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	public void updateAdaptersWithNoAdapters() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"start\", \"adapters\": [] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	public void updateAdaptersWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/adapters")
						.content("{ \"action\": \"badAction\", \"adapters\": [\"adapter1\", \"adapter2\"] }")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("no or unknown action provided"))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Bad Request"));
	}

	@Test
	public void updateAdapter() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"start\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"stop\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("ok"));
	}

	@Test
	public void updateAdapterWithNonStringAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": 0}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void updateAdapterWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter")
						.content("{\"action\": \"badAction\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("no or unknown action provided"))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Bad Request"));
	}

	@Test
	public void updateReceiver() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content("{\"action\": \"stop\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("ok"));
	}

	@Test
	public void updateReceiverWithNonStringAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content("{\"action\": 0}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void updateReceiverWithBadAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/configuration/adapters/adapter/receivers/receiver")
						.content("{\"action\": \"badAction\"}")
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("no or unknown action provided"))
				.andExpect(MockMvcResultMatchers.jsonPath("status").value("Bad Request"));
	}

	@Test
	public void getAdapterFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/configuration/adapters/adapter/flow", "FLOW", null);
	}

}
