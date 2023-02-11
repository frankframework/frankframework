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
package nl.nn.adapterframework.testutil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import nl.nn.adapterframework.logging.IbisPatternLayout;
import nl.nn.adapterframework.logging.IbisThreadFilter;
import nl.nn.adapterframework.logging.IbisXmlLayout;
import nl.nn.adapterframework.util.LogUtil;

public class TestAppender extends AbstractAppender {
	private final List<String> logMessages = new ArrayList<>();
	private final List<LogEvent> logEvents = new ArrayList<>();
	private Level minLogLevel = Level.DEBUG;

	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder().setName("jUnit-Test-Appender");
	}

	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B> {

		private Level minLogLevel = null;
		private Long onlyFromThread = null;

		public B useIbisThreadFilter(String rejectRegex) {
			IbisThreadFilter threadFilter = IbisThreadFilter.createFilter(rejectRegex, Level.WARN, Result.DENY, Result.NEUTRAL);
			return setFilter(threadFilter);
		}

		//Default pattern: %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%t] %x %c{2} - %m%n
		public B useIbisPatternLayout(String pattern) {
			IbisPatternLayout layout = IbisPatternLayout.createLayout(pattern, getConfiguration(), null, true, false, false);
			return setLayout(layout);
		}

		public B useIbisXmlLayout() {
			IbisXmlLayout layout = IbisXmlLayout.createLayout(getConfiguration(), null, false);
			return setLayout(layout);
		}

		public B minLogLevel(Level level) {
			this.minLogLevel = level;
			return asBuilder();
		}

		public TestAppender build() {
			TestAppender appender = new TestAppender(getName(), getFilter(), getOrCreateLayout());
			if (minLogLevel != null) {
				appender.minLogLevel = this.minLogLevel;
			}
			return appender;
		}
	}

	private TestAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
		super(name, filter, layout, false, null);
		start();
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
		return new ArrayList<String>(logMessages);
	}

	public List<LogEvent> getLogEvents() {
		return new ArrayList<LogEvent>(logEvents);
	}

	private static Logger getRootLogger() {
		return (Logger) LogUtil.getRootLogger();
	}

	public static void addToRootLogger(TestAppender appender) {
		Logger logger = getRootLogger();
		logger.addAppender(appender);
	}

	public static void removeAppender(TestAppender appender) {
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
}
