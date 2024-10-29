/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2022-2024 WeAreFrank!

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
package org.frankframework.extensions.esb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerConfigurationException;

import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IListenerConnector.CacheMode;
import org.frankframework.core.ITransactionRequirements;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Category;
import org.frankframework.doc.Default;
import org.frankframework.doc.Mandatory;
import org.frankframework.jms.BytesMessageInputStream;
import org.frankframework.jms.JmsListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;

/**
 * ESB (Enterprise Service Bus) extension of JmsListener.
 *
 * @author  Peter Leeuwenburgh
 */
@Category(Category.Type.NN_SPECIAL)
public class EsbJmsListener extends JmsListener implements ITransactionRequirements {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final String MSGLOG_KEYS = APP_CONSTANTS.getProperty("msg.log.keys");
	private static final Map<String, TransformerPool> LOG_KEY_TRANSFORMER_POOLS = new ConcurrentHashMap<>();
	static final String JMS_RR_FORCE_MESSAGE_KEY = "jms.esb.rr.forceMessageIdAsCorrelationId.default";
	private final String messageIdAsCorrelationIdRR = APP_CONSTANTS.getString(JMS_RR_FORCE_MESSAGE_KEY, null);

	private @Getter MessageProtocol messageProtocol = null;
	private @Getter boolean copyAEProperties = false;
	private @Getter String xPathLoggingKeys=null;

	private final Map<String, String> xPathLogMap = new HashMap<>();

	public enum MessageProtocol {
		/** Fire & Forget protocol */
		FF,
		/** Request-Reply protocol */
		RR
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == MessageProtocol.RR) {
			if (getForceMessageIdAsCorrelationId() == null) {
				if (StringUtils.isNotBlank(messageIdAsCorrelationIdRR)) {
					setForceMessageIdAsCorrelationId(Boolean.parseBoolean(messageIdAsCorrelationIdRR));
				} else {
					setForceMessageIdAsCorrelationId(true);
				}
			}
			if (getCacheMode()==CacheMode.CACHE_CONSUMER) {
				ConfigurationWarnings.add(this, log, "attribute [cacheMode] already has a default value [" + CacheMode.CACHE_CONSUMER + "]", SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY, getReceiver().getAdapter());
			}
			setCacheMode(CacheMode.CACHE_CONSUMER);
		} else {
			setUseReplyTo(false);
		}
		super.configure();
		try {
			configureXPathLogging();
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(e);
		}
	}

	protected Map<String, String> getxPathLogMap() {
		return xPathLogMap;
	}

	private void configureXPathLogging() throws TransformerConfigurationException {
		String logKeys;
		if(getXPathLoggingKeys() != null) //Override on listener level
			logKeys = getXPathLoggingKeys();
		else
			logKeys = MSGLOG_KEYS;

		if (logKeys == null) {
			return;
		}
		for (String name : StringUtil.split(logKeys)) {
			String xPath = APP_CONSTANTS.getProperty("msg.log.xPath." + name);
			if(xPath != null) {
				xPathLogMap.put(name, xPath);
				if (!LOG_KEY_TRANSFORMER_POOLS.containsKey(xPath)) {
					LOG_KEY_TRANSFORMER_POOLS.put(xPath, TransformerPool.getUtilityInstance(
						XmlUtils.createXPathEvaluatorSource(xPath), XmlUtils.DEFAULT_XSLT_VERSION
					));
				}
			}
		}
	}

	@Override
	public Map<String, Object> extractMessageProperties(Message rawMessage) {
		Map<String, Object> messageProperties = super.extractMessageProperties(rawMessage);
		if (isCopyAEProperties()) {
			Enumeration<?> propertyNames = null;
			try {
				propertyNames = rawMessage.getPropertyNames();
			} catch (JMSException e) {
				log.debug("ignoring JMSException in getPropertyName()", e);
			}
			while (propertyNames != null && propertyNames.hasMoreElements()) {
				String propertyName = (String) propertyNames.nextElement ();
				if (propertyName.startsWith("ae_")) {
					try {
						Object object = rawMessage.getObjectProperty(propertyName);
						messageProperties.put(propertyName, object);
					} catch (JMSException e) {
						log.debug("ignoring JMSException in getObjectProperty()", e);
					}
				}
			}
		}

		if (!getxPathLogMap().isEmpty()) {
			extractXpathLogProperties(rawMessage, messageProperties);
		}
		return messageProperties;
	}

	private void extractXpathLogProperties(Message rawMessage, Map<String, Object> messageProperties) {
		try {
			String soapMessage;
			if (rawMessage instanceof TextMessage textMessage) {
				soapMessage = textMessage.getText();
			} else if (rawMessage instanceof BytesMessage bytesMessage) {
				InputStream input = new BytesMessageInputStream(bytesMessage);
				soapMessage = StreamUtil.streamToString(input);
				bytesMessage.reset();
			} else {
				log.debug("Can only extract data from TextMessage or BytesMessage, not from [{}]", rawMessage.getClass().getName());
				soapMessage = null;
			}
			if (soapMessage == null) {
				return;
			}

			StringBuilder xPathLogKeys = new StringBuilder();
			for (Entry<String, String> pair : getxPathLogMap().entrySet()) {
				String sessionKey = pair.getKey();
				String xPath = pair.getValue();
				String result = getResultFromXPath(soapMessage, xPath);
				if (!result.isEmpty()) {
					messageProperties.put(sessionKey, result);
					xPathLogKeys.append(",").append(sessionKey); // Only pass items that have been found, otherwise logs will clutter with NULL.
				}
			}
			messageProperties.put("xPathLogKeys", xPathLogKeys.toString());
		} catch (JMSException | IOException e) {
			log.debug("ignoring Exception", e);
		}
	}

	protected String getResultFromXPath(String message, String xPathExpression) {
		String found = "";
		if(message != null && !message.isEmpty() && (XmlUtils.isWellFormed(message))) {
			try {
				TransformerPool test = LOG_KEY_TRANSFORMER_POOLS.get(xPathExpression);
				found = test.transform(message, null);

				//xPath not found and message length is 0 but not null nor ""
				if(found.isEmpty()) found = "";
			} catch (Exception e) {
				log.debug("could not evaluate xpath expression",e);
			}
		}
		return found;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessageWrapper, PipeLineSession pipeLineSession) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessageWrapper, pipeLineSession);
		if (getMessageProtocol() == MessageProtocol.RR) {
			Destination replyTo = (Destination) pipeLineSession.get("replyTo");
			if (replyTo == null) {
				log.warn("no replyTo address found for messageProtocol [{}], response is lost", getMessageProtocol());
			}
		}
	}

	@Override
	protected Map<String, Object> getMessageProperties(PipeLineSession session) {
		Map<String, Object> properties = super.getMessageProperties(session);

		if (isCopyAEProperties() && session != null) {
			if(properties == null)
				properties = new HashMap<>();

			properties.putAll(session.entrySet().stream()
					.filter(entry -> entry.getKey().startsWith("ae_"))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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
	 */
	@Default("false")
	public void setCopyAEProperties(boolean b) {
		copyAEProperties = b;
	}

	/** if messageProtocol=RR, default value is: true */
	@Override
	public void setForceMessageIdAsCorrelationId(Boolean force) {
		super.setForceMessageIdAsCorrelationId(force);
	}

	/** if messageProtocol=FF, default value is: false */
	@Override
	public void setUseReplyTo(boolean newUseReplyTo) {
		super.setUseReplyTo(newUseReplyTo);
	}

	/** Comma separated list of all XPath keys that need to be logged. (overrides <code>msg.log.keys</code> property) */
	public void setxPathLoggingKeys(String string) {
		xPathLoggingKeys = string;
	}
}
