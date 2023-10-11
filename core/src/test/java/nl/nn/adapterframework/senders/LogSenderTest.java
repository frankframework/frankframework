package nl.nn.adapterframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAppender;

public class LogSenderTest extends SenderTestBase<LogSender> {

	@Override
	public LogSender createSender() {
		return new LogSender();
	}

	@ParameterizedTest
	@ValueSource(strings = {"DEBUG", "INFO", "WARN", "ERROR"})
	public void defaultLogSettings(String level) throws Exception {
		sender.setLogLevel(level);
		sender.configure();
		sender.open();

		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level: %m").build();
		TestAppender.addToRootLogger(appender);

		try {
			String input = "Message2Log";
			Message message = new Message(input);
			String result = sender.sendMessageOrThrow(message, session).asString();

			assertEquals(input, result);
			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			assertEquals(level+": Message2Log", logEvents.get(0));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void faultyLogLevel() throws Exception {
		sender.setLogLevel("appelflap");
		assertThrows(IllegalArgumentException.class, sender::configure);
	}

	@ParameterizedTest
	@ValueSource(strings = {"INFO", "WARN", "ERROR"})
	public void dynamicParameter(String level) throws Exception {
		sender.addParameter(ParameterBuilder.create("logLevel", level));
		sender.configure();
		sender.open();

		TestAppender appender = TestAppender.newBuilder().minLogLevel(Level.INFO).useIbisPatternLayout("%level: %m").build();
		TestAppender.addToRootLogger(appender);

		try {
			String input = "Message2Log";
			Message message = new Message(input);
			String result = sender.sendMessageOrThrow(message, session).asString();

			assertEquals(input, result);
			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			assertEquals(level+": Message2Log", logEvents.get(0));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void faultyDynamicParameterShouldFallBackToAttribute() throws Exception {
		sender.addParameter(ParameterBuilder.create("logLevel", "perensap"));
		sender.setLogLevel("WARN");
		sender.configure();
		sender.open();

		TestAppender appender = TestAppender.newBuilder().minLogLevel(Level.INFO).useIbisPatternLayout("%level: %m").build();
		TestAppender.addToRootLogger(appender);

		try {
			String input = "Message2Log";
			Message message = new Message(input);
			String result = sender.sendMessageOrThrow(message, session).asString();

			assertEquals(input, result);
			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			assertEquals("WARN: Message2Log", logEvents.get(0));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void emptyLogLevelAttributeWithFaultyParameterShouldUseDEBUG() throws Exception {
		sender.addParameter(ParameterBuilder.create("logLevel", "perensap"));
		sender.setLogLevel("");
		sender.configure();
		sender.open();

		TestAppender appender = TestAppender.newBuilder().minLogLevel(Level.DEBUG).useIbisPatternLayout("%level: %m").build();
		TestAppender.addToRootLogger(appender);

		try {
			String input = "Message2Log";
			Message message = new Message(input);
			String result = sender.sendMessageOrThrow(message, session).asString();

			assertEquals(input, result);
			List<String> logEvents = appender.getLogLines();
			assertEquals(3, logEvents.size(), "found messages "+logEvents);
			assertEquals("DEBUG: Calculating value for Parameter [logLevel]", logEvents.get(0));
			assertEquals("DEBUG: Parameter [logLevel] resolved to [perensap]", logEvents.get(1));
			assertEquals("DEBUG: Message2Log", logEvents.get(2));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void withParameters() throws Exception {
		sender.setLogLevel("INFO");
		sender.addParameter(ParameterBuilder.create("logLevel", "")); // should be ignored
		sender.addParameter(ParameterBuilder.create("param 1", "appelflap"));
		sender.addParameter(ParameterBuilder.create("param 2", "perensap"));
		sender.configure();
		sender.open();

		TestAppender appender = TestAppender.newBuilder().minLogLevel(Level.INFO).useIbisPatternLayout("%level: %m").build();
		TestAppender.addToRootLogger(appender);

		try {
			String input = "Message2Log";
			Message message = new Message(input);
			String result = sender.sendMessageOrThrow(message, session).asString();

			assertEquals(input, result);
			List<String> logEvents = appender.getLogLines();
			assertEquals(3, logEvents.size(), "found messages "+logEvents);
			assertEquals("INFO: Message2Log", logEvents.get(0));
			assertEquals("INFO: parameter [param 1] value [appelflap]", logEvents.get(1));
			assertEquals("INFO: parameter [param 2] value [perensap]", logEvents.get(2));
		} finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testIfToStringWorks() {
		assertNotNull(sender.toString()); //And no NPE
	}
}
