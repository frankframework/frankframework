package org.frankframework.console.controllers;

import static org.frankframework.console.util.MatchUtils.assertJsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.frankframework.console.ApiException;
import org.frankframework.core.IbisException;
import org.frankframework.util.JacksonUtils;

public class ApiExceptionTest {

	private static final String API_EXCEPTION_MESSAGE = "api endpoint exception message";

	public static List<Arguments> data() {
		return List.of(
				Arguments.of("cannot configure", new IbisException("cannot configure"),
						"{\"error\":\"api endpoint exception message: cannot configure\",\"status\":\"Internal Server Error\"}"),
				Arguments.of("cannot configure: (IllegalStateException) something is wrong", new IbisException("cannot configure", new IllegalStateException("something is wrong")),
						"{\"error\":\"api endpoint exception message: cannot configure: (IllegalStateException) something is wrong\",\"status\":\"Internal Server Error\"}")
		);
	}

	public static List<Arguments> dataNoMessage() {
		return List.of(
				Arguments.of("cannot configure", new IbisException("cannot configure"),
						"{\"error\":\"cannot configure\",\"status\":\"Internal Server Error\"}"),
				Arguments.of("cannot configure: (IllegalStateException) something is wrong", new IbisException("cannot configure", new IllegalStateException("something is wrong")),
						"{\"error\":\"cannot configure: (IllegalStateException) something is wrong\",\"status\":\"Internal Server Error\"}")
		);
	}

	@Test
	public void message() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(500, response.getStatusCode().value());
		String jsonMessage = JacksonUtils.convertToJson(response.getBody());
		assertJsonEquals("{\"error\":\"api endpoint exception message\",\"status\":\"Internal Server Error\"}", jsonMessage);
	}

	@ParameterizedTest
	@MethodSource(value = "dataNoMessage")
	public void nestedNoMessage(String expectedMessage, Exception causedByException, String expectedJsonMessage) {
		ApiException exception = new ApiException(causedByException);
		assertThat(exception.getMessage(), Matchers.startsWith(expectedMessage));
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(500, response.getStatusCode().value());
		String jsonMessage = JacksonUtils.convertToJson(response.getBody());

		assertJsonEquals(expectedJsonMessage, jsonMessage);
	}

	@Test
	public void messageWithStatusCode() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, 404);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(404, response.getStatusCode().value());
		String jsonMessage = JacksonUtils.convertToJson(response.getBody());
		assertJsonEquals("{\"error\":\"api endpoint exception message\",\"status\":\"Not Found\"}", jsonMessage);
	}

	@Test
	public void messageWithStatus() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, HttpStatus.BAD_REQUEST);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(400, response.getStatusCode().value());
		String jsonMessage = JacksonUtils.convertToJson(response.getBody());
		assertEquals("{\"error\":\"api endpoint exception message\",\"status\":\"Bad Request\"}", jsonMessage);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void nestedException(String expectedMessage, Exception causedByException, String expectedJsonMessage) {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, causedByException);
		assertThat(exception.getMessage(), Matchers.startsWith(API_EXCEPTION_MESSAGE +": "+ expectedMessage));
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(500, response.getStatusCode().value());
		String jsonMessage = JacksonUtils.convertToJson(response.getBody());

		assertJsonEquals(expectedJsonMessage, jsonMessage);
	}

	@Test
	public void noNestedException() {
		ApiException exception = new ApiException((Throwable) null);
		assertNull(exception.getMessage());
		ResponseEntity<?> response = exception.getResponse();
		assertEquals(500, response.getStatusCode().value());
		assertNull(response.getBody());
	}
}
