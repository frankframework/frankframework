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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;


/**
 * Basic implementation of <code>IPipeLineSession</code>.
 * 
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSessionBase extends HashMap<String,Object> implements IPipeLineSession {
	private Logger log = LogUtil.getLogger(this);

	private ISecurityHandler securityHandler = null;
	
	// Map that maps resources to wrapped versions of them. The wrapper is used to unschedule them, once they are closed by a regular step in the process.
	private Map<AutoCloseable,AutoCloseable> closeables = new HashMap<>(); 
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
		return (String) Message.asObject(get(messageIdKey));
	}

	@Override
	public Message getOriginalMessage() {
		return Message.asMessage(get(originalMessageKey));
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
	public InputStream scheduleCloseOnSessionExit(InputStream stream) {
		return scheduleCloseOnSessionExit(stream, StreamUtil::onClose );
	}
		
	@Override
	public OutputStream scheduleCloseOnSessionExit(OutputStream stream) {
		return scheduleCloseOnSessionExit(stream, StreamUtil::onClose );
	}

	@Override
	public Reader scheduleCloseOnSessionExit(Reader reader) {
		return scheduleCloseOnSessionExit(reader, StreamUtil::onClose );
	}
		
	@Override
	public Writer scheduleCloseOnSessionExit(Writer writer) {
		return scheduleCloseOnSessionExit(writer, StreamUtil::onClose );
	}

	/**
	 * Ensure that the resource will be closed when the PipeLineSession is closed.
	 * @param resource 	the resource to be closed on exit
	 * @param onClose	a method that will make sure that a task is executed when the resource is closed. 
	 * 					This is used to unschedule the resource when it is closed.
	 */
	@SuppressWarnings("unchecked")
	private <R extends AutoCloseable> R scheduleCloseOnSessionExit(R resource, BiFunction<R, Runnable, R> onClose) {
		if (closeables.values().contains(resource)) {
			return resource; // resource is already a wrapped resource scheduled for closing at pipeline exit
		}
		return (R)closeables.computeIfAbsent(resource, k -> {
				if (log.isDebugEnabled()) log.debug("registering resource ["+resource+"] for close on exit");
				return onClose.apply(resource, () -> {
					if (log.isDebugEnabled()) log.debug("closed and unregistering resource ["+resource+"] from close on exit");
					closeables.remove(resource);
				});
			});
	}


	@Override
	public void unscheduleCloseOnSessionExit(AutoCloseable resource) {
		Optional<Entry<AutoCloseable, AutoCloseable>> entry = closeables.entrySet().stream().filter(e -> resource == e.getValue()).findFirst();
		if (entry.isPresent()) {
			closeables.remove(entry.get().getKey());
		}
	}
	
	@Override
	public void close() {
		log.debug("Closing PipeLineSession");
		while (!closeables.isEmpty()) {
			try {
				Iterator<Entry<AutoCloseable, AutoCloseable>>  it = closeables.entrySet().iterator();
				Entry<AutoCloseable, AutoCloseable> entry = it.next();
				log.warn("messageId ["+getMessageId()+"] auto closing resource ["+entry.getKey()+"]");
				entry.getValue().close();
			} catch (Exception e) {
				log.warn("Exception closing resource", e);
			}
		}
	}
}
