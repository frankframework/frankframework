package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.web.RequestMessageBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.mockito.MockitoAnnotations;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ContextConfiguration(classes = {WebTestConfig.class, Monitors.class})
public class MonitorsTest extends FrankApiTestBase {

	@Autowired
	private SpringUnitTestLocalGateway<?> outputGateway;

	@Override
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		super.setUp();
	}

	@Test
	public void testGetMonitors() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/configurations/{configuration}/monitors/", "TestConfiguration"))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value("MONITORING"))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value("GET"));
	}

	/*@Test
	public void testAddMonitor() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(new DefaultSuccessAnswer());
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"monitor\":\"MonitorName\", \"destinations\": [\"one\",\"two\",\"three\" ]}";

		mockMvc.perform(MockMvcRequestBuilders
					.post("/configurations/TestConfiguration/monitors")
					.content(jsonInput))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpectAll(
						jsonPath("type").value("FUNCTIONAL"),
						jsonPath("destinations").value("[\"one\",\"two\",\"three\"]"),
						jsonPath("name").value("MonitorName")
				);
	}*/

	/*@Test
	public void testUpdateMonitor() {
		// Arrange
		ArgumentCaptor<org.frankframework.management.web.RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(org.frankframework.management.web.RequestMessageBuilder.class);
		doAnswer(new ShowMonitorsTest.DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = "{\"type\":\"FUNCTIONAL\",\"action\":\"edit\",\"destinations\":[\"mockDestination\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> assertEquals("edit", BusMessageUtils.getHeader(request, "state")),
				() -> assertEquals("monitorName", BusMessageUtils.getHeader(request, "monitor")),
				() -> assertEquals("{\"type\":\"FUNCTIONAL\",\"destinations\":[\"mockDestination\"]}", request.getPayload())
		);
	}*/

	/*@Test
	public void testAddTrigger() {
		// Arrange
		ArgumentCaptor<org.frankframework.management.web.RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(org.frankframework.management.web.RequestMessageBuilder.class);
		doAnswer(new ShowMonitorsTest.DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = "{\"type\":\"ALARM\",\"filter\":\"NONE\",\"events\":[\"Receiver Configured\"],\"severity\":\"HARMLESS\",\"threshold\":1,\"period\":2}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/configurations/TestConfiguration/monitors/monitorName/triggers", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("UPLOAD", request.getHeaders().get("action")),
				() -> assertEquals("monitorName", BusMessageUtils.getHeader(request, "monitor")),
				() -> assertEquals(jsonInput, request.getPayload())
		);
	}*/

	/*@Test
	public void testUpdateTrigger() throws Exception {
		// Arrange
		ArgumentCaptor<org.frankframework.management.web.RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new ShowMonitorsTest.DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		URL jsonInputURL = ShowMonitorsTest.class.getResource("/monitoring/updateTrigger.json");
		assertNotNull(jsonInputURL, "unable to find input JSON"); // Check if the file exists to avoid NPE's
		String jsonInput = StreamUtil.streamToString(jsonInputURL.openStream());

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName/triggers/0", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("monitorName", BusMessageUtils.getHeader(request, "monitor")),
				() -> assertNull(BusMessageUtils.getHeader(request, "trigger")),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> assertEquals(jsonPretty(jsonInput), jsonPretty(String.valueOf(request.getPayload())))
		);
	}*/

	private static class DefaultSuccessAnswer implements Answer<Message<String>> {

		@Override
		public Message<String> answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			RequestMessageBuilder request = (RequestMessageBuilder)input;
			assertEquals(BusTopic.MONITORING, request.getTopic());
			return new JsonMessage(request);
		}

	}

}
