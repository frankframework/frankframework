package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.TestAppender;

@RunWith(Parameterized.class)
public class BusExceptionTest {

	private static final String BUS_EXCEPTION_MESSAGE = "outer exception";

	@Parameterized.Parameter(0)
	public String expectedLogMessage;

	@Parameterized.Parameter(1)
	public Level expectedLogLevel;

	@Parameterized.Parameter(2)
	public Exception innerException;

	@Parameters(name= "{0}")
	public static List<?> data() {
		return Arrays.asList(new Object[][] {
			{"", Level.WARN, null},
			{": cannot configure", Level.ERROR, new ConfigurationException("cannot configure")},
			{": cannot configure: (IllegalStateException) something is wrong", Level.ERROR, new ConfigurationException("cannot configure", new IllegalStateException("something is wrong"))},
		});
	}

	@Test
	public void testExceptionMessage() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m - %throwable{1}").build(); //only log the first line of the exception
		TestAppender.addToRootLogger(appender);
		try {
			Exception ex = new BusException(BUS_EXCEPTION_MESSAGE, innerException);
			assertEquals(BUS_EXCEPTION_MESSAGE + expectedLogMessage, ex.getMessage());

			List<String> logEvents = appender.getLogLines();
			assertEquals("expected only 1 line to be logged, got: "+logEvents, 1, logEvents.size());
			String logMessage = logEvents.get(0);
			String[] event = logMessage.split(" - ");
			String level = event[0];
			String message = event[1];
			String exception = (event.length == 3) ? event[2] : null;

			//Check log level
			assertEquals("["+level+"] did not match the expected log level ["+expectedLogLevel+"]", expectedLogLevel.name(), level);

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
