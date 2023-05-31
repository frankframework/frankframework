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
package nl.nn.adapterframework.jms;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.Message;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IListenerConnector.CacheMode;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.CredentialFactory;

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
 * Setting {@link #setAcknowledgeMode(String) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the success state.
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages.
 * In cases where the client is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode,
 * since a session has lower overhead in trying to prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(String) listener.acknowledgeMode} will only be processed if
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
 * <code>javax.jms.TextMessage</code>s <br/><br/>
 * </p>
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PushingJmsListener extends JmsListenerBase implements IPortConnectedListener<javax.jms.Message>, IThreadCountControllable, IKnowsDeliveryCount<javax.jms.Message> {

	private @Getter CacheMode cacheMode;
	private @Getter long pollGuardInterval = Long.MIN_VALUE;

	private @Getter @Setter IListenerConnector<javax.jms.Message> jmsConnector;
	private @Getter @Setter IMessageHandler<javax.jms.Message> handler;
	private @Getter @Setter Receiver<javax.jms.Message> receiver;
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
			setPollGuardInterval(getTimeOut() * 10);
		}
		if (getPollGuardInterval() <= getTimeOut()) {
			ConfigurationWarnings.add(this, log, "The pollGuardInterval ["+getPollGuardInterval()+"] should be larger than the receive timeout ["+getTimeOut()+"]");
		}
		CredentialFactory credentialFactory=null;
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			credentialFactory=new CredentialFactory(getAuthAlias());
		}
		try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), credentialFactory,
					destination, getExceptionListener(), getCacheMode(), getAcknowledgeModeEnum().getAcknowledgeMode(),
					isJmsTransacted(), getMessageSelector(), getTimeOut(), getPollGuardInterval());
		} catch (JmsException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		jmsConnector.start();
	}

	@Override
	public void close() {
		try {
			jmsConnector.stop();
		} catch (Exception e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		} finally {
			super.close();
		}
	}


	@Override
	public IListenerConnector<javax.jms.Message> getListenerPortConnector() {
		return jmsConnector;
	}

	@Override
	public RawMessageWrapper<Message> wrapRawMessage(Message rawMessage, Map<String, Object> threadContext) throws ListenerException {
		populateContextFromMessage(rawMessage, threadContext);
		String id = (String) threadContext.get(PipeLineSession.messageIdKey);
		String cid = (String) threadContext.get(PipeLineSession.correlationIdKey);

		return new RawMessageWrapper<>(rawMessage, id, cid);
	}

	@Override
	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountReadable();
		}
		return false;
	}

	@Override
	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountControllable();
		}
		return false;
	}

	@Override
	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.decreaseThreadCount();
		}
	}

	@Override
	public int getDeliveryCount(RawMessageWrapper<Message> rawMessage) {
		try {
			javax.jms.Message message=rawMessage.getRawMessage();
			// Note: Tibco doesn't set the JMSXDeliveryCount for messages
			// delivered for the first time (when JMSRedelivered is set to
			// false). Hence when set is has a value of 2 or higher. When not
			// set a NumberFormatException is thrown.
			int value = message.getIntProperty("JMSXDeliveryCount");
			if (log.isDebugEnabled()) log.debug("determined delivery count ["+value+"]");
			return value;
		} catch (NumberFormatException nfe) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"NumberFormatException in determination of DeliveryCount");
			return -1;
		} catch (Exception e) {
			log.error(getLogPrefix()+"exception in determination of DeliveryCount", e);
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
