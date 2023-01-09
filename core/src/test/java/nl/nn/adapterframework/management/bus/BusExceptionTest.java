package nl.nn.adapterframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.TestAppender;

public class BusExceptionTest {

	private static final String BUS_EXCEPTION_MESSAGE = "outer exception";

	public String expectedLogMessage;
	public Level expectedLogLevel;
	public Exception innerException;

	@Parameters(name= "{0}")
	public static List<?> data() {
		return Arrays.asList(new Object[][] {
			{"", Level.WARN, null},
			{": cannot configure", Level.ERROR, new ConfigurationException("cannot configure")},
			{": cannot configure: (IllegalStateException) something is wrong", Level.ERROR, new ConfigurationException("cannot configure", new IllegalStateException("something is wrong"))},
		});
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testExceptionMessage(String expectedLogMessage, Level expectedLogLevel, Exception innerException) {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m - %throwable{1}").build(); //only log the first line of the exception
		TestAppender.addToRootLogger(appender);
		try {
			Exception ex = new BusException(BUS_EXCEPTION_MESSAGE, innerException);
			assertEquals(BUS_EXCEPTION_MESSAGE + expectedLogMessage, ex.getMessage());

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "expected only 1 line to be logged, got: "+logEvents);
			String logMessage = logEvents.get(0);
			String[] event = logMessage.split(" - ");
			String level = event[0];
			String message = event[1];
			String exception = (event.length == 3) ? event[2] : null;

			//Check log level
			assertEquals(expectedLogLevel.name(), level, "["+level+"] did not match the expected log level ["+expectedLogLevel+"]");

			//Check log message
			assertEquals("outer exception", message);

			//Check log exception
			if(exception != null) {
				assertEquals("nl.nn.adapterframework.configuration.ConfigurationException"+ expectedLogMessage, exception);
			}
		} finally {
			TestAppender.removeAppender(appender);
		}
	}
}
