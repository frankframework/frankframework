/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.core;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;


/**
 * Basic implementation of <code>PipeLineSession</code>.
 *
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSession extends HashMap<String,Object> implements AutoCloseable {
	private static final Logger LOG = LogManager.getLogger(PipeLineSession.class);

	public static final String ORIGINAL_MESSAGE_KEY = "originalMessage";
	public static final String MESSAGE_ID_KEY       = "mid";        // externally determined (or generated) messageId, e.g. JmsMessageID, HTTP header configured as messageId
	public static final String CORRELATION_ID_KEY   = "cid";       // conversationId, e.g. JmsCorrelationID.
	public static final String STORAGE_ID_KEY       = "key";

	public static final String TS_RECEIVED_KEY = "tsReceived";
	public static final String TS_SENT_KEY = "tsSent";
	public static final String SECURITY_HANDLER_KEY ="securityHandler";

	public static final String HTTP_REQUEST_KEY    = "servletRequest";
	public static final String HTTP_RESPONSE_KEY   = "servletResponse";
	public static final String SERVLET_CONTEXT_KEY = "servletContext";

	public static final String API_PRINCIPAL_KEY   = "apiPrincipal";
	public static final String EXIT_STATE_CONTEXT_KEY="exitState";
	public static final String EXIT_CODE_CONTEXT_KEY="exitCode";

	private ISecurityHandler securityHandler = null;

	// closeables.keySet is a List of wrapped resources. The wrapper is used to unschedule them, once they are closed by a regular step in the process.
	// Values are labels to help debugging
	private final @Getter Map<AutoCloseable, String> closeables = new ConcurrentHashMap<>(); // needs to be concurrent, closes may happen from other threads
	public PipeLineSession() {
		super();
	}

	public PipeLineSession(int initialCapacity) {
		super(initialCapacity);
	}

	public PipeLineSession(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Create new PipeLineSession from existing map or session. This may not be null!
	 *
	 * @param t {@link Map} or PipeLineSession from which to copy session variables into the new session. Should not be null!
	 */
	public PipeLineSession(@Nonnull Map<String, Object> t) {
		super(t);
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
	 * @param keysToCopy Keys to be copied, separated by {@value ,} or {@value ;}.
	 *                   If {@code null} then all keys will be copied.
	 *                   If an empty string then no keys will be copied.
	 * @param from Child {@link PipeLineSession} from which keys are copied.
	 * @param to Parent {@link PipeLineSession} or {@link Map}.
	 * @param requester Tag of where the request to copy comes from so this can be logged when
	 *                  closing any messages.
	 */
	public static void mergeToParentSession(String keysToCopy, PipeLineSession from, Map<String,Object> to, INamedObject requester) {
		if (to == null) {
			return;
		}
		LOG.debug("returning context, returned session keys [{}]", keysToCopy);
		copyIfExists(EXIT_CODE_CONTEXT_KEY, from, to);
		copyIfExists(EXIT_STATE_CONTEXT_KEY, from, to);
		if (StringUtils.isNotEmpty(keysToCopy) && !"*".equals(keysToCopy)) {
			StringTokenizer st = new StringTokenizer(keysToCopy,",;");
			while (st.hasMoreTokens()) {
				String key = st.nextToken();
				copySessionKey(key, from, to, requester);
			}
		} else if (keysToCopy == null || "*".equals(keysToCopy)) { // if keys are not set explicitly ...
			for (String key : from.keySet()) { // ... all keys will be copied
				copySessionKey(key, from, to, requester);
			}
		}
		for (Entry<String, Object> sessionEntry : from.entrySet()) {
			if (sessionEntry.getValue() instanceof AutoCloseable &&
					to.containsKey(sessionEntry.getKey()) &&
					sessionEntry.getValue().equals(to.get(sessionEntry.getKey()))
			) {
				from.unscheduleCloseOnSessionExit((AutoCloseable) sessionEntry.getValue());
			}
		}
	}

	private static void copySessionKey(String key, PipeLineSession from, Map<String, Object> to, INamedObject requester) {
		Object value = from.get(key);
		to.put(key, value);
		if (value instanceof Message) {
			// Give messages the special treatment, because they do something extra before registering with session.
			Message message = (Message) value;
			message.unscheduleFromCloseOnExitOf(from);
			if (to instanceof PipeLineSession) {
				message.closeOnCloseOf((PipeLineSession) to, requester);
			}
		} else if (value instanceof AutoCloseable) {
			// Don't wrap closeables in a message, that makes unregistering them later unreliable
			AutoCloseable closeable = (AutoCloseable) value;
			from.unscheduleCloseOnSessionExit(closeable);
			if (to instanceof PipeLineSession) {
				((PipeLineSession) to).scheduleCloseOnSessionExit(closeable, ClassUtils.nameOf(requester));
			}
		}
	}

	private static void copyIfExists(String key, Map<String,Object> from, Map<String,Object> to) {
		if (from.containsKey(key)) {
			to.put(key, from.get(key));
		}
	}

	@Override
	public Object put(String key, Object value) {
		if (value instanceof AutoCloseable) {
			closeables.put((AutoCloseable) value, "Session key [" + key + "]");
		}
		return super.put(key, value);
	}

	/*
	 * The ladybug might stub the MessageId. The Stubbed value will be wrapped in a Message.
	 * Ensure that a proper string is returned in those cases too.
	 */
	@SneakyThrows
	@Nullable
	public String getMessageId() {
		return getString(MESSAGE_ID_KEY); // Allow Ladybug to wrap it in a Message
	}

	@SneakyThrows
	@Nullable
	public String getCorrelationId() {
		return getString(CORRELATION_ID_KEY); // Allow Ladybug to wrap it in a Message
	}

	@Nonnull
	public Message getMessage(String key) {
		Object obj = get(key);
		if(obj != null) {
			return Message.asMessage(obj);
		}
		return Message.nullMessage();
	}

	public Date getTsReceived() {
		return getTsReceived(this);
	}

	public static Date getTsReceived(Map<String, Object> context) {
		Object tsReceived = context.get(PipeLineSession.TS_RECEIVED_KEY);
		if(tsReceived instanceof Date) {
			return (Date) tsReceived;
		} else if(tsReceived instanceof String) {
			return DateUtils.parseToDate((String) tsReceived, DateUtils.FORMAT_FULL_GENERIC);
		}
		return null;
	}

	public Date getTsSent() {
		return getTsSent(this);
	}

	public static Date getTsSent(Map<String, Object> context) {
		Object tsSent = context.get(PipeLineSession.TS_SENT_KEY);
		if(tsSent instanceof Date) {
			return (Date) tsSent;
		} else if(tsSent instanceof String) {
			return DateUtils.parseToDate((String) tsSent, DateUtils.FORMAT_FULL_GENERIC);
		}
		return null;
	}

	/**
	 * Convenience method to set required parameters from listeners. Will not update messageId and
	 * correlationId when NULL. Will use current date-time for TS-Received if null.
	 */
	public static void updateListenerParameters(@Nonnull Map<String, Object> map, @Nullable String messageId, @Nullable String correlationId, @Nullable Date tsReceived, @Nullable Date tsSent) {
		if (messageId != null) {
			map.put(MESSAGE_ID_KEY, messageId);
		}
		if (correlationId != null) {
			map.put(CORRELATION_ID_KEY, correlationId);
		}
		if (tsReceived == null) {
			tsReceived = new Date();
		}
		map.put(TS_RECEIVED_KEY, DateUtils.format(tsReceived, DateUtils.FORMAT_FULL_GENERIC));
		if (tsSent != null) {
			map.put(TS_SENT_KEY, DateUtils.format(tsSent, DateUtils.FORMAT_FULL_GENERIC));
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

	public boolean isUserInRole(String role) throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.isUserInRole(role, this);
	}

	public Principal getPrincipal() throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.getPrincipal(this);
	}

	@Nullable
	@SneakyThrows
	public String getString(@Nonnull String key) {
		Object obj = get(key);
		if (obj == null) {
			return null;
		} else if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof Number) {
			return obj.toString();
		} else if (obj instanceof Message) {
			return ((Message) obj).asString();
		} else {
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

		if(ob instanceof Boolean) {
			return (Boolean) ob;
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

		if(ob instanceof Boolean) {
			return (Boolean) ob;
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

		if(ob instanceof Number) {
			return ((Number) ob).intValue();
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

		if(ob instanceof Integer) {
			return (Integer) ob;
		}
		if(ob instanceof Number) {
			return ((Number) ob).intValue();
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

		if(ob instanceof Number) {
			return ((Number) ob).longValue();
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

		if(ob instanceof Number) {
			return ((Number) ob).doubleValue();
		}
		return Double.parseDouble(Objects.requireNonNull(this.getString(key)));
	}

	public void scheduleCloseOnSessionExit(AutoCloseable resource, String requester) {
		closeables.put(resource, ClassUtils.nameOf(resource) +" of "+requester);
	}

	public boolean isScheduledForCloseOnExit(AutoCloseable message) {
		return closeables.containsKey(message);
	}

	public void unscheduleCloseOnSessionExit(AutoCloseable message) {
		closeables.remove(message);
	}

	@Override
	public void close() {
		LOG.debug("Closing PipeLineSession");
		while (!closeables.isEmpty()) {
			Iterator<Entry<AutoCloseable, String>> it = closeables.entrySet().iterator();
			Entry<AutoCloseable, String> entry = it.next();
			AutoCloseable closeable = entry.getKey();
			try {
				LOG.debug("messageId [{}] auto closing resource [{}]", this::getMessageId, entry::getValue);
				closeable.close();
			} catch (Exception e) {
				LOG.warn("Exception closing resource, messageId [" + getMessageId() + "], resource [" + entry.getKey() + "] " + entry.getValue(), e);
			} finally {
				closeables.remove(closeable);
			}
		}
	}
}
