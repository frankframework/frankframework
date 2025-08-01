/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.core;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.SneakyThrows;

import org.frankframework.stream.Message;
import org.frankframework.util.CleanerProvider;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.TimeProvider;

/**
 * Basic implementation of <code>PipeLineSession</code>.
 *
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSession extends HashMap<String,Object> implements AutoCloseable {
	private static final Logger LOG = LogManager.getLogger(PipeLineSession.class);

	public static final String SYSTEM_MANAGED_RESOURCE_PREFIX = "__";

	public static final String ORIGINAL_MESSAGE_KEY = "originalMessage";
	public static final String MESSAGE_ID_KEY       = "mid";        // externally determined (or generated) messageId, e.g. JmsMessageID, HTTP header configured as messageId
	public static final String CORRELATION_ID_KEY   = "cid";       // conversationId, e.g. JmsCorrelationID.
	public static final String STORAGE_ID_KEY       = "key";

	public static final String TS_RECEIVED_KEY = "tsReceived";
	public static final String TS_SENT_KEY = "tsSent";

	public static final String SECURITY_HANDLER_KEY = SYSTEM_MANAGED_RESOURCE_PREFIX + "securityHandler";
	public static final String MANUAL_RETRY_KEY     = SYSTEM_MANAGED_RESOURCE_PREFIX + "isManualRetry";
	public static final String HTTP_REQUEST_KEY    = SYSTEM_MANAGED_RESOURCE_PREFIX + "servletRequest"; // Used in one place to get the Request URI
	public static final String HTTP_RESPONSE_KEY   = SYSTEM_MANAGED_RESOURCE_PREFIX + "servletResponse"; // Used in one place to set a HTTP Cookie

	public static final String HTTP_METHOD_KEY 	   = "HttpMethod";

	public static final String API_PRINCIPAL_KEY   = "apiPrincipal";
	public static final String EXIT_STATE_CONTEXT_KEY="exitState";
	public static final String EXIT_CODE_CONTEXT_KEY="exitCode";

	private transient ISecurityHandler securityHandler = null;
	private transient Cleaner.Cleanable cleanable;

	// closeables.keySet is a List of wrapped resources. The wrapper is used to unschedule them, once they are closed by a regular step in the process.
	// Values are labels to help debugging
	private final transient @Getter Set<AutoCloseable> closeables = Collections.synchronizedSet(new HashSet<>()); // needs to be concurrent, closes may happen from other threads

	public PipeLineSession() {
		super();
		createCloseAction();
	}

	/**
	 * Create new PipeLineSession from existing map or session. This may not be null!
	 *
	 * @param t {@link Map} or PipeLineSession from which to copy session variables into the new session. Should not be null!
	 */
	public PipeLineSession(@Nonnull Map<String, Object> t) {
		super(t);
		createCloseAction();
	}

	private void createCloseAction() {
  PipeLineSessionCloseAction closeAction;
		closeAction = new PipeLineSessionCloseAction(this.closeables);
		cleanable = CleanerProvider.CLEANER.register(this, closeAction);
	}

	public void setExitState(PipeLine.ExitState state, Integer code) {
		put(EXIT_STATE_CONTEXT_KEY, state);
		if(code != null) {
			put(EXIT_CODE_CONTEXT_KEY, Integer.toString(code));
		}
	}

	public void setExitState(PipeLineResult pipeLineResult) {
		setExitState(pipeLineResult.getState(), pipeLineResult.getExitCode());
	}

	/**
	 * Copy specified keys from the {@code from} {@link PipeLineSession} to the parent
	 * {@link PipeLineSession} or {@link Map} {@code to}.
	 * Any keys present in both parent and child session will be unregistered from closing
	 * on the closing of the child session.
	 * <p>
	 *     The keys which will be copied are specified in parameter {@code keysToCopy}.
	 *     Keys names are separated by , or ; symbols.
	 *     If that parameter is {@code null} then all keys will be copied, if it is an
	 *     empty string then no keys will be copied.
	 * </p>
	 * @param keysToCopy Keys to be copied, separated by {@literal ,} or {@literal ;}.
	 *                   If {@code null} then all keys will be copied.
	 *                   If an empty string then no keys will be copied.
	 * @param parentSession Parent {@link PipeLineSession}.
	 */
	public void mergeToParentSession(String keysToCopy, PipeLineSession parentSession) {
		if (parentSession == null) {
			return;
		}
		LOG.debug("returning context, returned session keys [{}]", keysToCopy);
		copyIfExists(EXIT_CODE_CONTEXT_KEY, parentSession);
		copyIfExists(EXIT_STATE_CONTEXT_KEY, parentSession);
		if (StringUtils.isNotEmpty(keysToCopy) && !"*".equals(keysToCopy)) {
			StringTokenizer st = new StringTokenizer(keysToCopy,",;");
			while (st.hasMoreTokens()) {
				String key = st.nextToken();
				if (StringUtils.isNotBlank(key) && !MESSAGE_ID_KEY.equals(key)) {
					parentSession.put(key, get(key));
				}
			}
		} else if (keysToCopy == null || "*".equals(keysToCopy)) { // if keys are not set explicitly, all keys will be copied
			forEach((key, value) -> {
				if (!key.equals(MESSAGE_ID_KEY)) {
					parentSession.put(key, value);
				}
			});
		}
		Set<AutoCloseable> closeablesInDestination = parentSession.values().stream()
				.filter(AutoCloseable.class::isInstance)
				.map(AutoCloseable.class::cast)
				.collect(Collectors.toSet());
		closeablesInDestination.addAll(parentSession.closeables);
		closeables.removeAll(closeablesInDestination);
	}

	private void copyIfExists(String key, Map<String, Object> to) {
		if (containsKey(key)) {
			to.put(key, get(key));
		}
	}

	@Override
	public Object put(String key, Object value) {
		if (shouldCloseSessionResource(key, value)) {
			closeables.add((AutoCloseable) value);
		}
		return super.put(key, value);
	}

	private static boolean shouldCloseSessionResource(final String key, final Object value) {
		return value instanceof AutoCloseable autoCloseable &&
				isNotSystemManagedResource(key) &&
				isValueToBeClosed(autoCloseable);

	}

	/**
	 * Check that the AutoCloseable value is of type {@link Message}, and if it is, that it does not contain a scalar or array value.
	 * Scalar is defined as either {@link String}, {@link Number}, a {@link Date}, {@link Temporal} or {@link Boolean}.
	 *
	 * @param value AutoCloseable to check
	 * @return {@code true} if {@code value} is not a {@link Message}, or if it is a Message with a request that is not a scalar or array type. Returns {@code false} otherwise.
	 */
	private static boolean isValueToBeClosed(AutoCloseable value) {
		return (!(value instanceof Message)); // Don't close-on-close messages anymore
	}

	/**
	 * Check that key does not indicate the resource for this key should be managed by the system.
	 * @param key Key to check
	 * @return {@code true} if the key does not indicate this is not a system managed resource.
	 */
	private static boolean isNotSystemManagedResource(String key) {
		return !key.startsWith(SYSTEM_MANAGED_RESOURCE_PREFIX);
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		for (Entry<? extends String, ?> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/*
	 * The ladybug might stub the MessageId. The Stubbed value will be wrapped in a Message.
	 * Ensure that a proper string is returned in those cases too.
	 */
	@Nullable
	public String getMessageId() {
		return getString(MESSAGE_ID_KEY); // Allow Ladybug to wrap it in a Message
	}

	@Nullable
	public String getCorrelationId() {
		return getString(CORRELATION_ID_KEY); // Allow Ladybug to wrap it in a Message
	}

	/**
	 * Retrieves the value associated with the specified key and returns it as a {@link Message} object.
	 * If the key does not exist or the value is null, it returns a null message.
	 * <p>
	 *     <b>NB:</b> If the underlying value was a stream, reading the message will read the underlying
	 *     stream. The value can be preserved in the message, but the underlying stream can not be
	 *     preserved and reading the same session key again will effectively return an empty value.
	 * </p>
	 * @param key The key for which to retrieve the value.
	 * @return The value associated with the key encapsulated in a {@link Message} object.
	 *         If the key does not exist or the value is null, a null message is returned.
	 */
	@Nonnull
	public Message getMessage(String key) {
		Object obj = get(key);
		if (obj instanceof Message message) {
			return message;
		}
		if(obj != null) {
			return Message.asMessage(obj);
		}
		return Message.nullMessage();
	}

	public Instant getTsReceived() {
		return getTsReceived(this);
	}

	public static Instant getTsReceived(Map<String, Object> context) {
		Object tsReceived = context.get(PipeLineSession.TS_RECEIVED_KEY);
		if(tsReceived instanceof Instant instant) {
			return instant;
		} else if(tsReceived instanceof String string) {
			return DateFormatUtils.parseToInstant(string, DateFormatUtils.FULL_GENERIC_FORMATTER);
		}
		return null;
	}

	public Instant getTsSent() {
		return getTsSent(this);
	}

	public static Instant getTsSent(Map<String, Object> context) {
		Object tsSent = context.get(PipeLineSession.TS_SENT_KEY);
		if(tsSent instanceof Instant instant) {
			return instant;
		} else if(tsSent instanceof String string) {
			return DateFormatUtils.parseToInstant(string, DateFormatUtils.FULL_GENERIC_FORMATTER);
		}
		return null;
	}

	/**
	 * Convenience method to set required parameters from listeners. Will not update messageId and correlationId when NULL.
	 * Sets the current date-time as TS-Received.
	 */
	public static void updateListenerParameters(@Nonnull Map<String, Object> map, @Nullable String messageId, @Nullable String correlationId) {
		updateListenerParameters(map, messageId, correlationId, TimeProvider.now(), null);
	}

	/**
	 * Convenience method to set required parameters from listeners. Will not update messageId and
	 * correlationId when NULL. Will use current date-time for TS-Received if null.
	 */
	public static void updateListenerParameters(@Nonnull Map<String, Object> map, @Nullable String messageId, @Nullable String correlationId, @Nullable Instant tsReceived, @Nullable Instant tsSent) {
		if (messageId != null) {
			map.put(MESSAGE_ID_KEY, messageId);
		}
		if (correlationId != null) {
			map.put(CORRELATION_ID_KEY, correlationId);
		}
		if (tsReceived == null) {
			tsReceived = TimeProvider.now();
		}
		map.put(TS_RECEIVED_KEY, DateFormatUtils.format(tsReceived, DateFormatUtils.FULL_GENERIC_FORMATTER));
		if (tsSent != null) {
			map.put(TS_SENT_KEY, DateFormatUtils.format(tsSent, DateFormatUtils.FULL_GENERIC_FORMATTER));
		}
	}

	public void setSecurityHandler(ISecurityHandler handler) {
		securityHandler = handler;
		put(SECURITY_HANDLER_KEY, handler);
	}

	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		if (securityHandler == null) {
			securityHandler = (ISecurityHandler) get(SECURITY_HANDLER_KEY);
			if (securityHandler == null) {
				throw new NotImplementedException("no securityhandler found in PipeLineSession");
			}
		}
		return securityHandler;
	}

	/**
	 * Get value of a PipeLineSession key as String.
	 * <p>
	 *     <b>NB:</b> If the value was a stream, the stream is read and closed.
	 *     If the value was another kind of {@link AutoCloseable}, then a side effect of this method
	 *     may also be the value was closed.
	 * </p>
	 * @param key Session key to get.
	 * @return Value of the session key as String, or NULL of either the key was not present or had a NULL value.
	 */
	@Nullable
	@SneakyThrows(IOException.class)
	public String getString(@Nonnull String key) {
		Object obj = get(key);
		if (obj == null) {
			return null;
		} else if (obj instanceof String string) {
			return string;
		} else if (obj instanceof Number) {
			return obj.toString();
		} else if (obj instanceof Message message) {
			// Existing messages returned directly so they are not closed
			message.assertNotClosed();
			return message.asString();
		} else {
			// Other types are wrapped into a message, which is closed after converting to String.
			// NB: If the sessionKey value is a stream this consumes the stream.
			try (Message message = Message.asMessage(obj)) {
				return message.asString();
			}
		}
	}


	/**
	 * Retrieves a <code>String</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return String
	 */
	@Nullable
	public String get(@Nonnull String key, @Nullable String defaultValue) {
		String ob = this.getString(key);

		if (ob == null) return defaultValue;
		return ob;
	}

	/**
	 * Retrieves a <code>boolean</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return boolean
	 */
	public boolean get(String key, boolean defaultValue) {
		Object ob = this.get(key);
		if (ob == null) return defaultValue;

		if(ob instanceof Boolean boolean1) {
			return boolean1;
		}
		return "true".equalsIgnoreCase(this.getString(key));
	}

	/**
	 * Retrieves a <code>Boolean</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @return Boolean
	 */
	@Nullable
	public Boolean getBoolean(String key) {
		Object ob = this.get(key);
		if (ob == null) return null;

		if(ob instanceof Boolean boolean1) {
			return boolean1;
		}
		return "true".equalsIgnoreCase(this.getString(key));
	}

	/**
	 * Retrieves an <code>int</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return int
	 */
	public int get(String key, int defaultValue) {
		Object ob = this.get(key);
		if (ob == null) return defaultValue;

		if(ob instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(Objects.requireNonNull(this.getString(key)));
	}

	/**
	 * Retrieves an <code>Integer</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @return Integer
	 */
	@Nullable
	public Integer getInteger(String key) {
		Object ob = this.get(key);
		if (ob == null) return null;

		if(ob instanceof Integer integer) {
			return integer;
		}
		if(ob instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(Objects.requireNonNull(this.getString(key)));
	}

	/**
	 * Retrieves a <code>long</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return long
	 */
	public long get(String key, long defaultValue) {
		Object ob = this.get(key);
		if (ob == null) return defaultValue;

		if(ob instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(Objects.requireNonNull(this.getString(key)));
	}

	/**
	 * Retrieves a <code>double</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return double
	 */
	public double get(String key, double defaultValue) {
		Object ob = this.get(key);
		if (ob == null) return defaultValue;

		if(ob instanceof Number number) {
			return number.doubleValue();
		}
		return Double.parseDouble(Objects.requireNonNull(this.getString(key)));
	}

	public void scheduleCloseOnSessionExit(AutoCloseable resource) {
		if (isValueToBeClosed(resource)) {
			closeables.add(resource);
		}
	}

	public void unscheduleCloseOnSessionExit(AutoCloseable resource) {
		closeables.remove(resource);
	}

	@Override
	public void close() {
		LOG.debug("Closing PipeLineSession");
		cleanable.clean();
	}

	private static class PipeLineSessionCloseAction implements Runnable {
		private final Set<AutoCloseable> closeables;

		private PipeLineSessionCloseAction(Set<AutoCloseable> closeables) {
			this.closeables = closeables;
		}

		@Override
		public void run() {
			// Create a copy to safeguard against side-effects
			Set<AutoCloseable> closeableItems = new LinkedHashSet<>(closeables);
			closeables.clear();
			CloseUtils.closeSilently(closeableItems);
		}
	}
}
