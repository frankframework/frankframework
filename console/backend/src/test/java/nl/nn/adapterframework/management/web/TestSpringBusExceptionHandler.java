package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.HttpClientErrorException;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.management.bus.BusException;

public class TestSpringBusExceptionHandler {
	private SpringBusExceptionHandler handler = new SpringBusExceptionHandler();
	public enum TestExceptionType {
		MESSAGE, MESSAGE_WITH_CAUSE, CAUSE, AUTHORIZATION, AUTHENTICATION, CLIENT_EXCEPTION
	}

	private MessageHandlingException createException(TestExceptionType type) {
		Exception cause = createExceptionCause(type);

		GenericMessage<String> message = new GenericMessage<>("dummy message");
		return new MessageHandlingException(message, "error occurred during processing message", cause);
	}

	private Exception createExceptionCause(TestExceptionType type) {
		Exception cause = new IbisException("cannot stream",
			new IbisException("cannot configure",
				new IllegalStateException("something is wrong")));

		switch (type) {
		case MESSAGE:
			return new BusException("message without cause");
		case CAUSE:
			return new IllegalStateException("uncaught exception", cause);
		case AUTHORIZATION:
			return new AccessDeniedException("Access Denied");
		case AUTHENTICATION:
			return new AuthenticationCredentialsNotFoundException("An Authentication object was not found in the SecurityContext");
		case CLIENT_EXCEPTION:
			return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "custom status text ignored", HttpHeaders.EMPTY, "http body ignored".getBytes(), null);
		case MESSAGE_WITH_CAUSE:
		default:
			return new BusException("message with a cause", cause);
		}
	}

	@Test
	public void testEndpointMessageException() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.MESSAGE);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(500, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("message without cause", json);
	}

	@Test
	public void testEndpointMessageWithCauseException() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.MESSAGE_WITH_CAUSE);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(500, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong", json);
	}

	@Test
	public void testEndpointCauseException() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.CAUSE);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(500, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("error occurred during processing message; nested exception is java.lang.IllegalStateException: uncaught exception", json);
	}

	@Test
	public void testEndpointMessageWithAuthenticationError() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.AUTHENTICATION);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(401, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("An Authentication object was not found in the SecurityContext", json);
	}

	@Test
	@WithMockUser(authorities = { "lala" })
	public void testEndpointMessageWithAuthorizationError() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.AUTHORIZATION);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(403, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("Access Denied", json);
	}

	@Test
	public void testExceptionWithCustomMessage() {
		// Arrange
		MessageHandlingException e = createException(TestExceptionType.CLIENT_EXCEPTION);

		// Act
		Response response = handler.toResponse(e);

		// Assert
		assertEquals(503, response.getStatus());
		String json = ApiExceptionTest.toJsonString(response.getEntity());
		assertEquals("404 - Not Found", json);
	}
}