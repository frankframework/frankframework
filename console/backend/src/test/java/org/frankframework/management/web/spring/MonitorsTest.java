package org.frankframework.management.web.spring;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfig.class, ServerStatistics.class})
public class MonitorsTest extends FrankApiTestBase {

	@Test
	public void testGetMonitors() throws Exception {
		this.testBasicRequest("/configurations/TestConfiguration/monitors", "MONITORING", "GET");
	}

	/*@Test
	public void testAddMonitor() {
		// Arrange
		ArgumentCaptor<org.frankframework.management.web.RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(org.frankframework.management.web.RequestMessageBuilder.class);
		doAnswer(new ShowMonitorsTest.DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"monitor\":\"MonitorName\", \"destinations\": [\"one\",\"two\",\"three\" ]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/configurations/TestConfiguration/monitors", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("UPLOAD", request.getHeaders().get("action")),
				() -> assertEquals("{\"type\":\"FUNCTIONAL\",\"destinations\":[\"one\",\"two\",\"three\"],\"name\":\"MonitorName\"}", request.getPayload())
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

}
