package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.util.StreamUtil;

public class ShowMonitorsTest extends FrankApiTestBase<ShowMonitors> {

	@Override
	public ShowMonitors createJaxRsResource() {
		return new ShowMonitors();
	}

	private static class DefaultSuccessAnswer implements Answer<Message<String>> {

		@Override
		public Message<String> answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			RequestMessageBuilder request = (RequestMessageBuilder)input;
			assertEquals(BusTopic.MONITORING, request.getTopic());
			return new JsonResponseMessage(request);
		}

	}

	@Test
	public void testGetMonitors() {
		// Arrange
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/configurations/TestConfiguration/monitors");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("{\"topic\":\"MONITORING\",\"action\":\"GET\"}", response.getEntity());
	}

	@Test
	public void testAddMonitor() {
		// Arrange
		ArgumentCaptor<RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
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
	}

	@Test
	public void testUpdateMonitor() {
		// Arrange
		ArgumentCaptor<RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = "{\"type\":\"FUNCTIONAL\",\"state\":\"edit\",\"destinations\":[\"mockDestination\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> assertEquals("edit", request.getHeaders().get("state")),
				() -> assertEquals("monitorName", request.getHeaders().get("monitor")),
				() -> assertEquals("{\"type\":\"FUNCTIONAL\",\"destinations\":[\"mockDestination\"]}", request.getPayload())
			);
	}

	@Test
	public void testUpdateTrigger() throws Exception {
		// Arrange
		ArgumentCaptor<RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		URL jsonInputURL = ShowMonitorsTest.class.getResource("/monitoring/updateTrigger.json");
		assertNotNull(jsonInputURL, "unable to find input JSON"); // Check if the file exists to avoid NPE's
		String jsonInput = StreamUtil.streamToString(jsonInputURL.openStream());

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName/triggers/0", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()), () -> assertEquals("monitorName", request.getHeaders().get("monitor")),
				() -> assertEquals(0, request.getHeaders().get("trigger")),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> assertEquals(jsonPretty(jsonInput), jsonPretty(String.valueOf(request.getPayload())))
			);
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