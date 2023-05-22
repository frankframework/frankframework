/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;

import lombok.SneakyThrows;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;


/**
 * Basic implementation of <code>PipeLineSession</code>.
 *
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSession extends HashMap<String,Object> implements AutoCloseable {
	private Logger log = LogUtil.getLogger(this);

	public static final String originalMessageKey="originalMessage";
	public static final String originalMessageIdKey="id";
	public static final String messageIdKey="messageId";
	public static final String businessCorrelationIdKey="cid";
	public static final String technicalCorrelationIdKey="tcid";

	public static final String TS_RECEIVED_KEY = "tsReceived";
	public static final String TS_SENT_KEY = "tsSent";
	public static final String securityHandlerKey="securityHandler";

	public static final String HTTP_REQUEST_KEY    = "servletRequest";
	public static final String HTTP_RESPONSE_KEY   = "servletResponse";
	public static final String SERVLET_CONTEXT_KEY = "servletContext";

	public static final String API_PRINCIPAL_KEY   = "apiPrincipal";
	public static final String EXIT_STATE_CONTEXT_KEY="exitState";
	public static final String EXIT_CODE_CONTEXT_KEY="exitCode";

	private ISecurityHandler securityHandler = null;

	// closeables.keySet is a List of wrapped resources. The wrapper is used to unschedule them, once they are closed by a regular step in the process.
	// Values are labels to help debugging
	private Map<AutoCloseable, String> closeables = new ConcurrentHashMap<>(); // needs to be concurrent, closes may happen from other threads
	public PipeLineSession() {
		super();
	}

	public PipeLineSession(int initialCapacity) {
		super(initialCapacity);
	}

	public PipeLineSession(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public PipeLineSession(Map<String, Object> t) {
		super(t);
	}

	//Shouldn't this be `id` ? See {#setListenerParameters(...)};
	@SneakyThrows
	public String getMessageId() {
		return Message.asString(get(messageIdKey)); // Allow Ladybug to wrap it in a Message
	}

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
	 * Convenience method to set required parameters from listeners
	 */
	public static void setListenerParameters(Map<String, Object> map, String messageId, String technicalCorrelationId, Date tsReceived, Date tsSent) {
		if (messageId!=null) {
			map.put(originalMessageIdKey, messageId);
		}
		if (technicalCorrelationId!=null) {
			map.put(technicalCorrelationIdKey, technicalCorrelationId);
		}
		if (tsReceived==null) {
			tsReceived=new Date();
		}
		map.put(TS_RECEIVED_KEY, DateUtils.format(tsReceived, DateUtils.FORMAT_FULL_GENERIC));
		if (tsSent!=null) {
			map.put(TS_SENT_KEY, DateUtils.format(tsSent, DateUtils.FORMAT_FULL_GENERIC));
		}
	}

	public void setSecurityHandler(ISecurityHandler handler) {
		securityHandler = handler;
		put(securityHandlerKey, handler);
	}

	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		if (securityHandler==null) {
			securityHandler=(ISecurityHandler)get(securityHandlerKey);
			if (securityHandler==null) {
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

	private String getString(String key) {
		try {
			return (String) get(key);
		} catch(Exception e) {
			return get(key).toString();
		}
	}


	/**
	 * Retrieves a <code>String</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @param defaultValue the value to return when the key cannot be found
	 * @return String
	 */
	public String get(String key, String defaultValue) {
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
		return this.getString(key).equalsIgnoreCase("true");
	}

	/**
	 * Retrieves a <code>Boolean</code> value from the PipeLineSession
	 * @param key the referenced key
	 * @return Boolean
	 */
	public Boolean getBoolean(String key) {
		Object ob = this.get(key);
		if (ob == null) return null;

		if(ob instanceof Boolean) {
			return (Boolean) ob;
		}
		return this.getString(key).equalsIgnoreCase("true");
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

		if(ob instanceof Integer) {
			return (Integer) ob;
		}
		return Integer.parseInt(this.getString(key));
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

		if(ob instanceof Long) {
			return (Long) ob;
		}
		return Long.parseLong(this.getString(key));
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

		if(ob instanceof Double) {
			return (Double) ob;
		}
		return Double.parseDouble(this.getString(key));
	}

	public void scheduleCloseOnSessionExit(AutoCloseable resource, String requester) {
		closeables.put(resource, ClassUtils.nameOf(resource) +" of "+requester);
	}

	public boolean isScheduledForCloseOnExit(Message message) {
		return closeables.containsKey(message);
	}

	public void unscheduleCloseOnSessionExit(Message message) {
		closeables.remove(message);
	}

	@Override
	public void close() {
		log.debug("Closing PipeLineSession");
		while (!closeables.isEmpty()) {
			Iterator<Entry<AutoCloseable, String>> it = closeables.entrySet().iterator();
			Entry<AutoCloseable, String> entry = it.next();
			AutoCloseable closeable = entry.getKey();
			try {
				log.warn("messageId ["+getMessageId()+"] auto closing resource "+entry.getValue());
				closeable.close();
			} catch (Exception e) {
				log.warn("Exception closing resource", e);
			} finally {
				closeables.remove(closeable);
			}
		}
	}
}
