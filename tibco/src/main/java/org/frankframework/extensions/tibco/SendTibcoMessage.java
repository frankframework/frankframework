/*
   Copyright 2013-2016, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
		/** Fire & Forget */
		@EnumLabel("FF") FIRE_AND_FORGET
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getParameterList() != null && getParameterList().hasParameter("userName")) {
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
		Connection connection = null;
		Session jSession = null;
		MessageProducer msgProducer = null;
		Destination destination = null;

		String url_work;
		String authAlias_work;
		String userName_work;
		String password_work;
		String queueName_work;
		MessageProtocol protocol = getMessageProtocol();
		int replyTimeout_work;
		String soapAction_work;

		String result = null;
		try {
			input.preserve();
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot preserve input",e);
		}
		ParameterValueList pvl = null;
		if (getParameterList()!=null) {
			try {
				pvl = getParameterList().getValues(input, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception on extracting parameters", e);
			}
		}

		url_work = getParameterValue(pvl, "url");
		if (url_work == null) {
			url_work = getUrl();
		}
		authAlias_work = getParameterValue(pvl, "authAlias");
		if (authAlias_work == null) {
			authAlias_work = getAuthAlias();
		}
		userName_work = pvl.contains("username") ? getParameterValue(pvl, "username") : getParameterValue(pvl, "userName");
		if (userName_work == null) {
			userName_work = getUsername();
		}
		password_work = getParameterValue(pvl, "password");
		if (password_work == null) {
			password_work = getPassword();
		}
		queueName_work = getParameterValue(pvl, "queueName");
		if (queueName_work == null) {
			queueName_work = getQueueName();
		}
		String protocolParam = getParameterValue(pvl, "messageProtocol");
		if (protocolParam != null) {
			protocol = EnumUtils.parse(MessageProtocol.class, protocolParam);
		}
		String replyTimeout_work_str = getParameterValue(pvl, "replyTimeout");
		if (replyTimeout_work_str == null) {
			replyTimeout_work = getReplyTimeout();
		} else {
			replyTimeout_work = Integer.parseInt(replyTimeout_work_str);
		}
		soapAction_work = getParameterValue(pvl, "soapAction");
		if (soapAction_work == null)
			soapAction_work = getSoapAction();

		if (StringUtils.isEmpty(soapAction_work) && !StringUtils.isEmpty(queueName_work)) {
			String[] q = queueName_work.split("\\.");
			if (q.length>0) {
				if ("P2P".equalsIgnoreCase(q[0]) && q.length>=4) {
					soapAction_work = q[3];
				} else if ("ESB".equalsIgnoreCase(q[0]) && q.length==8) {
					soapAction_work = q[5] + "_" + q[6];
				} else if ("ESB".equalsIgnoreCase(q[0]) && q.length>8) {
					soapAction_work = q[6] + "_" + q[7];
				}
			}
		}

		if (StringUtils.isEmpty(soapAction_work)) {
			log.debug("deriving default soapAction");
			try {
				Resource resource = Resource.getResource(this, "/xml/xsl/esb/soapAction.xsl");
				TransformerPool tp = TransformerPool.getInstance(resource, 2);
				soapAction_work = tp.transform(input.asString(), null);
			} catch (Exception e) {
				log.error("failed to execute soapAction.xsl");
			}
		}

		if (protocol == null) {
			throw new PipeRunException(this, "messageProtocol must be set");
		}

		CredentialFactory cf = new CredentialFactory(authAlias_work, userName_work, password_work);
		try {
			TibjmsAdmin admin;
			try {
				admin = TibcoUtils.getActiveServerAdmin(url_work, cf, emsProperties);
			} catch (TibjmsAdminException e) {
				log.debug("caught exception", e);
				admin = null;
			}
			if (admin != null) {
				QueueInfo queueInfo;
				try {
					queueInfo = admin.getQueue(queueName_work);
				} catch (Exception e) {
					throw new PipeRunException(this, "exception on getting queue info", e);
				}
				if (queueInfo == null) {
					throw new PipeRunException(this, "queue [" + queueName_work + "] does not exist");
				}

				try {
					admin.close();
				} catch (TibjmsAdminException e) {
					log.warn("exception on closing Tibjms Admin", e);
				}
			}

			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(url_work, null, emsProperties); //url, clientid, properties
			connection = factory.createConnection(cf.getUsername(), cf.getPassword());
			jSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = jSession.createQueue(queueName_work);

			msgProducer = jSession.createProducer(destination);
			TextMessage msg = jSession.createTextMessage();
			msg.setText(input.asString());
			Destination replyQueue = null;
			if (protocol == MessageProtocol.REQUEST_REPLY) {
				replyQueue = jSession.createTemporaryQueue();
				msg.setJMSReplyTo(replyQueue);
				msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setTimeToLive(replyTimeout_work);
			} else {
				msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
			}
			if (StringUtils.isNotEmpty(soapAction_work)) {
				log.debug("setting [SoapAction] property to value [{}]", soapAction_work);
				msg.setStringProperty("SoapAction", soapAction_work);
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
				MessageConsumer msgConsumer = jSession.createConsumer(replyQueue, "JMSCorrelationID='" + replyCorrelationId+ "'");
				log.debug("start waiting for reply on [{}] selector [{}] for [{}] ms", replyQueue, replyCorrelationId, replyTimeout_work);

				connection.start();
				jakarta.jms.Message rawReplyMsg = msgConsumer.receive(replyTimeout_work);
				if (rawReplyMsg == null) {
					throw new PipeRunException(this, "did not receive reply on [" + replyQueue+ "] replyCorrelationId [" + replyCorrelationId+ "] within [" + replyTimeout_work + "] ms");
				}
				if (rawReplyMsg instanceof TextMessage replyMsg) {
					result = replyMsg.getText();
				} else if (rawReplyMsg instanceof BytesMessage bytesMessage) {
					InputStream inputStream = new BytesMessageInputStream(bytesMessage);
					result = StreamUtil.streamToString(inputStream);
				} else {
					throw new PipeRunException(this, "Unsupported message type received: " + ClassUtils.classNameOf(rawReplyMsg));
				}

			} else {
				result = msg.getJMSMessageID();
			}
		} catch (IOException|JMSException e) {
			throw new PipeRunException(this, "exception on sending message to Tibco queue", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn("exception on closing connection", e);
				}
			}
		}
		return new PipeRunResult(getSuccessForward(), result);
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
