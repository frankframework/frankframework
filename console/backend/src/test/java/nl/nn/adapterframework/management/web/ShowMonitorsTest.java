package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.webcontrol.api.ShowMonitors;

public class ShowMonitorsTest extends FrankApiTestBase<ShowMonitors> {

	@Override
	public ShowMonitors createJaxRsResource() {
		return new ShowMonitors();
	}

	private static class DefaultSuccessAnswer implements Answer<Message<String>> {
		private Consumer<Message<?>>[] requestMessageAssertions;
		@SafeVarargs
		public DefaultSuccessAnswer(Consumer<Message<?>>... requestMessageAssertions) {
			this.requestMessageAssertions = requestMessageAssertions;
		}

		@Override
		public Message<String> answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			RequestMessageBuilder request = (RequestMessageBuilder)input;
			assertEquals(BusTopic.MONITORING, request.getTopic());

			Message<?> requestMessage = request.build();
			for(Consumer<Message<?>> assertion : requestMessageAssertions) {
				assertion.accept(requestMessage);
			}

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
		DefaultSuccessAnswer answer = new DefaultSuccessAnswer(
				(request)->assertEquals("FUNCTIONAL", request.getHeaders().get("type")),
				(request)->assertEquals("one,two,three", request.getHeaders().get("destinations")),
				(request)->assertEquals("UPLOAD", request.getHeaders().get("action"))
			);
		doAnswer(answer).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"destinations\":[\"one\",\"two\",\"three\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/configurations/TestConfiguration/monitors", jsonInput);

		// Assert
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString())
			);
	}

	@Test
	public void testManageMonitor() {
		// Arrange
		DefaultSuccessAnswer answer = new DefaultSuccessAnswer(
				(request)->assertEquals("FUNCTIONAL", request.getHeaders().get("type")),
				(request)->assertEquals("mockDestination", request.getHeaders().get("destinations")),
				(request)->assertEquals("MANAGE", request.getHeaders().get("action")),
				(request)->assertEquals("monitorName", request.getHeaders().get("monitor"))
			);
		doAnswer(answer).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));
		String jsonInput = "{ \"type\":\"FUNCTIONAL\", \"destinations\":[\"mockDestination\"]}";

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/configurations/TestConfiguration/monitors/monitorName", jsonInput);

		// Assert
		assertAll(
				() -> assertEquals(200, response.getStatus()),
				() -> assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString())
			);
	}
}