/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

/**
 * Test-only Log4j2 appender that captures log output for assertions.
 * Attach to a specific logger with {@link #attach(Class)} and release it with {@link #close()}.
 */
public class TestAppender extends AbstractAppender implements AutoCloseable {

	private final List<String> logMessages = new ArrayList<>();
	private LoggerConfig loggerConfig;
	private Level previousLevel;

	private TestAppender() {
		super("TestAppender", null, null, false, Property.EMPTY_ARRAY);
	}

	public static TestAppender attach(Class<?> loggerClass) {
		TestAppender appender = new TestAppender();
		appender.start();

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		appender.loggerConfig = ctx.getConfiguration().getLoggerConfig(loggerClass.getName());
		appender.previousLevel = appender.loggerConfig.getLevel();
		appender.loggerConfig.setLevel(Level.WARN);
		appender.loggerConfig.addAppender(appender, Level.WARN, null);
		ctx.updateLoggers();
		return appender;
	}

	@Override
	public void append(LogEvent event) {
		logMessages.add(event.getMessage().getFormattedMessage());
	}

	public boolean contains(String msg) {
		return logMessages.stream().anyMatch(log -> log.contains(msg));
	}

	@Override
	public void close() {
		loggerConfig.removeAppender(getName());
		loggerConfig.setLevel(previousLevel);
		((LoggerContext) LogManager.getContext(false)).updateLoggers();
		stop();
	}
}
