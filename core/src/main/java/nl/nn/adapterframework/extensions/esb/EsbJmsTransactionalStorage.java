/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsTransactionalStorage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * ESB (Enterprise Service Bus) extension of JmsTransactionalStorage.
 * 
 * <p>
 * Depending on the <code>type</code> of the <code>TransactionalStorage</code>
 * one of the following messages is sent:
 * <ul>
 * <li><code>errorStore</code>:
 * ESB.Infrastructure.US.Log.BusinessLog.2.ExceptionLog.1.Action</li>
 * <li><code>messageLog</code>:
 * ESB.Infrastructure.US.Log.BusinessLog.2.AuditLog.1.Action</li>
 * </ul>
 * 
 * <p>
 * <b>Configuration </b><i>(where deviating from
 * JmsTransactionalStorage)</i><b>:</b>
 * <table border="1">
 * <tr>
 * <th>attributes</th>
 * <th>description</th>
 * <th>default</th>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public class EsbJmsTransactionalStorage<S extends Serializable> extends JmsTransactionalStorage<S> {
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private TransformerPool exceptionLogTp = null;
	private TransformerPool auditLogTp = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String exceptionLogString = "/xml/xsl/esb/exceptionLog.xsl";
		String auditLogString = "/xml/xsl/esb/auditLog.xsl";
		try {
			Resource exceptionLogResource = Resource.getResource(classLoader, exceptionLogString);
			if (exceptionLogResource == null) {
				throw new ConfigurationException(getLogPrefix() + "cannot find stylesheet [" + exceptionLogString + "]");
			}
			exceptionLogTp = TransformerPool.getInstance(exceptionLogResource, 2);
		} catch (IOException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot retrieve [" + exceptionLogString + "]", e);
		} catch (TransformerConfigurationException te) {
			throw new ConfigurationException(getLogPrefix() + "got error creating transformer from file [" + exceptionLogString + "]", te);
		}
		try {
			Resource auditLogResource =Resource.getResource(classLoader, auditLogString);
			if (auditLogResource == null) {
				throw new ConfigurationException(getLogPrefix() + "cannot find stylesheet [" + auditLogString + "]");
			}
			auditLogTp = TransformerPool.getInstance(auditLogResource, 2);
		} catch (IOException e) {
			throw new ConfigurationException(getLogPrefix() + "cannot retrieve [" + auditLogString + "]", e);
		} catch (TransformerConfigurationException te) {
			throw new ConfigurationException(getLogPrefix() + "got error creating transformer from file [" + auditLogString + "]", te);
		}
	}

	@Override
	public void open() throws ListenerException {
		try {
			super.open();
		} catch (Exception e) {
			throw new ListenerException(e);
		}
		if (exceptionLogTp != null) {
			try {
				exceptionLogTp.open();
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix() + "cannot start TransformerPool for exceptionLog", e);
			}
		}
		if (auditLogTp != null) {
			try {
				auditLogTp.open();
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix() + "cannot start TransformerPool for auditLog", e);
			}
		}
	}

	@Override
	public void close() {
		super.close();
		if (exceptionLogTp != null) {
			exceptionLogTp.close();
		}
		if (auditLogTp != null) {
			auditLogTp.close();
		}
	}

	@Override
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException {
		Session session = null;
		try {
			Map<String,Object> parameterValues = createParameterValues(messageId, correlationId, receivedDate, comments, message);
			String logRequest;
			if (getType().equalsIgnoreCase("E")) {
				log.debug(getLogPrefix() + "creating exceptionLog request");
				logRequest = exceptionLogTp.transform("<dummy/>", parameterValues, true);
			} else {
				log.debug(getLogPrefix() + "creating auditLog request");
				logRequest = auditLogTp.transform("<dummy/>", parameterValues, true);
			}
			session = createSession();
			javax.jms.Message msg = createMessage(session, null, new Message(logRequest));
			String returnMessage = send(session, getDestination(), msg);
			log.debug(getLogPrefix() + "sent message [" + logRequest + "] " + "to [" + getDestination() + "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID() + "]");
			return returnMessage;
		} catch (Exception e) {
			throw new SenderException(e);
		} finally {
			closeSession(session);
		}
	}

	private Map<String,Object> createParameterValues(String messageId, String correlationId, Date receivedDate, String comments, S message) throws JMSException, DomBuilderException, TransformerException, IOException {
		Map<String,Object> parameterValues = new HashMap<>();
		parameterValues.put("fromId", AppConstants.getInstance().getProperty("instance.name", ""));
		parameterValues.put("conversationId", 	Misc.getHostname() + "_" + Misc.createSimpleUUID());
		parameterValues.put("messageId", 		Misc.getHostname() + "_" + Misc.createSimpleUUID());
		parameterValues.put("timestamp", 		DateUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss"));
		parameterValues.put("msgMessageId", 	messageId);
		parameterValues.put("msgCorrelationId", correlationId);
		parameterValues.put("msgTimestamp", 	DateUtils.format( receivedDate.getTime(), "yyyy-MM-dd'T'HH:mm:ss"));
		parameterValues.put("slotId", 			getSlotId());
		if (getType().equalsIgnoreCase("E")) {
			parameterValues.put("errorText", comments);
		} else {
			if (getType().equalsIgnoreCase("L")) {
				parameterValues.put("msgType", "sent");
			} else {
				parameterValues.put("msgType", "received");
			}
		}
		String rawMessageText;
		if (message instanceof String) {
			rawMessageText = message.toString();
		} else {
			try {
				TextMessage textMessage = null;
				textMessage = (TextMessage) message;
				rawMessageText = textMessage.getText();
			} catch (ClassCastException e) {
				log.error("message was not of type TextMessage, but [" + message.getClass().getName() + "]", e);
				rawMessageText = message.toString();
			}
		}
		parameterValues.put("msg", rawMessageText);
		return parameterValues;
	}

	@Override
	public int getMessageCount() throws ListenerException {
		return -1;
	}
}
