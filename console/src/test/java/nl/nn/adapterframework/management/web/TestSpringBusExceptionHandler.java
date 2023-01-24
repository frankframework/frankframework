package nl.nn.adapterframework.management.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTestEndpoints.ExceptionTestTypes;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.SpringRootInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {SpringRootInitializer.class})
public class TestSpringBusExceptionHandler extends BusTestBase {
	private SpringBusExceptionHandler handler = new SpringBusExceptionHandler();

	@Test
	public void testEndpointMessageException() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE.name());
		try {
			callSyncGateway(request);
		} catch (MessageHandlingException e) {
			Response response = handler.toResponse(e);
			assertEquals(500, response.getStatus());
			String json = ApiExceptionTest.toJsonString(response.getEntity());
			assertEquals("message with a cause", json);
		}
	}

	@Test
	public void testEndpointMessageWithCauseException() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE_WITH_CAUSE.name());
		try {
			callSyncGateway(request);
		} catch (MessageHandlingException e) {
			Response response = handler.toResponse(e);
			assertEquals(500, response.getStatus());
			String json = ApiExceptionTest.toJsonString(response.getEntity());
			assertEquals("message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong", json);
		}
	}

	@Test
	public void testEndpointCauseException() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.CAUSE.name());
		try {
			callSyncGateway(request);
		} catch (MessageHandlingException e) {
			Response response = handler.toResponse(e);
			assertEquals(500, response.getStatus());
			String json = ApiExceptionTest.toJsonString(response.getEntity());
			assertThat(json, Matchers.startsWith("error occurred during processing message in 'MethodInvokingMessageProcessor'"));
			assertThat(json, Matchers.endsWith("nested exception is nl.nn.adapterframework.stream.StreamingException: cannot stream: cannot configure: (IllegalStateException) something is wrong"));
		}
	}

	@Test
	public void testEndpointMessageWithAuthenticationError() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		try {
			callSyncGateway(request);
		} catch (MessageHandlingException e) {
			Response response = handler.toResponse(e);
			assertEquals(401, response.getStatus());
			String json = ApiExceptionTest.toJsonString(response.getEntity());
			assertEquals("An Authentication object was not found in the SecurityContext", json);
		}
	}

	@Test
	@WithMockUser(authorities = { "lala" })
	public void testEndpointMessageWithAuthorizationError() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		try {
			callSyncGateway(request);
		} catch (MessageHandlingException e) {
			Response response = handler.toResponse(e);
			assertEquals(403, response.getStatus());
			String json = ApiExceptionTest.toJsonString(response.getEntity());
			assertEquals("Access Denied", json);
		}
	}
}