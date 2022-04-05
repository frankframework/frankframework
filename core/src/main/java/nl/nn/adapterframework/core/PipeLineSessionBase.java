/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;


/**
 * Basic implementation of <code>IPipeLineSession</code>.
 * 
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSessionBase extends HashMap<String,Object> implements IPipeLineSession, AutoCloseable {
	private Logger log = LogUtil.getLogger(this);

	private ISecurityHandler securityHandler = null;
	
	// Map that maps resources to wrapped versions of them. The wrapper is used to unschedule them, once they are closed by a regular step in the process.
	private Set<Message> closeables = ConcurrentHashMap.newKeySet(); // needs to be concurrent, closes may happen from other threads
	public PipeLineSessionBase() {
		super();
	}

	public PipeLineSessionBase(int initialCapacity) {
		super(initialCapacity);
	}

	public PipeLineSessionBase(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public PipeLineSessionBase(Map<String, Object> t) {
		super(t);
	}

	//Shouldn't this be `id` ? See {#setListenerParameters(...)};
	@Override
	public String getMessageId() {
		return (String) Message.asObject(get(messageIdKey)); // Allow Ladybug to wrap it in a Message
	}

	public Message getMessage(String key) {
		Object obj = get(key);
		if(obj != null) {
			return Message.asMessage(obj);
		}
		return Message.nullMessage();
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
		map.put(tsReceivedKey,DateUtils.format(tsReceived, DateUtils.FORMAT_FULL_GENERIC));
		if (tsSent!=null) {
			map.put(tsSentKey,DateUtils.format(tsSent, DateUtils.FORMAT_FULL_GENERIC));
		}
	}

	@Override
	public void setSecurityHandler(ISecurityHandler handler) {
		securityHandler = handler;
		put(securityHandlerKey, handler);
	}

	@Override
	public ISecurityHandler getSecurityHandler() throws NotImplementedException {
		if (securityHandler==null) {
			securityHandler=(ISecurityHandler)get(securityHandlerKey);
			if (securityHandler==null) {
				throw new NotImplementedException("no securityhandler found in PipeLineSession");
			}
		}
		return securityHandler;
	}

	@Override
	public boolean isUserInRole(String role) throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.isUserInRole(role, this);
	}

	@Override
	public Principal getPrincipal() throws NotImplementedException {
		ISecurityHandler handler = getSecurityHandler();
		return handler.getPrincipal(this);
	}

	private String getString(String key) {
		try {
			return (String) get(key);
		}
		catch(Exception e) {
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

		if(ob instanceof Boolean)
			return (Boolean) ob;
		else
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

		if(ob instanceof Integer)
			return (Integer) ob;
		else
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

		if(ob instanceof Long)
			return (Long) ob;
		else
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

		if(ob instanceof Double)
			return (Double) ob;
		else
			return Double.parseDouble(this.getString(key));
	}
	
	@Override
	public void scheduleCloseOnSessionExit(Message message) {
		closeables.add(message);
	}

	@Override
	public void scheduleCloseOnSessionExit(AutoCloseable resource) {
		// create a dummy Message, to be able to schedule the resource for close on exit of session
		Message resourceMessage = new Message(new StringReader("dummy")) {
			@Override
			public void close() throws Exception {
				resource.close();
			}
		};
		scheduleCloseOnSessionExit(resourceMessage);
	}

	@Override
	public boolean isScheduledForCloseOnExit(Message message) {
		return closeables.contains(message);
	}

	@Override
	public void unscheduleCloseOnSessionExit(Message message) {
		closeables.remove(message);
	}
	
	@Override
	public void close() {
		log.debug("Closing PipeLineSession");
		while (!closeables.isEmpty()) {
			try {
				Iterator<Message> it = closeables.iterator();
				Message entry = it.next();
				log.warn("messageId ["+getMessageId()+"] auto closing resource ["+entry+"]");
				entry.close();
				closeables.remove(entry);
			} catch (Exception e) {
				log.warn("Exception closing resource", e);
			}
		}
	}
}
