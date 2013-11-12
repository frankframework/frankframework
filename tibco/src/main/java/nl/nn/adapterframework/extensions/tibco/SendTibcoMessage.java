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
package nl.nn.adapterframework.extensions.tibco;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Sends a message to a Tibco queue.
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueueName(String) queueName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of Tibco service to be called. Possible values
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyTimeout(int) replyTimeout}</td><td>maximum time in ms to wait for a reply. 0 means no timeout. (Only for messageProtocol=RR)</td><td>5000</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>url</td><td>string</td><td>When a parameter with name serviceId is present, it is used instead of the serviceId specified by the attribute</td></tr>
 * <tr><td>authAlias</td><td>string</td><td>When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute</td></tr>
 * <tr><td>userName</td><td>string</td><td>When a parameter with name userName is present, it is used instead of the userName specified by the attribute</td></tr>
 * <tr><td>password</td><td>string</td><td>When a parameter with name password is present, it is used instead of the password specified by the attribute</td></tr>
 * <tr><td>queueName</td><td>string</td><td>When a parameter with name queueName is present, it is used instead of the queueName specified by the attribute</td></tr>
 * <tr><td>messageProtocol</td><td>string</td><td>When a parameter with name messageProtocol is present, it is used instead of the messageProtocol specified by the attribute</td></tr>
 * <tr><td>replyTimeout</td><td>string</td><td>When a parameter with name replyTimeout is present, it is used instead of the replyTimeout specified by the attribute</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class SendTibcoMessage extends FixedForwardPipe {
	private final static String REQUEST_REPLY = "RR";
	private final static String FIRE_AND_FORGET = "FF";

	private String url;
	private String authAlias;
	private String userName;
	private String password;
	private String queueName;
	private String messageProtocol;
	private int replyTimeout = 5000;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		Connection connection = null;
		Session jSession = null;
		MessageProducer msgProducer = null;
		Destination destination = null;

		String url_work;
		String authAlias_work;
		String userName_work;
		String password_work;
		String queueName_work;
		String messageProtocol_work;
		int replyTimeout_work;

		String result = null;

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on extracting parameters", e);
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
		userName_work = getParameterValue(pvl, "userName");
		if (userName_work == null) {
			userName_work = getUserName();
		}
		password_work = getParameterValue(pvl, "password");
		if (password_work == null) {
			password_work = getPassword();
		}
		queueName_work = getParameterValue(pvl, "queueName");
		if (queueName_work == null) {
			queueName_work = getQueueName();
		}
		messageProtocol_work = getParameterValue(pvl, "messageProtocol");
		if (messageProtocol_work == null) {
			messageProtocol_work = getMessageProtocol();
		}
		String replyTimeout_work_str = getParameterValue(pvl, "replyTimeout");
		if (replyTimeout_work_str == null) {
			replyTimeout_work = getReplyTimeout();
		} else {
			replyTimeout_work = Integer.parseInt(replyTimeout_work_str);
		}

		if (messageProtocol_work == null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "messageProtocol must be set");
		}
		if (!messageProtocol_work.equalsIgnoreCase(REQUEST_REPLY)
				&& !messageProtocol_work.equalsIgnoreCase(FIRE_AND_FORGET)) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "illegal value for messageProtocol ["
					+ messageProtocol_work + "], must be '" + REQUEST_REPLY
					+ "' or '" + FIRE_AND_FORGET + "'");
		}

		CredentialFactory cf = new CredentialFactory(authAlias_work, userName_work, password_work);
		try {
			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
					url_work);
			connection = factory.createConnection(cf.getUsername(), cf.getPassword());
			jSession = connection.createSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			destination = jSession.createQueue(queueName_work);
			msgProducer = jSession.createProducer(destination);
			TextMessage msg = jSession.createTextMessage();
			msg.setText(input.toString());
			Destination replyQueue = null;
			if (messageProtocol_work.equalsIgnoreCase(REQUEST_REPLY)) {
				replyQueue = jSession.createTemporaryQueue();
				msg.setJMSReplyTo(replyQueue);
				msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				msgProducer.setTimeToLive(replyTimeout_work);
			} else {
				msg.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
				msgProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
			}
			String soapAction = null;
			if (queueName_work.contains(".")) {
				String[] q = StringUtils.split(queueName_work, ".");
				if (q[0].equalsIgnoreCase("P2P") && q.length>=4) {
					soapAction = q[2] + "_" + q[3];
				} else if (q[0].equalsIgnoreCase("ESB") && q.length>=8) {
					soapAction = q[6] + "_" + q[7];
				}
			}
			if (soapAction != null) {
				log.debug(getLogPrefix(session) + "setting [SoapAction] property to value [" + soapAction + "]");
				msg.setStringProperty("SoapAction", soapAction);
			}
			msgProducer.send(msg);
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix(session) + "sent message ["
						+ msg.getText() + "] " + "to ["
						+ msgProducer.getDestination() + "] " + "msgID ["
						+ msg.getJMSMessageID() + "] " + "correlationID ["
						+ msg.getJMSCorrelationID() + "] " + "replyTo ["
						+ msg.getJMSReplyTo() + "]");
			} else {
				if (log.isInfoEnabled()) {
					log.info(getLogPrefix(session) + "sent message to ["
							+ msgProducer.getDestination() + "] " + "msgID ["
							+ msg.getJMSMessageID() + "] " + "correlationID ["
							+ msg.getJMSCorrelationID() + "] " + "replyTo ["
							+ msg.getJMSReplyTo() + "]");
				}
			}
			if (messageProtocol_work.equalsIgnoreCase(REQUEST_REPLY)) {
				String replyCorrelationId = msg.getJMSMessageID();
				MessageConsumer msgConsumer = jSession.createConsumer(
						replyQueue, "JMSCorrelationID='" + replyCorrelationId
								+ "'");
				log.debug(getLogPrefix(session)
						+ "] start waiting for reply on [" + replyQueue
						+ "] selector [" + replyCorrelationId + "] for ["
						+ replyTimeout_work + "] ms");
				try {
					connection.start();
					Message rawReplyMsg = msgConsumer
							.receive(replyTimeout_work);
					if (rawReplyMsg == null) {
						throw new PipeRunException(this, getLogPrefix(session)
								+ "did not receive reply on [" + replyQueue
								+ "] replyCorrelationId [" + replyCorrelationId
								+ "] within [" + replyTimeout_work + "] ms");
					}
					TextMessage replyMsg = (TextMessage) rawReplyMsg;
					result = replyMsg.getText();
				} finally {
				}

			} else {
				result = msg.getJMSMessageID();
			}
		} catch (JMSException e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " exception on sending message to Tibco queue", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn(getLogPrefix(session)
							+ "exception on closing connection", e);
				}
			}
		}
		return new PipeRunResult(getForward(), result);
	}

	private String getParameterValue(ParameterValueList pvl,
			String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(parameterName)) {
					return pvl.getParameterValue(i).asStringValue(null);
				}
			}
		}
		return null;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String string) {
		url = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String string) {
		userName = string;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String string) {
		password = string;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String string) {
		queueName = string;
	}

	public String getMessageProtocol() {
		return messageProtocol;
	}

	public void setMessageProtocol(String string) {
		messageProtocol = string;
	}

	public int getReplyTimeout() {
		return replyTimeout;
	}

	public void setReplyTimeout(int i) {
		replyTimeout = i;
	}
}
