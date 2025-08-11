package org.frankframework.console.controllers;

import static org.frankframework.console.util.MatchUtils.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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

		return switch (type) {
			case NOT_FOUND -> new BusException("resource not found", 404);
			case MESSAGE -> new BusException("message without cause");
			case CAUSE -> new IllegalStateException("uncaught exception", cause);
			case AUTHORIZATION -> new AccessDeniedException("Access Denied");
			case AUTHENTICATION -> new AuthenticationCredentialsNotFoundException("An Authentication object was not found in the SecurityContext");
			case CLIENT_EXCEPTION_400 ->
					HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "custom status text ignored", HttpHeaders.EMPTY, "some exception text".getBytes(), null);
			case CLIENT_EXCEPTION_404 ->
					HttpClientErrorException.create(HttpStatus.NOT_FOUND, "custom status text ignored", HttpHeaders.EMPTY, "http body ignored".getBytes(), null);
			default -> new BusException("message with a cause", cause);
		};
	}

	public static List<Arguments> data() {
		return List.of(
				Arguments.of(TestExceptionType.MESSAGE, HttpStatus.BAD_REQUEST,
						"{\"error\":\"message without cause\",\"status\":\"Bad Request\"}"),
				Arguments.of(TestExceptionType.MESSAGE_WITH_CAUSE, HttpStatus.INTERNAL_SERVER_ERROR,
						"{\"error\":\"message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong\",\"status\":\"Internal Server Error\"}"),
				Arguments.of(TestExceptionType.NOT_FOUND, HttpStatus.NOT_FOUND,
						"{\"error\":\"resource not found\",\"status\":\"Not Found\"}"),
				Arguments.of(TestExceptionType.CAUSE, HttpStatus.INTERNAL_SERVER_ERROR,
						"{\"error\":\"error occurred during processing message; nested exception is java.lang.IllegalStateException: uncaught exception\",\"status\":\"Internal Server Error\"}"),
				Arguments.of(TestExceptionType.AUTHORIZATION, HttpStatus.FORBIDDEN,
						"{\"error\":\"Access Denied\",\"status\":\"Forbidden\"}"),
				Arguments.of(TestExceptionType.AUTHENTICATION, HttpStatus.UNAUTHORIZED,
						"{\"error\":\"An Authentication object was not found in the SecurityContext\",\"status\":\"Unauthorized\"}"),
				Arguments.of(TestExceptionType.CLIENT_EXCEPTION_400, HttpStatus.BAD_REQUEST,
						"{\"error\":\"some exception text\",\"status\":\"Bad Request\"}"),
				Arguments.of(TestExceptionType.CLIENT_EXCEPTION_404, HttpStatus.NOT_FOUND,
						"{\"error\":\"404 - Not Found\",\"status\":\"Not Found\"}")
		);
	}

	@ParameterizedTest
	@MethodSource("data")
	void testEndpointExceptions(TestExceptionType type, HttpStatus expectedHttpStatus, String expectedJson) {
		// Arrange
		MessageHandlingException e = createException(type);

		// Act
		ResponseEntity<?> response = handler.toResponse(e);

		// Assert
		assertEquals(expectedHttpStatus.value(), response.getStatusCode().value());
		String json = JacksonUtils.convertToJson(response.getBody());
		assertJsonEquals(expectedJson, json);
	}
}
