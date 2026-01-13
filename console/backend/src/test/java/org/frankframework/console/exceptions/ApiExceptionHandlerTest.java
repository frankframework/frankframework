package org.frankframework.console.exceptions;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.stream.Stream;

import jakarta.servlet.ServletException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;

import org.frankframework.console.ApiException;
import org.frankframework.console.controllers.FrankApiTestBase;
import org.frankframework.console.controllers.TestController;
import org.frankframework.console.controllers.WebTestConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, TestController.class})
public class ApiExceptionHandlerTest extends FrankApiTestBase {

	private static Stream<Arguments> urlAndExpectedExceptionStream() {
		return Stream.of(
				Arguments.of("/test/apiexception", ApiException.class, HttpStatus.INTERNAL_SERVER_ERROR.value()),
				Arguments.of("/test/mapping-not-found-exception", NoHandlerFoundException.class, HttpStatus.NOT_FOUND.value()),
				Arguments.of("/test/servletexception", ServletException.class, HttpStatus.INTERNAL_SERVER_ERROR.value()),
				Arguments.of("/test/methodnotsupportedexception", HttpRequestMethodNotSupportedException.class, HttpStatus.METHOD_NOT_ALLOWED.value())
		);
	}

	@ParameterizedTest
	@MethodSource("urlAndExpectedExceptionStream")
	public <T extends Exception> void test(String url, Class<T> expectedException, int expectedStatus) throws Exception {

		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders.get(url))
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().is(expectedStatus))
				.andReturn();

		mockResult.getResolvedException().printStackTrace();
		assertInstanceOf(expectedException, mockResult.getResolvedException());
	}
}
