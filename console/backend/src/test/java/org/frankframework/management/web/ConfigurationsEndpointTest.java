package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import org.frankframework.management.Action;
import org.frankframework.management.bus.message.StringMessage;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, ConfigurationsEndpoint.class})
public class ConfigurationsEndpointTest extends FrankApiTestBase {

	@Test
	public void getConfigurationXML() throws Exception {
		testActionAndTopicHeaders("/configurations/name", "CONFIGURATION", "GET");
	}

	@Test
	public void reloadConfigurationNoValidAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name")
				.content("{\"action\": \"abc\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void reloadConfiguration() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals(Action.RELOAD.name(), headers.get("meta-action"));

			return new StringMessage(msg.getPayload(), MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations/name")
				.content("{\"action\": \"reload\"}")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isAccepted())
			.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void fullReloadNoValidAction() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/configurations")
					.content("{\"action\": \"abc\"}")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	public void fullReload() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals(Action.FULLRELOAD.name(), headers.get("meta-action"));

			return new StringMessage(msg.getPayload(), MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders.put("/configurations")
					.content("{\"action\": \"reload\"}")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isAccepted());
	}

	@Test
	public void getConfigurationByName() throws Exception {
		testActionAndTopicHeaders("/configurations/name", "CONFIGURATION", "GET");
	}

	@Test
	public void getConfigurationHealth() throws Exception {
		testActionAndTopicHeaders("/configurations/name/health", "HEALTH", null);
	}

	@Test
	public void getConfigurationFlow() throws Exception {
		testActionAndTopicHeaders("/configurations/name/flow", "FLOW", null);
	}

	@Test
	public void getConfigurationDetailsByName() throws Exception {
		testActionAndTopicHeaders("/configurations/name/versions", "CONFIGURATION", "FIND");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"activate\":true}", "{\"autoreload\":true}"})
	void testManageConfigurations(String jsonInput) throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("configName", headers.get("meta-configuration"));

			return new StringMessage(msg.getPayload(), MediaType.APPLICATION_JSON);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/versions/{version}", "configName", "versionName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"activate\":\"notABoolean\"}", "{\"autoreload\":\"notABoolean\"}"})
	void testManageConfigurationsError(String jsonInput) throws Exception {
		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/versions/{version}", "configName", "versionName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is5xxServerError())
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}

	@Test
	void testUploadConfigurations() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameters actually get sent to the outputGateway
			assertEquals("file.txt", headers.get("meta-filename"));
			assertEquals("test", headers.get("meta-datasourceName"));

			return msg;
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart("/configurations")
						.file(createMockMultipartFile("file", "file.txt", "contents".getBytes()))
						.part(
								new MockPart[] {
										new MockPart("datasource", "test".getBytes()),
										new MockPart("user", "username".getBytes()),
										new MockPart("multiple_configs", "true".getBytes()),
										new MockPart("activate_config", "false".getBytes()),
										new MockPart("automatic_reload", "true".getBytes())
								}
						)
						.characterEncoding("UTF-8")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void downloadConfiguration() throws Exception {
		testActionAndTopicHeaders("/configurations/name/versions/1.0.0/download", "CONFIGURATION", "DOWNLOAD");
	}

	@Test
	public void deleteConfiguration() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/name/versions/1.0.0"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}
}
