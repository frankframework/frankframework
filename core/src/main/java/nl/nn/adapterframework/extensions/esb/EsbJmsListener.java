/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.IListenerConnector.CacheMode;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.Default;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * ESB (Enterprise Service Bus) extension of JmsListener.
 *
 * @author  Peter Leeuwenburgh
 */
@Category("NN-Special")
public class EsbJmsListener extends JmsListener implements ITransactionRequirements {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private final String MSGLOG_KEYS = APP_CONSTANTS.getResolvedProperty("msg.log.keys");

	private @Getter MessageProtocol messageProtocol = null;
	private @Getter boolean copyAEProperties = false;
	private @Getter String xPathLoggingKeys=null;

	private final Map<String, String> xPathLogMap = new HashMap<String, String>();

	public enum MessageProtocol {
		/** Fire & Forget protocol */
		FF,
		/** Request-Reply protocol */
		RR;
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == MessageProtocol.RR) {
			setForceMessageIdAsCorrelationId(true);
			if (getCacheMode()==CacheMode.CACHE_CONSUMER) {
				ConfigurationWarnings.add(this, log, "attribute [cacheMode] already has a default value [" + CacheMode.CACHE_CONSUMER + "]", SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY, getReceiver().getAdapter());
			}
			setCacheMode(CacheMode.CACHE_CONSUMER);
		} else {
			setUseReplyTo(false);
		}
		super.configure();
		configurexPathLogging();
	}

	protected Map<String, String> getxPathLogMap() {
		return xPathLogMap;
	}

	private void configurexPathLogging() {
		String logKeys = MSGLOG_KEYS;
		if(getXPathLoggingKeys() != null) //Override on listener level
			logKeys = getXPathLoggingKeys();

		StringTokenizer tokenizer = new StringTokenizer(logKeys, ",");
		while (tokenizer.hasMoreTokens()) {
			String name = tokenizer.nextToken();
			String xPath = APP_CONSTANTS.getResolvedProperty("msg.log.xPath." + name);
			if(xPath != null)
				xPathLogMap.put(name, xPath);
		}
	}

	@Override
	protected String retrieveIdFromMessage(Message message, Map<String, Object> threadContext) throws ListenerException {
		String id = super.retrieveIdFromMessage(message, threadContext);
		if (isCopyAEProperties()) {
			Enumeration<?> propertyNames = null;
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

		try {
			TextMessage textMessage = (TextMessage) message;
			String soapMessage = textMessage.getText();

			if(getxPathLogMap().size() > 0) {
				String xPathLogKeys = "";
				Iterator<Entry<String, String>> it = getxPathLogMap().entrySet().iterator();
				while(it.hasNext()) {
					Map.Entry<String, String> pair = it.next();
					String sessionKey = pair.getKey();
					String xPath = pair.getValue();
					String result = getResultFromxPath(soapMessage, xPath);
					if(result.length() > 0) {
						threadContext.put(sessionKey, result);
						xPathLogKeys = xPathLogKeys + "," + sessionKey; // Only pass items that have been found, otherwise logs will clutter with NULL.
					}
				}
				threadContext.put("xPathLogKeys", xPathLogKeys);
			}
		} catch (JMSException e) {
			log.debug("ignoring JMSException", e);
		}
		return id;
	}

	protected String getResultFromxPath(String message, String xPathExpression) {
		String found = "";
		if(message != null && message.length() > 0) {
			if(XmlUtils.isWellFormed(message)) {
				try {
					TransformerPool test = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource("", xPathExpression, OutputType.TEXT, false));
					found = test.transform(message, null);

					//xPath not found and message length is 0 but not null nor ""
					if(found.length() == 0) found = "";
				} catch (Exception e) {
					log.debug("could not evaluate xpath expression",e);
				}
			}
		}
		return found;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessage, Map<String, Object> threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessage, threadContext);
		if (getMessageProtocol() == MessageProtocol.RR) {
			Destination replyTo = (Destination) threadContext.get("replyTo");
			if (replyTo == null) {
				log.warn("no replyTo address found for messageProtocol [" + getMessageProtocol() + "], response is lost");
			}
		}
	}

	@Override
	protected Map<String, Object> getMessageProperties(Map<String, Object> threadContext) {
		Map<String, Object> properties = super.getMessageProperties(threadContext);

		if (isCopyAEProperties()) {
			if(properties == null)
				properties = new HashMap<String, Object>();

			if (threadContext != null) {
				for (Iterator<String> it = threadContext.keySet().iterator(); it.hasNext();) {
					String key = it.next();
					if (key.startsWith("ae_")) {
						Object value = threadContext.get(key);
						properties.put(key, value);
					}
				}
			}
		}

		return properties;
	}

	/** protocol of ESB service to be called */
	@Mandatory
	public void setMessageProtocol(MessageProtocol string) {
		messageProtocol = string;
	}

	public boolean isSynchronous() {
		return getMessageProtocol() == MessageProtocol.RR;
	}

	@Override
	public boolean transactionalRequired() {
		return getMessageProtocol() == MessageProtocol.FF;
	}

	@Override
	public boolean transactionalAllowed() {
		return getMessageProtocol() == MessageProtocol.FF;
	}

	/**
	 * if true, all JMS properties in the request starting with "ae_" are copied to the reply.
	 * @ff.default false
	 */
	public void setCopyAEProperties(boolean b) {
		copyAEProperties = b;
	}

	@Override
	@Default("if messageProtocol=<code>RR</code>: </td><td><code>true</code>")
	public void setForceMessageIdAsCorrelationId(boolean force) {
		super.setForceMessageIdAsCorrelationId(force);
	}

	@Override
	@Default("if messageProtocol=<code>FF</code>: <code>false</code>")
	public void setUseReplyTo(boolean newUseReplyTo) {
		super.setUseReplyTo(newUseReplyTo);
	}

	/** Comma separated list of all XPath keys that need to be logged. (overrides <code>msg.log.keys</code> property) */
	public void setxPathLoggingKeys(String string) {
		xPathLoggingKeys = string;
	}
}
