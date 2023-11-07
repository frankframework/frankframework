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
package nl.nn.adapterframework.logging;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StackLocatorUtil;

import nl.nn.adapterframework.util.StringUtil;

/**
 * This is a wrapper for Log4j2 layouts.
 * It enables us to: <br/>
 * - limit log message length<br/>
 * - apply global masking<br/>
 * - apply local (thread-wise) masking<br/>
 *
 * @author Murat Kaan Meral
 */
public abstract class IbisMaskingLayout extends AbstractStringLayout {

	/**
	 * Max length for the log message to be displayed. -1 for unlimited.
	 */
	private static int maxLength = -1;

	/**
	 * Explanation to display, if number of characters exceed the max length.
	 * %d is for the number of extra characters.
	 */
	private static String moreMessage = "...(%d more characters)";

	/**
	 * Set of regex strings to hide locally, meaning for specific threads/adapters.
	 * This is required to be set for each thread individually.
	 */
	private static ThreadLocal<Set<String>> threadLocalReplace = new ThreadLocal<>();

	/**
	 * Set of regex strings to hide globally, meaning for every thread/adapter.
	 */
	private static Set<String> globalReplace = new HashSet<>();

	/**
	 * @param config
	 * @param charset defaults to the system's default
	 */
	protected IbisMaskingLayout(Configuration config, Charset charset) {
		super(config, charset, null, null);

	}

	@Override
	public final String toSerializable(LogEvent logEvent) {
		Message msg = logEvent.getMessage();
		String message = msg.getFormattedMessage();

		if (StringUtils.isNotEmpty(message)) {
			message = StringUtil.hideAll(message, globalReplace);
			message = StringUtil.hideAll(message, threadLocalReplace.get());

			int length = message.length();
			if (maxLength > 0 && length > maxLength) {
				int diff = length - maxLength;
				//We trim the message because it may end with a newline or whitespace character.
				message = message.substring(0, maxLength).trim() + " " + String.format(moreMessage, diff) + "\r\n";
			}
		}

		Message logMessage = new LogMessage(message, msg.getThrowable());
		LogEvent event = updateLogEventMessage(logEvent, logMessage);

		return serializeEvent(event);
	}

	/**
	 * Wrapper around SimpleMessage so we can persist throwables, if any.
	 */
	private static class LogMessage extends SimpleMessage {
		private static final long serialVersionUID = 3907571033273707664L;

		private Throwable throwable;

		public LogMessage(String message, Throwable throwable) {
			super(message);
			this.throwable = throwable;
		}

		@Override
		public Throwable getThrowable() {
			return throwable;
		}
	}

	/**
	 * When converting from a (Log4jLogEvent) to a mutable LogEvent ensure to not invoke any getters but assign the fields directly.
	 *
	 * Directly calling RewriteAppender.append(LogEvent) can do 44 million ops/sec, but when calling rewriteLogger.debug(msg) to invoke
	 * a logger that calls this appender, all of a sudden throughput drops to 37 thousand ops/sec. That's 1000x slower.
	 *
	 * Rewriting the event ({@link MutableLogEvent#initFrom(LogEvent)}) includes invoking caller location information, {@link LogEvent#getSource()}
	 * This is done by taking a snapshot of the stack and walking it, see {@link StackLocatorUtil#calcLocation(String)}).
	 * Hence avoid this at all costs, fixed from version 2.6 (LOG4J2-1382) use a builder instance to update the @{link Message}.
	 * 
	 * @see "https://issues.apache.org/jira/browse/LOG4J2-1179"
	 * @see "https://issues.apache.org/jira/browse/LOG4J2-1382"
	 * @see StackLocatorUtil#calcLocation(String)
	 */
	private LogEvent updateLogEventMessage(LogEvent event, Message message) {
		if(event instanceof Log4jLogEvent) {
			Log4jLogEvent.Builder builder = ((Log4jLogEvent) event).asBuilder();
			builder.setMessage(message);
			return builder.build();
		}

		//NB: this might trigger a source location lookup.
		MutableLogEvent mutable = new MutableLogEvent();
		mutable.initFrom(event);
		mutable.setMessage(message);
		return mutable;
	}

	/**
	 * Mutable LogEvent which masks messages using global and local regex strings,
	 * and shortens the message to a maximum length, if necessary.
	 *
	 * @param event Event to be serialized to a String.
	 * @return Serialized and masked event.
	 */
	protected abstract String serializeEvent(LogEvent event);

	public static void setMaxLength(int maxLength) {
		IbisMaskingLayout.maxLength = maxLength;
	}

	public static int getMaxLength() {
		return maxLength;
	}

	public static String getMoreMessageString() {
		return moreMessage;
	}

	public static void setMoreMessageString(String moreMessage) {
		IbisMaskingLayout.moreMessage = moreMessage;
	}

	public static void addToGlobalReplace(String regex) {
		globalReplace.add(regex);
	}

	public static void removeFromGlobalReplace(String regex) {
		globalReplace.remove(regex);
	}

	public static Set<String> getGlobalReplace() {
		return globalReplace;
	}

	public static void cleanGlobalReplace() {
		globalReplace = new HashSet<>();
	}

	public static void addToThreadLocalReplace(Collection<String> collection) {
		if(collection == null) return;

		if (threadLocalReplace.get() == null)
			createThreadLocalReplace();

		threadLocalReplace.get().addAll(collection);
	}

	/**
	 * Add regex to hide locally, meaning for specific threads/adapters.
	 * This used to be LogUtil.setThreadHideRegex(String hideRegex)
	 */
	public static void addToThreadLocalReplace(String regex) {
		if(StringUtils.isEmpty(regex)) return;

		if (threadLocalReplace.get() == null)
			createThreadLocalReplace();
		threadLocalReplace.get().add(regex);
	}

	/**
	 * Remove regex to hide locally, meaning for specific threads/adapters.
	 * When the last item is removed the Set will be removed as well.
	 */
	public static void removeFromThreadLocalReplace(String regex) {
		if(StringUtils.isEmpty(regex)) return;

		threadLocalReplace.get().remove(regex);

		if(threadLocalReplace.get().isEmpty())
			removeThreadLocalReplace();
	}

	/**
	 * Set of regex strings to hide locally, meaning for specific threads/adapters.
	 * Can return null when not used/initalized!
	 */
	public static Set<String> getThreadLocalReplace() {
		return threadLocalReplace.get();
	}

	private static void createThreadLocalReplace() {
		threadLocalReplace.set(new HashSet<>());
	}

	public static void removeThreadLocalReplace() {
		threadLocalReplace.remove();
	}
}
