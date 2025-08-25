package org.frankframework.metrics;

import static org.frankframework.metrics.PrometheusMeterServlet.FALLBACK_CONTENT_TYPE;
import static org.frankframework.metrics.PrometheusMeterServlet.PRIMARY_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;

class PrometheusMeterServletTest {

	PrometheusMeterServlet servlet = new PrometheusMeterServlet();

	MockHttpServletRequest request = new MockHttpServletRequest();


	private static Stream<Arguments> contentTypeArguments() {
		return Stream.of(
				Arguments.of(PRIMARY_CONTENT_TYPE, "application/openmetrics-text; version=1.0.0; charset=utf-8"),
				Arguments.of(FALLBACK_CONTENT_TYPE, "text/plain")
		);
	}

	@MethodSource("contentTypeArguments")
	@ParameterizedTest
	void determineContentType(String expectedType, String actualType) {
		request.addHeader("Accept", actualType);

		String actualContentType = servlet.determineContentType(request);
		assertEquals(expectedType, actualContentType);
	}
}
