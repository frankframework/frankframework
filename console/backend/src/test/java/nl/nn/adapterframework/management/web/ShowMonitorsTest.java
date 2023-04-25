package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.webcontrol.api.ShowMonitors;

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
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"destinations\":[\"one\",\"two\",\"three\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/configurations/TestConfiguration/monitors", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("FUNCTIONAL", request.getHeaders().get("type")),
				() -> assertEquals("one,two,three", request.getHeaders().get("destinations")),
				() -> assertEquals("UPLOAD", request.getHeaders().get("action"))
			);
	}

	@Test
	public void testManageMonitor() {
		// Arrange
		ArgumentCaptor<RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"destinations\":[\"mockDestination\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),
				() -> assertEquals("FUNCTIONAL", request.getHeaders().get("type")),
				() -> assertEquals("mockDestination", request.getHeaders().get("destinations")),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> assertEquals("monitorName", request.getHeaders().get("monitor"))
			);
	}

	@Test
	public void testUpdateTrigger() throws Exception {
		// Arrange
		ArgumentCaptor<RequestMessageBuilder> requestMessage = ArgumentCaptor.forClass(RequestMessageBuilder.class);
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(requestMessage.capture());
		String jsonInput = TestFileUtils.getTestFile("/monitoring/updateTrigger.json");

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName/triggers/0", jsonInput);

		// Assert
		Message<?> request = requestMessage.getValue().build();
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString()),	() -> assertEquals("monitorName", request.getHeaders().get("monitor")),
				() -> assertEquals(0, request.getHeaders().get("trigger")),
				() -> assertEquals("MANAGE", request.getHeaders().get("action")),
				() -> MatchUtils.assertJsonEquals(jsonInput, String.valueOf(request.getPayload()))
			);
	}
}