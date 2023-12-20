package org.frankframework.management.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.frankframework.core.IbisException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ApiExceptionTest {

	private static final String API_EXCEPTION_MESSAGE = "api endpoint exception message";

	public static List<?> data() {
		return Arrays.asList(new Object[][] {
			{"cannot configure", new IbisException("cannot configure")},
			{"cannot configure: (IllegalStateException) something is wrong", new IbisException("cannot configure", new IllegalStateException("something is wrong"))},
		});
	}

	public static String toJsonString(Object entity) {
		assertTrue(entity instanceof ApiException.FormattedJsonEntity);
		ApiException.FormattedJsonEntity fje = (ApiException.FormattedJsonEntity) entity;
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		fje.write(boas);
		String json = boas.toString();
		return json.split("error\": \"")[1].replace("\"\n}", "");
	}

	@Test
	public void message() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		Response response = exception.getResponse();
		assertEquals(500, response.getStatus());
		String jsonMessage = toJsonString(response.getEntity());
		assertEquals(API_EXCEPTION_MESSAGE, jsonMessage);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void nestedNoMessage(String expectedMessage, Exception causedByException) {
		ApiException exception = new ApiException(causedByException);
		assertThat(exception.getMessage(), Matchers.startsWith(expectedMessage));
		Response response = exception.getResponse();
		assertEquals(500, response.getStatus());
		String jsonMessage = toJsonString(response.getEntity());
		assertThat(jsonMessage, Matchers.startsWith(expectedMessage));
		exception.printStackTrace();
	}

	@Test
	public void messageWithStatusCode() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, 404);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		Response response = exception.getResponse();
		assertEquals(404, response.getStatus());
		String jsonMessage = toJsonString(response.getEntity());
		assertEquals(API_EXCEPTION_MESSAGE, jsonMessage);
	}

	@Test
	public void messagetWithStatus() {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, Status.BAD_REQUEST);
		assertEquals(API_EXCEPTION_MESSAGE, exception.getMessage());
		Response response = exception.getResponse();
		assertEquals(400, response.getStatus());
		String jsonMessage = toJsonString(response.getEntity());
		assertEquals(API_EXCEPTION_MESSAGE, jsonMessage);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void nestedException(String expectedMessage, Exception causedByException) {
		ApiException exception = new ApiException(API_EXCEPTION_MESSAGE, causedByException);
		assertThat(exception.getMessage(), Matchers.startsWith(API_EXCEPTION_MESSAGE +": "+ expectedMessage));
		Response response = exception.getResponse();
		assertEquals(500, response.getStatus());
		String jsonMessage = toJsonString(response.getEntity());
		assertThat(jsonMessage, Matchers.startsWith("api endpoint exception message: cannot configure"));

		assertThat(jsonMessage, Matchers.startsWith(API_EXCEPTION_MESSAGE +": "+ expectedMessage));
		exception.printStackTrace();
	}

	@Test
	public void noNestedException() {
		ApiException exception = new ApiException((Throwable) null);
		assertNull(exception.getMessage());
		Response response = exception.getResponse();
		assertEquals(500, response.getStatus());
		assertNull(response.getEntity());
	}
}
