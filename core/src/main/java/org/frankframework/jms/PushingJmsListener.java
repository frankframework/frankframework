/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.jms;

import java.util.Map;

import jakarta.jms.Destination;
import jakarta.jms.Message;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IKnowsDeliveryCount;
import org.frankframework.core.IListenerConnector;
import org.frankframework.core.IListenerConnector.CacheMode;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPortConnectedListener;
import org.frankframework.core.ISender;
import org.frankframework.core.IThreadCountControllable;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Mandatory;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.CredentialFactory;

/**
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 *
 * This version of the <code>JmsListener</code> supports distributed transactions using the XA-protocol.
 * No special action is required to have the listener join the transaction.
 *
 * <p><b>Using jmsTransacted and acknowledgement</b><br/>
 * If jmsTransacted is set <code>true</code>, it should ensure that a message is received and processed on
 * a both or nothing basis. IBIS will commit the the message, otherwise perform rollback. However, using
 * jmsTransacted, IBIS does not bring transactions within the adapters under transaction control,
 * compromising the idea of atomic transactions. In the roll-back situation messages sent to other
 * destinations within the Pipeline are NOT rolled back if jmsTransacted is set <code>true</code>! In
 * the failure situation the message is therefore completely processed, and the roll back does not mean
 * that the processing is rolled back! To obtain the correct (transactional) behaviour, set
 * <code>transacted</code>="true" for the enclosing Receiver. Do not use jmsTransacted for any new situation.
 *
 * </p><p>
 * Setting {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the success state.
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages.
 * In cases where the client is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode,
 * since a session has lower overhead in trying to prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} will only be processed if
 * the setting for {@link #setTransacted(boolean) listener.transacted} as well as for
 * {@link #setJmsTransacted(boolean) listener.jmsTransacted} is false.</p>
 *
 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 *
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * {@link jakarta.jms.TextMessage}s and {@link jakarta.jms.BytesMessage}<br/><br/>
 * </p>
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PushingJmsListener extends AbstractJmsListener implements IPortConnectedListener<Message>, IThreadCountControllable, IKnowsDeliveryCount<Message> {

	private @Getter CacheMode cacheMode;
	private @Getter long pollGuardInterval = Long.MIN_VALUE;

	private @Getter @Setter IListenerConnector<Message> jmsConnector;
	private @Getter @Setter IMessageHandler<Message> handler;
	private @Getter @Setter Receiver<Message> receiver;
	private @Getter @Setter IbisExceptionListener exceptionListener;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jmsConnector==null) {
			throw new ConfigurationException(getLogPrefix()+" has no jmsConnector. It should be configured via springContext.xml");
		}
		Destination destination;
		try {
			destination = getDestination();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
		if (getPollGuardInterval() == Long.MIN_VALUE) {
			setPollGuardInterval(getTimeout() * 10);
		}
		if (getPollGuardInterval() <= getTimeout()) {
			ConfigurationWarnings.add(this, log, "The pollGuardInterval ["+getPollGuardInterval()+"] should be larger than the receive timeout ["+ getTimeout()+"]");
		}
		CredentialFactory credentialFactory=null;
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			credentialFactory=new CredentialFactory(getAuthAlias());
		}
		try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), credentialFactory,
					destination, getExceptionListener(), getCacheMode(), getAcknowledgeMode().getAcknowledgeMode(),
					isJmsTransacted(), getMessageSelector(), getTimeout(), getPollGuardInterval());
		} catch (JmsException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void start() {
		super.start();
		jmsConnector.start();
	}

	@Override
	public void stop() {
		try {
			jmsConnector.stop();
		} catch (Exception e) {
			log.warn("{}caught exception stopping listener", getLogPrefix(), e);
		} finally {
			super.stop();
		}
	}

	@Override
	public RawMessageWrapper<Message> wrapRawMessage(Message rawMessage, PipeLineSession session) throws ListenerException {
		Map<String, Object> messageProperties = extractMessageProperties(rawMessage);
		session.putAll(messageProperties);
		return new RawMessageWrapper<>(rawMessage, session.getMessageId(), session.getCorrelationId());
	}

	@Override
	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			return tcc.isThreadCountReadable();
		}
		return false;
	}

	@Override
	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			return tcc.isThreadCountControllable();
		}
		return false;
	}

	@Override
	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			tcc.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable tcc) {

			tcc.decreaseThreadCount();
		}
	}

	@Override
	public int getDeliveryCount(RawMessageWrapper<Message> rawMessage) {
		try {
			Message message=rawMessage.getRawMessage();
			// Note: Tibco doesn't set the JMSXDeliveryCount for messages
			// delivered for the first time (when JMSRedelivered is set to
			// false). Hence when set is has a value of 2 or higher. When not
			// set a NumberFormatException is thrown.
			int value = message.getIntProperty("JMSXDeliveryCount");
			if (log.isDebugEnabled()) log.debug("determined delivery count [{}]", value);
			return value;
		} catch (NumberFormatException nfe) {
			if (log.isDebugEnabled()) log.debug("{}NumberFormatException in determination of DeliveryCount", getLogPrefix());
			return -1;
		} catch (Exception e) {
			log.error("{}exception in determination of DeliveryCount", getLogPrefix(), e);
			return -1;
		}
	}

	@Mandatory
	@Override
	public void setDestinationName(String destinationName) {
		super.setDestinationName(destinationName);
	}

	public void setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	/**
	 * Interval <i>in milliseconds</i> for the poll guard to check whether a successful poll was done by the receive
	 * (https://docs.oracle.com/javaee/7/api/javax/jms/messageconsumer.html#receive-long-) since last check. If polling has stopped this will be logged
	 * and the listener will be stopped and started in an attempt to workaround problems with polling.
	 * Polling might stop due to bugs in the JMS driver/implementation which should be fixed by the supplier. As the poll time includes reading
	 * and processing of the message no successful poll might be registered since the last check when message processing takes a long time, hence
	 * while messages are being processed the check on last successful poll will be skipped. Set to -1 to disable.
	 *
	 * @ff.default ten times the specified timeout
	 * */
	public void setPollGuardInterval(long pollGuardInterval) {
		this.pollGuardInterval = pollGuardInterval;
	}

}
