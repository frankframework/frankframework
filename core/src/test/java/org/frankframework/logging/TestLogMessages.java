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
package org.frankframework.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.LogUtil;

public class TestLogMessages {
	protected Logger log = LogUtil.getLogger(this);

	private static final String TEST_REGEX_IN = "log my name but not my password! username=\"top\" password=\"secret\" hihi";
	private static final String TEST_REGEX_OUT = "log my name but not my password! username=\"top\" password=\"******\" hihi";
	private static final String PATTERN = "%level - %m";

	@BeforeEach
	public void setup() {
		ThreadContext.clearAll();
	}

	@Test
	public void testHideRegexMatchInLogMessage() {
		Set<Pattern> globalReplace = IbisMaskingLayout.getGlobalReplace();
		IbisMaskingLayout.clearGlobalReplace();
		// Password matching regex that is intentionally different from the default
		IbisMaskingLayout.addToGlobalReplace("(?<=password=\").+?(?=\")");
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			log.debug(TEST_REGEX_IN);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - "+ TEST_REGEX_OUT, message);
		} finally {
			IbisMaskingLayout.clearGlobalReplace();
			globalReplace.forEach(IbisMaskingLayout::addToGlobalReplace);
		}
	}

	@Test
	public void testLogHideRegexPropertyAppliedFromConfig() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			log.debug("my beautiful log with <password>TO BE HIDDEN</password> hidden value");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful log with <password>************</password> hidden value", message);
		}
	}

	@Test
	public void dontLogDebugAndInfoLevelsWhenThreadFilterIsActive() {
		String threadName = Thread.currentThread().getName();
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).useIbisThreadFilter("HIDE-HERE").build()) {
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
		}
	}

	@Test
	public void testCdataInMessage() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			log.debug("my beautiful <![CDATA[debug]]> for me & you --> \"world\"");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size());
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful <![CDATA[debug]]> for me & you --> \"world\"", message);
		}
	}

	@Test
	public void testUnicodeCharactersInMessage() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			log.debug("my beautiful unicode debug  aâΔع你好ಡತ  message for me & you --> \\\"world\\\"");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);
			assertEquals("DEBUG - my beautiful unicode debug  aâΔع你好ಡತ  message for me & you --> \\\"world\\\"", message);
		}
	}

	@Test
	public void logLength() {
		int length = 23;
		IbisMaskingLayout.setMaxLength(length);
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			log.debug(TEST_REGEX_IN);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);

			String expected = "DEBUG - "+ TEST_REGEX_IN.substring(0, length).trim() + " ...("+(TEST_REGEX_IN.length()-length)+" more characters)";
			TestAssertions.assertEqualsIgnoreCRLF(expected, message);
		}
		finally {
			IbisMaskingLayout.setMaxLength(-1);
		}
	}

	@Test
	public void logWithStacktrace() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout(PATTERN).build()) {
			Throwable t = new Throwable("my exception message");
			StackTraceElement[] stackTrace = new StackTraceElement[1];
			stackTrace[0] = new StackTraceElement(this.getClass().getSimpleName(), "logWithStacktrace", "TestLogMessages", 0);
			t.setStackTrace(stackTrace);
			log.debug("Oh no, something went wrong!", t);

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size(), "found messages "+logEvents);
			String message = logEvents.get(0);

			String expected = """
					DEBUG - Oh no, something went wrong! java.lang.Throwable: my exception message
						at TestLogMessages.logWithStacktrace(TestLogMessages:0) ~[?:?]\
					""";
			TestAssertions.assertEqualsIgnoreCRLF(expected, message);
		}
	}

	@Test
	public void throwExceptionWhenOldLog4jVersion() throws Exception {
		URL log4jOld = TestFileUtils.getTestFileURL("/Logging/log4j-old.xml");
		assertNotNull(log4jOld, "cannot find log4j-old.xml");
		InputStream oldLog4jConfiguration = log4jOld.openStream();
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			FrankLogConfigurationFactory.readLog4jConfiguration(oldLog4jConfiguration);
		});
		assertEquals("Did not recognize configuration format, unable to configure Log4j2. Please use the log4j2 layout in file log4j4ibis.xml", ex.getMessage());
	}

	@Test
	public void readLog4jConfiguration() throws Exception {
		URL log4jNew = TestFileUtils.getTestFileURL("/Logging/log4j-new.xml");
		assertNotNull(log4jNew, "cannot find log4j-new.xml");
		InputStream newLog4jConfiguration = log4jNew.openStream();

		String config = FrankLogConfigurationFactory.readLog4jConfiguration(newLog4jConfiguration);
		String expected = TestFileUtils.getTestFile("/Logging/log4j-new.xml");
		TestAssertions.assertEqualsIgnoreCRLF(expected, config);
	}

	@Test
	public void testChangeLogLevel() {
		String rootLoggerName = LogUtil.getLogger(this).getName(); //For tests, we use the `org.frankframework` logger instead of the rootlogger

		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
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
			Configurator.setLevel(rootLoggerName, Level.DEBUG);
		}
	}

	@Test
	public void testMessageLogThreadContext() throws Exception {
		PatternLayout layout =  PatternLayout.newBuilder().withPattern("%level - %m %TC").build();
		try (TestAppender appender = TestAppender.newBuilder().setLayout(layout).build()) {
			try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("key", "value").put("key.two", "value2")) {
				log.debug("Adapter Success");
			}

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size());
			String message = logEvents.get(0);
			assertAll(
				() -> assertThat(message, StringContains.containsString("DEBUG - Adapter Success ")),
				() -> assertThat(message, StringContains.containsString("key [value]")),
				() -> assertThat(message, StringContains.containsString("key-two [value2]")),
				() -> assertFalse(message.endsWith(" "), "message should not end with a space"),
				() -> assertFalse(message.contains("log.dir")) // No other info in log context
			);
		}
	}

	@Test
	public void testMessageEmptyLogThreadContext() {
		PatternLayout layout =  PatternLayout.newBuilder().withPattern("%level - %m %TC").build();
		try (TestAppender appender = TestAppender.newBuilder().setLayout(layout).build()) {
			log.debug("Adapter Success");

			List<String> logEvents = appender.getLogLines();
			assertEquals(1, logEvents.size());
			String message = logEvents.get(0);
			assertEquals("DEBUG - Adapter Success ", message);
		}
	}
}
