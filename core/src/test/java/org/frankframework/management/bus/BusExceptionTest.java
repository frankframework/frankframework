package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.testutil.TestAppender;

class BusExceptionTest {

	private static final String BUS_EXCEPTION_MESSAGE = "outer exception";

	private static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of("", Level.WARN, null),
				Arguments.of(": cannot configure", Level.ERROR, new ConfigurationException("cannot configure")),
				Arguments.of(": cannot configure: (IllegalStateException) something is wrong", Level.ERROR, new ConfigurationException("cannot configure", new IllegalStateException("something is wrong")))
		);
	}

	@ParameterizedTest
	@MethodSource("data")
	void testExceptionMessage(String expectedLogMessage, Level expectedLogLevel, Exception innerException) {
		//only log the first line of the exception
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m - %throwable{1}").build()) {
			Exception ex = new BusException(BUS_EXCEPTION_MESSAGE, innerException);
			assertEquals(BUS_EXCEPTION_MESSAGE + expectedLogMessage, ex.getMessage());

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "expected only 1 line to be logged, got: "+logEvents);
			String logMessage = logEvents.get(0);
			String[] event = logMessage.split(" - ");
			String level = event[0];
			String message = event[1];
			String exception = event.length == 3 ? event[2] : null;

			//Check log level
			assertEquals(expectedLogLevel.name(), level, "["+level+"] did not match the expected log level ["+expectedLogLevel+"]");

			//Check log message
			assertEquals("outer exception", message.trim());

			//Check log exception
			if(exception != null) {
				assertEquals("org.frankframework.configuration.ConfigurationException" + expectedLogMessage, exception.trim());
			}
		}
	}
}
