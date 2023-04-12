package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		public Message answer(InvocationOnMock invocation) {
			Object input = invocation.getArguments()[0];
			RequestMessageBuilder request = (RequestMessageBuilder)input;
			assertEquals(BusTopic.MONITORING, request.getTopic());
			return new JsonResponseMessage(input);
		}

	}

	@Test
	public void test123() {
		// Arrange
		doAnswer(new DefaultSuccessAnswer()).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		// Act
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/configurations/TestConfiguration/monitors");

		// Assert
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("{\"topic\":\"MONITORING\",\"action\":\"GET\"}", response.getEntity());
	}
}