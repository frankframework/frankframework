/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * ESB (Enterprise Service Bus) extension of JmsListener.
 *
 * <p><b>Configuration </b><i>(where deviating from JmsListener)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of ESB service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>if messageProtocol=<code>FF</code>: </td><td><code>false</code></td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>if messageProtocol=<code>RR</code>: </td><td><code>true</code></td></tr>
 * <tr><td>{@link #setCopyAEProperties(boolean) copyAEProperties}</td><td>if <code>true</code>, all JMS properties in the request starting with "ae_" are copied to the reply</td><td><code>false</code></td></tr>
 * </table></p>
 * 
 * @author  Peter Leeuwenburgh
 */
public class EsbJmsListener extends JmsListener implements ITransactionRequirements {
	private final static String REQUEST_REPLY = "RR";
	private final static String FIRE_AND_FORGET = "FF";
	private final static String CACHE_CONSUMER = "CACHE_CONSUMER";

	private String messageProtocol = null;
	private boolean copyAEProperties = false;
	
	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == null) {
			throw new ConfigurationException(getLogPrefix() + "messageProtocol must be set");
		}
		if (!getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY) && !getMessageProtocol().equalsIgnoreCase(FIRE_AND_FORGET)) {
			throw new ConfigurationException(getLogPrefix() + "illegal value for messageProtocol [" + getMessageProtocol() + "], must be '" + REQUEST_REPLY + "' or '" + FIRE_AND_FORGET + "'");
		}
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			setForceMessageIdAsCorrelationId(true);
			if (CACHE_CONSUMER.equals(getCacheMode())) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				configWarnings.add(log, "attribute [cacheMode] already has a default value [" + CACHE_CONSUMER + "]");
			}
			setCacheMode("CACHE_CONSUMER");
		} else {
			setUseReplyTo(false);
		}
		super.configure();
	}

	protected String retrieveIdFromMessage(Message message, Map threadContext) throws ListenerException {
		String id = super.retrieveIdFromMessage(message, threadContext);
		if (isCopyAEProperties()) {
			Enumeration propertyNames = null;
			try {
				propertyNames = message.getPropertyNames();
			} catch (JMSException e) {
				log.debug("ignoring JMSException in getPropertyName()", e);
			}
			while (propertyNames.hasMoreElements()) {
				String propertyName = (String) propertyNames.nextElement ();
				if (propertyName.startsWith("ae_")) {
					try {
						Object object = message.getObjectProperty(propertyName);
						threadContext.put(propertyName, object);
					} catch (JMSException e) {
						log.debug("ignoring JMSException in getObjectProperty()", e);
					}
				}
			}
		}
		return id;
	}

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessage, threadContext);
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			Destination replyTo = (Destination) threadContext.get("replyTo");
			if (replyTo == null) {
				log.warn("no replyTo address found for messageProtocol [" + getMessageProtocol() + "], response is lost");
			}
		}
	}

	public Map getMessagePropertiesToSet(Map threadContext) {
		if (isCopyAEProperties()) {
			Map properties = new HashMap();
			if (threadContext!=null) {
				for (Iterator it = threadContext.keySet().iterator(); it.hasNext();) {
					String key = (String)it.next();
					if (key.startsWith("ae_")) {
						Object value = threadContext.get(key);
						properties.put(key, value);
					}
				}
			}
			return properties;
		} else {
			return null;
		}
	}
	
	public void setMessageProtocol(String string) {
		messageProtocol = string;
	}

	public String getMessageProtocol() {
		return messageProtocol;
	}

	public boolean isSynchronous() {
		return getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY);
	}

	public boolean transactionalRequired() {
		if (getMessageProtocol().equals(FIRE_AND_FORGET)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean transactionalAllowed() {
		if (getMessageProtocol().equals(FIRE_AND_FORGET)) {
			return true;
		} else {
			return false;
		}
	}

	public void setCopyAEProperties(boolean b) {
		copyAEProperties = b;
	}

	public boolean isCopyAEProperties() {
		return copyAEProperties;
	}
}
