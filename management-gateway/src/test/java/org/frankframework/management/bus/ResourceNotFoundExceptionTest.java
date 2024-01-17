package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.frankframework.core.IbisException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceNotFoundExceptionTest {

	private static final String RESOURCE_NOT_FOUND_EXCEPTION_MESSAGE = "outer exception";

	public static Stream<Arguments> data() {
		return Stream.of(
				Arguments.arguments("", Level.WARN, null),
				Arguments.arguments(": cannot configure", Level.ERROR, new IbisException("cannot configure")),
				Arguments.arguments(": cannot configure: (IllegalStateException) something is wrong", Level.ERROR, new IbisException("cannot configure", new IllegalStateException("something is wrong")))
		);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testExceptionMessage(String expectedLogMessage, Level expectedLogLevel, Exception innerException) {
		Exception ex = new ResourceNotFoundException(RESOURCE_NOT_FOUND_EXCEPTION_MESSAGE, innerException);
		assertEquals(RESOURCE_NOT_FOUND_EXCEPTION_MESSAGE + expectedLogMessage, ex.getMessage());
	}
}
