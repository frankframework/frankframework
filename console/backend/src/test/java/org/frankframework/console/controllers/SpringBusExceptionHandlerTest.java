package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.HttpClientErrorException;

import org.frankframework.console.configuration.SpringBusExceptionHandler;
import org.frankframework.core.IbisException;
import org.frankframework.management.bus.BusException;
import org.frankframework.util.JacksonUtils;

public class SpringBusExceptionHandlerTest {

	private final SpringBusExceptionHandler handler = new SpringBusExceptionHandler();
	public enum TestExceptionType {
		MESSAGE, MESSAGE_WITH_CAUSE, CAUSE, AUTHORIZATION, AUTHENTICATION, CLIENT_EXCEPTION_400, CLIENT_EXCEPTION_404, NOT_FOUND
	}

	private MessageHandlingException createException(SpringBusExceptionHandlerTest.TestExceptionType type) {
		Exception cause = createExceptionCause(type);

		GenericMessage<String> message = new GenericMessage<>("dummy message");
		return new MessageHandlingException(message, "error occurred during processing message", cause);
	}

	private Exception createExceptionCause(SpringBusExceptionHandlerTest.TestExceptionType type) {
		Exception cause = new IbisException("cannot stream",
				new IbisException("cannot configure",
						new IllegalStateException("something is wrong")));

		switch (type) {
			case NOT_FOUND:
				return new BusException("resource not found", 404);
			case MESSAGE:
				return new BusException("message without cause");
			case CAUSE:
				return new IllegalStateException("uncaught exception", cause);
			case AUTHORIZATION:
				return new AccessDeniedException("Access Denied");
			case AUTHENTICATION:
				return new AuthenticationCredentialsNotFoundException("An Authentication object was not found in the SecurityContext");
			case CLIENT_EXCEPTION_400:
				return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "custom status text ignored", HttpHeaders.EMPTY, "some exception text".getBytes(), null);
			case CLIENT_EXCEPTION_404:
				return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "custom status text ignored", HttpHeaders.EMPTY, "http body ignored".getBytes(), null);
			case MESSAGE_WITH_CAUSE:
			default:
				return new BusException("message with a cause", cause);
		}
	}

	@Test
	public void testEndpointMessageException() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.MESSAGE);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(400, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("message without cause", json);
	}

	@Test
	public void testEndpointMessageWithCauseException() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.MESSAGE_WITH_CAUSE);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(500, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong", json);
	}

	@Test
	public void testEndpointNotFoundException() {
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.NOT_FOUND);

		ResponseEntity<?> response = handler.toResponse(e);

		assertEquals(404, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("resource not found", json);
	}

	@Test
	public void testEndpointCauseException() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.CAUSE);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(500, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("error occurred during processing message; nested exception is java.lang.IllegalStateException: uncaught exception", json);
	}

	@Test
	public void testEndpointMessageWithAuthenticationError() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.AUTHENTICATION);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(401, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("An Authentication object was not found in the SecurityContext", json);
	}

	@Test
	@WithMockUser(authorities = { "lala" })
	public void testEndpointMessageWithAuthorizationError() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.AUTHORIZATION);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(403, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("Access Denied", json);
	}

	@Test
	public void test400ExceptionWithCustomMessage() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.CLIENT_EXCEPTION_400);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(400, response.getStatusCode().value());
		assertEquals("some exception text", asJsonString(response.getBody()));
	}

	@Test
	public void test404ExceptionWithCustomMessage() {
		// Arrange
		MessageHandlingException e = createException(SpringBusExceptionHandlerTest.TestExceptionType.CLIENT_EXCEPTION_404);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(404, response.getStatusCode().value());
		String json = asJsonString(response.getBody());
		assertEquals("404 - Not Found", json);
	}

	private String asJsonString(final Object obj) {
		try {
			String json = JacksonUtils.convertToJson(obj);
			ApiExceptionTest.ApiErrorResponse response = JacksonUtils.convertToDTO(json, ApiExceptionTest.ApiErrorResponse.class);
			return response.error();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
