/*
   Copyright 2013-2016, 2020 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.extensions.tibco;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.Resource;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.jms.BytesMessageInputStream;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.TimeoutGuardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TransformerPool;

/**
 * Sends a message to a Tibco queue.
 *
 * @ff.parameter url When a parameter with name url is present, it is used instead of the url specified by the attribute
 * @ff.parameter authAlias When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute
 * @ff.parameter username When a parameter with name userName is present, it is used instead of the userName specified by the attribute
 * @ff.parameter password When a parameter with name password is present, it is used instead of the password specified by the attribute
 * @ff.parameter queueName When a parameter with name queueName is present, it is used instead of the queueName specified by the attribute
 * @ff.parameter messageProtocol When a parameter with name messageProtocol is present, it is used instead of the messageProtocol specified by the attribute
 * @ff.parameter replyTimeout When a parameter with name replyTimeout is present, it is used instead of the replyTimeout specified by the attribute
 * @ff.parameter When a parameter with name soapAction is present, it is used instead of the soapAction specified by the attribute
 *
 * @author Peter Leeuwenburgh
 */
public class SendTibcoMessage extends TimeoutGuardPipe {

	private String url;
	private String authAlias;
	private String username;
	private String password;
	private String queueName;
	private MessageProtocol messageProtocol;
	private int replyTimeout = 5000;
	private String soapAction;
	private String emsPropertiesFile;
	private Map<String, Object> emsProperties;

	public enum MessageProtocol implements DocumentedEnum {
		/** Request-Reply */
		@EnumLabel("RR") REQUEST_REPLY,
		/** Fire &amp; Forget */
		@EnumLabel("FF") FIRE_AND_FORGET
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getParameterList().hasParameter("userName")) {
			ConfigurationWarnings.add(this, log, "parameter [userName] has been replaced with [username]");
		}

		if(StringUtils.isNotEmpty(emsPropertiesFile)) {
			try {
				emsProperties = new TibcoEmsProperties(this, emsPropertiesFile);
			} catch (IOException e) {
				throw new ConfigurationException("unable to find/load the EMS properties file", e);
			}
		} else {
			emsProperties = Collections.emptyMap();
		}

		super.configure();
	}

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {

		String urlWork;
		String authAliasWork;
		String userNameWork;
		String passwordWork;
		String queueNameWork;
		MessageProtocol protocol = getMessageProtocol();
		int replyTimeoutWork;
		String soapActionWork;

		String result;
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(input, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception on extracting parameters", e);
		}

		urlWork = getParameterValue(pvl, "url");
		if (urlWork == null) {
			urlWork = getUrl();
		}
		authAliasWork = getParameterValue(pvl, "authAlias");
		if (authAliasWork == null) {
			authAliasWork = getAuthAlias();
		}
		userNameWork = pvl.contains("username") ? getParameterValue(pvl, "username") : getParameterValue(pvl, "userName");
		if (userNameWork == null) {
			userNameWork = getUsername();
		}
		passwordWork = getParameterValue(pvl, "password");
		if (passwordWork == null) {
			passwordWork = getPassword();
		}
		queueNameWork = getParameterValue(pvl, "queueName");
		if (queueNameWork == null) {
			queueNameWork = getQueueName();
		}
		String protocolParam = getParameterValue(pvl, "messageProtocol");
		if (protocolParam != null) {
			protocol = EnumUtils.parse(MessageProtocol.class, protocolParam);
		}
		String replyTimeoutStr = getParameterValue(pvl, "replyTimeout");
		if (replyTimeoutStr == null) {
			replyTimeoutWork = getReplyTimeout();
		} else {
			replyTimeoutWork = Integer.parseInt(replyTimeoutStr);
		}
		soapActionWork = getParameterValue(pvl, "soapAction");
		if (soapActionWork == null)
			soapActionWork = getSoapAction();

		if (StringUtils.isEmpty(soapActionWork) && !StringUtils.isEmpty(queueNameWork)) {
			String[] q = queueNameWork.split("\\.");
			if (q.length>0) {
				if ("P2P".equalsIgnoreCase(q[0]) && q.length>=4) {
					soapActionWork = q[3];
				} else if ("ESB".equalsIgnoreCase(q[0]) && q.length==8) {
					soapActionWork = q[5] + "_" + q[6];
				} else if ("ESB".equalsIgnoreCase(q[0]) && q.length>8) {
					soapActionWork = q[6] + "_" + q[7];
				}
			}
		}

		if (StringUtils.isEmpty(soapActionWork)) {
			log.debug("deriving default soapAction");
			try {
				Resource resource = Resource.getResource(this, "/xml/xsl/esb/soapAction.xsl");
				TransformerPool tp = TransformerPool.getInstance(resource, 2);
				soapActionWork = tp.transformToString(input.asString(), null);
			} catch (Exception e) {
				log.error("failed to execute soapAction.xsl");
			}
		}

		if (protocol == null) {
			throw new PipeRunException(this, "messageProtocol must be set");
		}

		CredentialFactory cf = new CredentialFactory(authAliasWork, userNameWork, passwordWork);
		validateQueueName(urlWork, cf, queueNameWork);

		ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(urlWork, null, emsProperties); // url, clientid, properties
		try (Connection connection = factory.createConnection(cf.getUsername(), cf.getPassword());
			Session jSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageProducer msgProducer = jSession.createProducer(jSession.createQueue(queueNameWork))) {

			TextMessage msg = jSession.createTextMessage();
			msg.setText(input.asString());
			Destination replyQueue = null;
			if (protocol == MessageProtocol.REQUEST_REPLY) {
				replyQueue = jSession.createTemporaryQueue();
				msg.setJMSReplyTo(replyQueue);
				msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setTimeToLive(replyTimeoutWork);
			} else {
				msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
			}
			if (StringUtils.isNotEmpty(soapActionWork)) {
				log.debug("setting [SoapAction] property to value [{}]", soapActionWork);
				msg.setStringProperty("SoapAction", soapActionWork);
			}
			msgProducer.send(msg);
			if (log.isDebugEnabled()) {
				log.debug("sent message [{}] to [{}] msgID [{}] correlationID [{}] replyTo [{}]", msg.getText(), msgProducer.getDestination(), msg.getJMSMessageID(), msg.getJMSCorrelationID(), msg.getJMSReplyTo());
			} else {
				if (log.isInfoEnabled()) {
					log.info("sent message to [{}] msgID [{}] correlationID [{}] replyTo [{}]", msgProducer.getDestination(), msg.getJMSMessageID(), msg.getJMSCorrelationID(), msg.getJMSReplyTo());
				}
			}
			if (protocol == MessageProtocol.REQUEST_REPLY) {
				String replyCorrelationId = msg.getJMSMessageID();
				try (MessageConsumer msgConsumer = jSession.createConsumer(replyQueue, "JMSCorrelationID='" + replyCorrelationId+ "'")) {
					log.debug("start waiting for reply on [{}] selector [{}] for [{}] ms", replyQueue, replyCorrelationId, replyTimeoutWork);

					connection.start();
					jakarta.jms.Message rawReplyMsg = msgConsumer.receive(replyTimeoutWork);
					if (rawReplyMsg == null) {
						throw new PipeRunException(this, "did not receive reply on [" + replyQueue+ "] replyCorrelationId [" + replyCorrelationId+ "] within [" + replyTimeoutWork + "] ms");
					}
					if (rawReplyMsg instanceof TextMessage replyMsg) {
						result = replyMsg.getText();
					} else if (rawReplyMsg instanceof BytesMessage bytesMessage) {
						InputStream inputStream = new BytesMessageInputStream(bytesMessage);
						result = StreamUtil.streamToString(inputStream);
					} else {
						throw new PipeRunException(this, "Unsupported message type received: " + ClassUtils.classNameOf(rawReplyMsg));
					}
				}

			} else {
				result = msg.getJMSMessageID();
			}
		} catch (IOException|JMSException e) {
			throw new PipeRunException(this, "exception on sending message to Tibco queue", e);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	private void validateQueueName(String urlWork, CredentialFactory cf, String queueNameWork) throws PipeRunException {
		TibjmsAdmin admin;
		try {
			admin = TibcoUtils.getActiveServerAdmin(urlWork, cf, emsProperties);
		} catch (TibjmsAdminException e) {
			log.debug("caught exception, cannot validate Tibco queue name", e);
			return;
		}
		if (admin == null) {
			return;
		}
		QueueInfo queueInfo;
		try {
			queueInfo = admin.getQueue(queueNameWork);
		} catch (Exception e) {
			throw new PipeRunException(this, "exception on getting queue info", e);
		} finally {
			TibcoUtils.closeAdminClient(admin);
		}
		if (queueInfo == null) {
			throw new PipeRunException(this, "queue [" + queueNameWork + "] does not exist");
		}
	}

	public String getUrl() {
		return url;
	}

	/** URL or base of URL to be used. When multiple URLs are defined (comma separated list), the first URL is used of which the server has an active state */
	public void setUrl(String string) {
		url = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	/** alias used to obtain credentials for authentication to host */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getUsername() {
		return username;
	}

	/** username used in authentication to host */
	public void setUsername(String string) {
		username = string;
	}

	public String getPassword() {
		return password;
	}

	/** password used in authentication to host */
	public void setPassword(String string) {
		password = string;
	}

	public String getQueueName() {
		return queueName;
	}

	/** The name of the queue which is used for browsing one queue */
	public void setQueueName(String string) {
		queueName = string;
	}

	public MessageProtocol getMessageProtocol() {
		return messageProtocol;
	}

	/** Protocol of Tibco service to be called */
	public void setMessageProtocol(MessageProtocol string) {
		messageProtocol = string;
	}

	public int getReplyTimeout() {
		return replyTimeout;
	}

	/** Maximum time in milliseconds to wait for a reply. 0 means no timeout. (Only for messageProtocol=RR)
	 * @ff.default 5000
	 */
	public void setReplyTimeout(int i) {
		replyTimeout = i;
	}

	/** If empty then derived from queueName (if $messagingLayer='P2P' then '$applicationFunction' else '$operationName_$operationVersion) */
	public void setSoapAction(String string) {
		soapAction = string;
	}

	public String getSoapAction() {
		return soapAction;
	}

	/** Location to a <code>jndi.properties</code> file for additional EMS (SSL) properties */
	public void setEmsPropertiesFile(String propertyFile) {
		emsPropertiesFile = propertyFile;
	}
}
