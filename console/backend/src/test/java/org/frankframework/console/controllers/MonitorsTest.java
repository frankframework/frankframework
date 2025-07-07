package org.frankframework.console.controllers;

import static org.frankframework.console.util.MatchUtils.assertJsonEquals;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.StreamUtil;

@ContextConfiguration(classes = {WebTestConfiguration.class, Monitors.class})
public class MonitorsTest extends FrankApiTestBase {

	@Test
	public void testGetMonitors() throws Exception {
		this.testActionAndTopicHeaders("/configurations/configurationName/monitors/", "MONITORING", "GET", "TestConfiguration");
	}

	@Test
	void testGetMonitor() throws Exception {
		this.testActionAndTopicHeaders("/configurations/configurationName/monitors/monitorName", "MONITORING", "GET", "TestConfiguration");
	}

	@Test
	void testGetTriggers() throws Exception {
		this.testActionAndTopicHeaders("/configurations/configurationName/monitors/monitorName/triggers", "MONITORING", "GET", "TestConfiguration");
	}

	@Test
	void testGetTrigger() throws Exception {
		this.testActionAndTopicHeaders("/configurations/configurationName/monitors/monitorName/triggers/1", "MONITORING", "GET", "TestConfiguration");
	}

	@Test
	void testDeleteMonitor() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("DELETE", in.getHeaders().get("action")),
					() -> assertEquals("MONITORING", in.getHeaders().get("topic")),
					() -> assertEquals("configurationName", in.getHeaders().get("meta-configuration"))
			);
			return in;
		});

		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/configurationName/monitors/monitorName"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	void testDeleteTrigger() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("DELETE", in.getHeaders().get("action")),
					() -> assertEquals("MONITORING", in.getHeaders().get("topic")),
					() -> assertEquals("configurationName", in.getHeaders().get("meta-configuration"))
			);
			return in;
		});

		mockMvc.perform(MockMvcRequestBuilders.delete("/configurations/configurationName/monitors/monitorName/triggers/1"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void testAddMonitor() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("UPLOAD", in.getHeaders().get("action")),
					() -> assertEquals("MONITORING", in.getHeaders().get("topic")),
					() -> assertEquals("TestConfiguration", in.getHeaders().get("meta-configuration"))
			);
			return in;
		});

		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"monitor\":\"MonitorName\", \"destinations\": [\"one\",\"two\",\"three\" ]}";

		mockMvc.perform(MockMvcRequestBuilders
					.post("/configurations/{configuration}/monitors/", "TestConfiguration")
					.content(jsonInput)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testUpdateMonitor() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("MANAGE", in.getHeaders().get("action")),
					() -> assertEquals("edit", BusMessageUtils.getHeader(in, "state")),
					() -> assertEquals("monitorName", BusMessageUtils.getHeader(in, "monitor"))
			);
			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		});
		String jsonInput = "{\"type\":\"FUNCTIONAL\",\"action\":\"edit\",\"destinations\":[\"mockDestination\"]}";

		mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/monitors/{monitorName}", "TestConfiguration", "monitorName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpectAll(
						jsonPath("type").value("FUNCTIONAL"),
						jsonPath("destinations").value(contains("mockDestination"))
				);
	}

	@Test
	public void testAddTrigger() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("UPLOAD", in.getHeaders().get("action")),
					() -> assertEquals("monitorName", BusMessageUtils.getHeader(in, "monitor"))
			);
			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		});

		URL jsonInputURL = MonitorsTest.class.getResource("/management/web/MonitorTest_testAddTrigger.json");
		assertNotNull(jsonInputURL, "unable to find input JSON");
		String jsonInput = StreamUtil.streamToString(jsonInputURL.openStream());
		URL jsonOutputURL = MonitorsTest.class.getResource("/management/web/MonitorTest_testAddTrigger_output.json");
		assertNotNull(jsonOutputURL, "unable to find output JSON");
		String jsonOutput = StreamUtil.streamToString(jsonOutputURL.openStream());

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders
						.post("/configurations/{configuration}/monitors/{monitorName}/triggers", "TestConfiguration", "monitorName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		assertJsonEquals(jsonOutput, result.getResponse().getContentAsString());
	}

	@Test
	public void testUpdateTrigger() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);
			assertAll(
					() -> assertEquals("monitorName", BusMessageUtils.getHeader(in, "monitor")),
					() -> assertNull(BusMessageUtils.getHeader(in, "trigger")),
					() -> assertEquals("MANAGE", in.getHeaders().get("action"))
			);
			return mockResponseMessage(in, in::getPayload, 200, MediaType.APPLICATION_JSON);
		});
		URL jsonInputURL = MonitorsTest.class.getResource("/management/web/MonitorTest_updateTrigger.json");
		assertNotNull(jsonInputURL, "unable to find input JSON"); // Check if the file exists to avoid NPE's
		String jsonInput = StreamUtil.streamToString(jsonInputURL.openStream());
		URL jsonOutputURL = MonitorsTest.class.getResource("/management/web/MonitorTest_updateTrigger_output.json");
		assertNotNull(jsonOutputURL, "unable to find output JSON");
		String jsonOutput = StreamUtil.streamToString(jsonOutputURL.openStream());

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/monitors/{monitorName}/triggers/1", "TestConfiguration", "monitorName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		assertJsonEquals(jsonOutput, result.getResponse().getContentAsString());
	}
}
