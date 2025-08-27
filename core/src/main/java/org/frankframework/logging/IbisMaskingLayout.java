/*
   Copyright 2020-2024 WeAreFrank!

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

import java.io.Serial;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StackLocatorUtil;

import org.frankframework.threading.ThreadConnector;
import org.frankframework.util.StringUtil;

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
	private static final ThreadLocal<Deque<Pattern>> threadLocalReplace = new ThreadLocal<>();

	/**
	 * Set of regex strings to hide globally, meaning for every thread/adapter.
	 */
	private static Set<Pattern> globalReplace = new HashSet<>();

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
			message = maskSensitiveInfo(message);

			int length = message.length();
			if (maxLength > 0 && length > maxLength) {
				int diff = length - maxLength;
				//We trim the message because it may end with a newline or whitespace character.
				message = message.substring(0, maxLength).trim() + " " + moreMessage.formatted(diff) + "\r\n";
			}
		}

		Message logMessage = new LogMessage(message, msg.getThrowable());
		LogEvent event = updateLogEventMessage(logEvent, logMessage);

		return serializeEvent(event);
	}

	public static String maskSensitiveInfo(String message) {
		if (StringUtils.isBlank(message)) {
			return message;
		}
		String tmpResult = StringUtil.hideAll(message, globalReplace);
		return StringUtil.hideAll(tmpResult, threadLocalReplace.get());
	}

	/**
	 * Wrapper around SimpleMessage so we can persist throwables, if any.
	 */
	private static class LogMessage extends SimpleMessage {
		@Serial
		private static final long serialVersionUID = 3907571033273707664L;

		private final Throwable throwable;

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
	 * <br/>
	 * Directly calling RewriteAppender.append(LogEvent) can do 44 million ops/sec, but when calling rewriteLogger.debug(msg) to invoke
	 * a logger that calls this appender, all of a sudden throughput drops to 37 thousand ops/sec. That's 1000x slower.
	 * <br/>
	 * Rewriting the event ({@link MutableLogEvent#initFrom(LogEvent)}) includes invoking caller location information, {@link LogEvent#getSource()}
	 * This is done by taking a snapshot of the stack and walking it, see {@link StackLocatorUtil#calcLocation(String)}).
	 * Hence avoid this at all costs, fixed from version 2.6 (LOG4J2-1382) use a builder instance to update the {@link Message}.
	 *
	 * @see "https://issues.apache.org/jira/browse/LOG4J2-1179"
	 * @see "https://issues.apache.org/jira/browse/LOG4J2-1382"
	 * @see StackLocatorUtil#calcLocation(String)
	 */
	private LogEvent updateLogEventMessage(LogEvent event, Message message) {
		if(event instanceof Log4jLogEvent logEvent) {
			Log4jLogEvent.Builder builder = logEvent.asBuilder();
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
		globalReplace.add(Pattern.compile(regex));
	}

	public static void addToGlobalReplace(Pattern regex) {
		globalReplace.add(regex);
	}

	public static Set<Pattern> getGlobalReplace() {
		return globalReplace;
	}

	public static void clearGlobalReplace() {
		globalReplace = new HashSet<>();
	}

	/**
	 * Replace all thread-local hideRegexes. Always clears the current stack, even if the
	 * new collection is null or empty.
	 * This method should be used to initialize the stack of hideregexes at the start of a new thread
	 * when there might be hideregexes to be carried over from a calling thread.
	 *
	 * @see ThreadConnector
	 *
	 * @param hideRegexCollection Collection of new hideRegexes. Can be null or empty.
	 */
	public static void setThreadLocalReplace(@Nullable Collection<Pattern> hideRegexCollection) {
		clearThreadLocalReplace();
		if (hideRegexCollection == null || hideRegexCollection.isEmpty()) return;

		Deque<Pattern> stack = getOrCreateThreadLocalReplace();
		stack.addAll(hideRegexCollection);
	}

	/**
	 * Collection of regex strings to hide locally, meaning for specific threads/adapters.
	 * Can return null when not used/initialized!
	 */
	@Nullable
	public static Collection<Pattern> getThreadLocalReplace() {
		Deque<Pattern> stack = threadLocalReplace.get();
		if (stack == null) return null;
		return Collections.unmodifiableCollection(stack);
	}

	@Nonnull
	private static Deque<Pattern> getOrCreateThreadLocalReplace() {
		Deque<Pattern> stack = threadLocalReplace.get();
		if (stack == null) {
			return createThreadLocalReplace();
		}
		return stack;
	}

	@Nonnull
	private static Deque<Pattern> createThreadLocalReplace() {
		Deque<Pattern> stack = new ArrayDeque<>();
		threadLocalReplace.set(stack);
		return stack;
	}

	/**
	 * Clear all thread-local hide-regexes.
	 */
	public static void 	clearThreadLocalReplace() {
		threadLocalReplace.remove();
	}

	/**
	 * Push a hide-regex pattern to the ThreadLocal replace-hideregex stack and
	 * return a {@link HideRegexContext} that can be closed to pop the pattern from the
	 * stack again.
	 * This is meant to be used in try-with-resources construct.
	 *
	 * @param pattern Pattern used to find strings in loglines that need to be hidden.
	 * @return {@link HideRegexContext} that can be closed to remove above pattern from the stack again.
	 */
	@Nonnull
	public static HideRegexContext pushToThreadLocalReplace(@Nullable Pattern pattern) {
		if (pattern == null) {
			return () -> {
				// No-op
			};
		}
		Deque<Pattern> stack = getOrCreateThreadLocalReplace();
		stack.push(pattern);
		return stack::pop;
	}

	/**
	 * Interface overrides {@link AutoCloseable#close()} to remove the exception so this
	 * can be used in a try-with-resources without having to handle any exceptions, however
	 * does not need to add any extra methods.
	 * <br/>
	 * Is used in the return value of {@link IbisMaskingLayout#pushToThreadLocalReplace(Pattern)}, instances
	 * are lambdas or method references.
	 */
	public interface HideRegexContext extends AutoCloseable {
		@Override
		void close();
	}
}
