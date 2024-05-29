package org.frankframework.management.web;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.StreamUtil;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Monitors.class})
public class MonitorsTest extends FrankApiTestBase {

	@Test
	public void testGetMonitors() throws Exception {
		this.testBasicRequest("/configurations/{configuration}/monitors/", "MONITORING", "GET", "TestConfiguration");
	}

	@Test
	public void testAddMonitor() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(new DefaultSuccessAnswer());
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"monitor\":\"MonitorName\", \"destinations\": [\"one\",\"two\",\"three\" ]}";

		mockMvc.perform(MockMvcRequestBuilders
					.post("/configurations/{configuration}/monitors/", "TestConfiguration")
					.content(jsonInput)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpectAll(
						jsonPath("type").value("FUNCTIONAL"),
						jsonPath("destinations").value(contains("one", "two", "three")),
						jsonPath("name").value("MonitorName")
				);
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
		String jsonInput = "{\"type\":\"ALARM\",\"filter\":\"NONE\",\"events\":[\"Receiver Configured\"],\"severity\":\"HARMLESS\",\"threshold\":1,\"period\":2}";

		mockMvc.perform(MockMvcRequestBuilders
						.post("/configurations/{configuration}/monitors/{monitorName}/triggers", "TestConfiguration", "monitorName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.content().string(jsonInput));
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

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders
						.put("/configurations/{configuration}/monitors/{monitorName}", "TestConfiguration", "monitorName")
						.content(jsonInput)
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		assertEquals(jsonInput, jsonPretty(result.getResponse().getContentAsString()));
	}

	private static String jsonPretty(String json) {
		StringWriter sw = new StringWriter();
		try(JsonReader jr = Json.createReader(new StringReader(json))) {
			JsonStructure jobj = jr.read();

			Map<String, Object> properties = new HashMap<>(1);
			properties.put(JsonGenerator.PRETTY_PRINTING, true);

			JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
			try (JsonWriter jsonWriter = writerFactory.createWriter(sw)) {
				jsonWriter.write(jobj);
			}
		}
		return sw.toString().trim();
	}

}
