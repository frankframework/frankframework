/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.console.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;

public class TestAppender extends AbstractAppender implements AutoCloseable {
	private final List<String> logMessages = new ArrayList<>();
	private final List<LogEvent> logEvents = new ArrayList<>();
	private Level minLogLevel = Level.DEBUG;

	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder().setName("jUnit-Test-Appender");
	}

	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B> {

		private Level minLogLevel = null;


		public B minLogLevel(Level level) {
			this.minLogLevel = level;
			return asBuilder();
		}

		public TestAppender build() {
			TestAppender appender = new TestAppender(getName(), getFilter(), getOrCreateLayout());
			if (this.minLogLevel != null) {
				appender.minLogLevel = this.minLogLevel;
			}
			return appender;
		}
	}

	private TestAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
		super(name, filter, layout, false, null);
		start();
		addToRootLogger(this);
	}

	@Override
	public void close() throws Exception {
		removeAppender(this);
	}

	@Override
	public void append(LogEvent logEvent) {
		if (this.minLogLevel != null && !logEvent.getLevel().isMoreSpecificThan(this.minLogLevel)) {
			return;
		}
		logMessages.add((String) this.toSerializable(logEvent));
		logEvents.add(logEvent);
	}

	public int getNumberOfAlerts() {
		return logMessages.size();
	}

	public List<String> getLogLines() {
		return new ArrayList<>(logMessages);
	}

	public List<LogEvent> getLogEvents() {
		return new ArrayList<>(logEvents);
	}

	private static Logger getRootLogger() {
		return (Logger) LogManager.getRootLogger();
	}

	private void addToRootLogger(TestAppender appender) {
		Logger logger = getRootLogger();
		logger.addAppender(appender);
	}

	public static void addToLogger(String loggerName, TestAppender appender) {
		Logger logger = (Logger) LogManager.getLogger(loggerName);
		logger.addAppender(appender);
	}

	private void removeAppender(TestAppender appender) {
		Logger logger = getRootLogger();
		logger.removeAppender(appender);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String log : getLogLines()) {
			sb.append(log);
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean contains(String msg) {
		for (String log : getLogLines()) {
			if(log.contains(msg)) {
				return true;
			}
		}
		return false;
	}

	public void clearLogs() {
		logMessages.clear();
	}

	public static void setRootLogLevel(Level loglevel) {
		LoggerContext logContext = LoggerContext.getContext(false);
		org.apache.logging.log4j.core.Logger rootLogger = logContext.getRootLogger();
		if(rootLogger.getLevel() != loglevel) {
			Configurator.setLevel(rootLogger.getName(), loglevel);
		}
	}
}
