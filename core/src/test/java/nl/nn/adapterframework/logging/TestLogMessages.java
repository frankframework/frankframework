/*
   Copyright 2020-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.LogUtil;

public class TestLogMessages {
	protected Logger log = LogUtil.getLogger(this);

	private static String TEST_REGEX_IN  = "log my name but not my password! username=\"top\" password=\"secret\" hihi";
	private static String TEST_REGEX_OUT = "log my name but not my password! username=\"top\" password=\"******\" hihi";
	private static String PATTERN = "%level - %m";

	@Test
	public void hidePasswordInLogMessages() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		IbisMaskingLayout.addToGlobalReplace("(?<=password=\").+?(?=\")");
		try {
			log.debug(TEST_REGEX_IN);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - "+ TEST_REGEX_OUT, message);
		}
		finally {
			IbisMaskingLayout.cleanGlobalReplace();
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void dontLogDebugAndInfoLevelsWhenThreadFilterIsActive() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).useIbisThreadFilter("HIDE-HERE").build();
		TestAppender.addToRootLogger(appender);
		String threadName = Thread.currentThread().getName();
		try {
			Thread.currentThread().setName("HIDE-HERE");
			log.debug("my beautiful debug message");
			log.info("my beautiful info message");
			log.warn("my beautiful warning message");
			log.error("my beautiful error message");

			Thread.currentThread().setName("LOG-ALL");
			log.debug("some message");
			log.info("some message");
			log.warn("some message");
			log.error("some message");

			List<String> logEvents = appender.getLogLines();
			assertEquals(6, logEvents.size(), "found messages "+logEvents);
			assertEquals("WARN - my beautiful warning message", logEvents.get(0));
			assertEquals("ERROR - my beautiful error message", logEvents.get(1));
		}
		finally {
			Thread.currentThread().setName(threadName);
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testXmlLayout() {
		TestAppender appender = TestAppender.newBuilder().useIbisXmlLayout().useIbisThreadFilter("HIDE-HERE").build();
		TestAppender.addToRootLogger(appender);
		String threadName = Thread.currentThread().getName();
		try {
			Thread.currentThread().setName("HIDE-HERE");
			log.debug("my beautiful debug <![CDATA[message]]> for me & you --> \"world\"");
			log.info("my beautiful info <![CDATA[message]]> for me & you --> \"world\"");
			log.warn("my beautiful warning <![CDATA[message]]> for me & you --> \"world\"");
			log.error("my beautiful error <![CDATA[message]]> for me & you --> \"world\"");

			Thread.currentThread().setName("LOG-ALL");
			log.debug("some message");
			log.info("some message");
			log.warn("some message");
			log.error("some message");

			List<String> logEvents = appender.getLogLines();
			assertEquals(6, logEvents.size(), "found messages "+logEvents);

			String expectedWarn = "<event logger=\"nl.nn.adapterframework.logging.TestLogMessages\" timestamp=\"xxx\" level=\"WARN\" thread=\"HIDE-HERE\">\n" +
			"  <message>my beautiful warning &lt;![CDATA[message]]&gt; for me &amp; you --&gt; \\\"world\\\"</message>\n" +
			"</event>";
			String expectedError = "<event logger=\"nl.nn.adapterframework.logging.TestLogMessages\" timestamp=\"xxx\" level=\"ERROR\" thread=\"HIDE-HERE\">\n" +
			"  <message>my beautiful error &lt;![CDATA[message]]&gt; for me &amp; you --&gt; \\\"world\\\"</message>\n" +
			"</event>";

			//Remove the timestamp
			String actualWarn = logEvents.get(0).replaceAll("(?<=timestamp=\").+?(?=\")", "xxx");
			String actualError = logEvents.get(1).replaceAll("(?<=timestamp=\").+?(?=\")", "xxx");

			TestAssertions.assertEqualsIgnoreCRLF(expectedWarn, actualWarn);
			TestAssertions.assertEqualsIgnoreCRLF(expectedError, actualError);
		}
		finally {
			Thread.currentThread().setName(threadName);
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testCdataInMessage() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		try {
			log.debug("my beautiful <![CDATA[debug]]> for me & you --> \"world\"");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size());
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful <![CDATA[debug]]> for me & you --> \"world\"", message);
		}
		finally {
			IbisMaskingLayout.cleanGlobalReplace();
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testUnicodeCharactersInMessage() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		try {
			log.debug("my beautiful unicode debug  aâΔع你好ಡತ  message for me & you --> \\\"world\\\"");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful unicode debug  aâΔع你好ಡತ  message for me & you --> \\\"world\\\"", message);
		}
		finally {
			IbisMaskingLayout.cleanGlobalReplace();
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testHideRegexMatchInMessage() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		try {
			log.debug("my beautiful log with <password>TO BE HIDDEN</password> hidden value");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful log with <password>************</password> hidden value", message);
		}
		finally {
			IbisMaskingLayout.cleanGlobalReplace();
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void testXmlLayoutWithUnicodeAndCdata() {
		TestAppender appender = TestAppender.newBuilder().useIbisXmlLayout().build();
		TestAppender.addToRootLogger(appender);
		try {
			log.debug("my beautiful  aâΔع你好ಡತ  debug <![CDATA[message]]> for me & you --> \"world\"");
			log.info("my beautiful  aâΔع你好ಡತ  info <![CDATA[message]]> for me & you --> \"world\"");

			List<String> logEvents = appender.getLogLines();
			assertEquals(2, logEvents.size(), "found messages "+logEvents);

			String expectedWarn = "<event logger=\"nl.nn.adapterframework.logging.TestLogMessages\" timestamp=\"xxx\" level=\"DEBUG\" thread=\"main\">\n" +
			"  <message>my beautiful \\u0010 a\\u00E2\\u0394\\u0639\\u4F60\\u597D\\u0CA1\\u0CA4  debug &lt;![CDATA[message]]&gt; for me &amp; you --&gt; \\\"world\\\"</message>\n" +
			"</event>";
			String expectedError = "<event logger=\"nl.nn.adapterframework.logging.TestLogMessages\" timestamp=\"xxx\" level=\"INFO\" thread=\"main\">\n" +
			"  <message>my beautiful \\u0010 a\\u00E2\\u0394\\u0639\\u4F60\\u597D\\u0CA1\\u0CA4  info &lt;![CDATA[message]]&gt; for me &amp; you --&gt; \\\"world\\\"</message>\n" +
			"</event>";

			//Remove the timestamp
			String actualWarn = logEvents.get(0).replaceAll("(?<=timestamp=\").+?(?=\")", "xxx");
			String actualError = logEvents.get(1).replaceAll("(?<=timestamp=\").+?(?=\")", "xxx");

			TestAssertions.assertEqualsIgnoreCRLF(expectedWarn, actualWarn);
			TestAssertions.assertEqualsIgnoreCRLF(expectedError, actualError);
		}
		finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void logLength() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		int length = 23;
		IbisMaskingLayout.setMaxLength(length);
		try {
			log.debug(TEST_REGEX_IN);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);

			String expected = "DEBUG - "+ TEST_REGEX_IN.substring(0, length).trim() + " ...("+(TEST_REGEX_IN.length()-length)+" more characters)";
			TestAssertions.assertEqualsIgnoreCRLF(expected, message);
		}
		finally {
			IbisMaskingLayout.setMaxLength(-1);
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void logWithStacktrace() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build();
		TestAppender.addToRootLogger(appender);
		try {
			Throwable t = new Throwable("my exception message");
			StackTraceElement[] stackTrace = new StackTraceElement[1];
			stackTrace[0] = new StackTraceElement(this.getClass().getSimpleName(), "logWithStacktrace", "TestLogMessages", 0);
			t.setStackTrace(stackTrace);
			log.debug("Oh no, something went wrong!", t);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);

			String expected = "DEBUG - Oh no, something went wrong! java.lang.Throwable: my exception message\n" +
					"	at TestLogMessages.logWithStacktrace(TestLogMessages:0) ~[?:?]";
			TestAssertions.assertEqualsIgnoreCRLF(expected, message);
		}
		finally {
			TestAppender.removeAppender(appender);
		}
	}

	@Test
	public void throwExceptionWhenOldLog4jVersion() throws Exception {
		URL log4jOld = TestFileUtils.getTestFileURL("/Logging/log4j-old.xml");
		assertNotNull(log4jOld, "cannot find log4j-old.xml");
		InputStream oldLog4jConfiguration = log4jOld.openStream();
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			IbisLoggerConfigurationFactory.readLog4jConfiguration(oldLog4jConfiguration);
		});
		assertEquals("Did not recognize configuration format, unable to configure Log4j2. Please use the log4j2 layout in file log4j4ibis.xml", ex.getMessage());
	}

	@Test
	public void readLog4jConfiguration() throws Exception {
		URL log4jNew = TestFileUtils.getTestFileURL("/Logging/log4j-new.xml");
		assertNotNull(log4jNew, "cannot find log4j-new.xml");
		InputStream newLog4jConfiguration = log4jNew.openStream();

		String config = IbisLoggerConfigurationFactory.readLog4jConfiguration(newLog4jConfiguration);
		String expected = TestFileUtils.getTestFile("/Logging/log4j-new.xml");
		TestAssertions.assertEqualsIgnoreCRLF(expected, config);
	}

	@Test
	public void testChangeLogLevel() {
		TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build();
		TestAppender.addToRootLogger(appender);
		String rootLoggerName = LogUtil.getLogger(this).getName(); //For tests, we use the `nl.nn` logger instead of the rootlogger

		try {
			Configurator.setLevel(rootLoggerName, Level.DEBUG);
			log.debug("debug");

			Configurator.setLevel(rootLoggerName, Level.INFO);
			log.debug("debug");
			log.info("info");

			Configurator.setLevel(rootLoggerName, Level.WARN);
			log.debug("debug");
			log.info("info");
			log.warn("warn");

			Configurator.setLevel(rootLoggerName, Level.ERROR);
			log.debug("debug");
			log.info("info");
			log.warn("warn");
			log.error("error");

			assertEquals(4, appender.getNumberOfAlerts(), "found messages "+appender.getLogLines());
		} finally {
			TestAppender.removeAppender(appender);
			Configurator.setLevel(rootLoggerName, Level.DEBUG);
		}
	}
}
