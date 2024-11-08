/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Resource;
import org.frankframework.core.SenderException;
import org.frankframework.doc.Category;
import org.frankframework.jms.JmsTransactionalStorage;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.UUIDUtil;

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
 * </p>
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
@Category(Category.Type.NN_SPECIAL)
public class EsbJmsTransactionalStorage<S extends Serializable> extends JmsTransactionalStorage<S> {
	private TransformerPool exceptionLogTp = null;
	private TransformerPool auditLogTp = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String exceptionLogString = "/xml/xsl/esb/exceptionLog.xsl";
		String auditLogString = "/xml/xsl/esb/auditLog.xsl";
		try {
			Resource exceptionLogResource = Resource.getResource(this, exceptionLogString);
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
			Resource auditLogResource =Resource.getResource(this, auditLogString);
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
	public void start() {
		super.start();

		if (exceptionLogTp != null) {
			try {
				exceptionLogTp.open();
			} catch (Exception e) {
				throw new LifecycleException(getLogPrefix() + "cannot start TransformerPool for exceptionLog", e);
			}
		}
		if (auditLogTp != null) {
			try {
				auditLogTp.open();
			} catch (Exception e) {
				throw new LifecycleException(getLogPrefix() + "cannot start TransformerPool for auditLog", e);
			}
		}
	}

	@Override
	public void stop() {
		super.stop();
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
			if ("E".equalsIgnoreCase(getType())) {
				log.debug("{}creating exceptionLog request", getLogPrefix());
				logRequest = exceptionLogTp.transform("<dummy/>", parameterValues, true);
			} else {
				log.debug("{}creating auditLog request", getLogPrefix());
				logRequest = auditLogTp.transform("<dummy/>", parameterValues, true);
			}
			session = createSession();
			jakarta.jms.Message msg = createMessage(session, null, new Message(logRequest));
			String returnMessage = send(session, getDestination(), msg);
			log.debug("{}sent message [{}] to [{}] msgID [{}] correlationID [{}]", getLogPrefix(), logRequest, getDestination(), msg.getJMSMessageID(), msg.getJMSCorrelationID());
			return returnMessage;
		} catch (Exception e) {
			throw new SenderException(e);
		} finally {
			closeSession(session);
		}
	}

	private Map<String,Object> createParameterValues(String messageId, String correlationId, Date receivedDate, String comments, S message) throws JMSException {
		Map<String,Object> parameterValues = new HashMap<>();
		parameterValues.put("fromId", AppConstants.getInstance().getProperty("instance.name", ""));
		parameterValues.put("conversationId", 	Misc.getHostname() + "_" + UUIDUtil.createSimpleUUID());
		parameterValues.put("messageId", 		Misc.getHostname() + "_" + UUIDUtil.createSimpleUUID());
		parameterValues.put("timestamp", 		DateFormatUtils.now(DateFormatUtils.FULL_ISO_FORMATTER));
		parameterValues.put("msgMessageId", 	messageId);
		parameterValues.put("msgCorrelationId", correlationId);
		parameterValues.put("msgTimestamp", 	DateFormatUtils.format( receivedDate));
		parameterValues.put("slotId", 			getSlotId());
		if ("E".equalsIgnoreCase(getType())) {
			parameterValues.put("errorText", comments);
		} else {
			if ("L".equalsIgnoreCase(getType())) {
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
				TextMessage textMessage = (TextMessage) message;
				rawMessageText = textMessage.getText();
			} catch (ClassCastException e) {
				log.error("message was not of type TextMessage, but [{}]", message.getClass().getName(), e);
				rawMessageText = message.toString();
			}
		}
		parameterValues.put("msg", rawMessageText);
		return parameterValues;
	}

	@Override
	public int getMessageCount() {
		return -1;
	}
}
