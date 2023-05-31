/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;

/**
 * A true multi-threaded {@link IPullingListener Listener}-class.
 * <br/>
 *
 * Since version 4.1, Ibis supports distributed transactions using the XA-protocol. This feature is controlled by the
 * {@link #setTransacted(boolean) transacted} attribute. If this is set to <code>true</code>, received messages are
 * committed or rolled back, possibly together with other actions, by the receiver or the pipeline.
 * In case of a failure, all actions within the transaction are rolled back.
 *
 * <p><b>Using jmsTransacted and acknowledgement</b><br/>
 * If jmsTransacted is set <code>true</code>: it should ensure that a message is received and processed on a both or nothing basis.
 * IBIS will commit the the message, otherwise perform rollback. However using jmsTransacted, IBIS does not bring transactions within
 * the adapters under transaction control, compromising the idea of atomic transactions. In the roll-back situation messages sent to
 * other destinations within the Pipeline are NOT rolled back if jmsTransacted is set <code>true</code>! In the failure situation the
 * message is therefore completely processed, and the roll back does not mean that the processing is rolled back! To obtain the correct
 * (transactional) behaviour, {@link #setTransacted(boolean) transacted} should be used instead of {@link #setJmsTransacted(boolean)
 * listener.transacted}.
 *<p>
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
 * </p>
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * <code>javax.jms.TextMessage</code>s <br/><br/>
 * </p>
 * @author Gerrit van Brakel
 * @since 4.0.1
 */
public class PullingJmsListener extends JmsListenerBase implements IPostboxListener<javax.jms.Message>, ICorrelatedPullingListener<javax.jms.Message>, HasSender, RunStateEnquiring {

	private static final String THREAD_CONTEXT_MESSAGECONSUMER_KEY="messageConsumer";
	private RunStateEnquirer runStateEnquirer=null;

	public PullingJmsListener() {
		setTimeout(20000);
	}

	protected Session getSession(Map<String,Object> threadContext) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return createSession();
			} catch (JmsException e) {
				throw new ListenerException("exception creating QueueSession", e);
			}
		}
		return (Session) threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
	}

	protected void releaseSession(Session session) throws ListenerException {
		if (isSessionsArePooled()) {
			closeSession(session);
		}
	}

	protected MessageConsumer getReceiver(Map<String,Object> threadContext, Session session, String correlationId) throws ListenerException {
		try {
			if (StringUtils.isNotEmpty(correlationId)) {
				return getMessageConsumerForCorrelationId(session, getDestination(), correlationId);
			}
			if (isSessionsArePooled()) {
				return getMessageConsumer(session, getDestination());
			}
			return (MessageConsumer) threadContext.get(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"exception creating QueueReceiver for "+getPhysicalDestinationName(), e);
		}
	}

	protected void releaseReceiver(MessageConsumer receiver, String correlationId) throws ListenerException {
		if ((isSessionsArePooled() || StringUtils.isNotEmpty(correlationId)) && receiver != null) {
			try {
				receiver.close();
				// do not write to log, this occurs too often
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"exception closing QueueReceiver", e);
			}
		}
	}

	@Nonnull
	@Override
	public Map<String,Object> openThread() throws ListenerException {
		Map<String,Object> threadContext = new HashMap<>();

		try {
			if (!isSessionsArePooled()) {
				Session session = createSession();
				threadContext.put(IListenerConnector.THREAD_CONTEXT_SESSION_KEY, session);

				MessageConsumer mc = getMessageConsumer(session, getDestination());
				threadContext.put(THREAD_CONTEXT_MESSAGECONSUMER_KEY, mc);
			}
			return threadContext;
		} catch (Exception e) {
			throw new ListenerException("exception in ["+getName()+"]", e);
		}
	}


	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		try {
			if (!isSessionsArePooled()) {
				MessageConsumer mc = (MessageConsumer) threadContext.remove(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
				releaseReceiver(mc,null);

				Session session = (Session) threadContext.remove(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
				closeSession(session);
			}
		} catch (Exception e) {
			throw new ListenerException("exception in [" + getName() + "]", e);
		}
	}


	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessage, Map<String, Object> threadContext) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessage, threadContext);
		if (!isTransacted() && isJmsTransacted() && isSessionsArePooled()) {
			Session session = (Session)threadContext.remove(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
			if (session!=null) {
				releaseSession(session);
			}
		}
	}


	@Override
	protected void sendReply(PipeLineResult plr, Destination replyTo, String replyCid, long timeToLive, boolean ignoreInvalidDestinationException, Map<String, Object> threadContext, Map<String, Object> properties) throws SenderException, ListenerException, NamingException, JMSException, IOException {
		Session session = (Session)threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
		if (session==null) {
			try {
				session=getSession(threadContext);
				send(session, replyTo, replyCid, prepareReply(plr.getResult(),threadContext), getReplyMessageType(), timeToLive, getReplyDeliveryMode().getDeliveryMode(), getReplyPriority(), ignoreInvalidDestinationException, properties);
			} finally {
				releaseSession(session);
			}
		} else {
			send(session, replyTo, replyCid, plr.getResult(), getReplyMessageType(), timeToLive, getReplyDeliveryMode().getDeliveryMode(), getReplyPriority(), ignoreInvalidDestinationException, properties);
		}
	}



	/**
     * Retrieves messages from queue or other channel, but does no processing on it.
     */
	@Override
	public RawMessageWrapper<Message> getRawMessage(@Nonnull @Nonnull Map<String, Object> threadContext) throws ListenerException {
		return getRawMessageFromDestination(null, threadContext);
	}

	@Override
	public RawMessageWrapper<javax.jms.Message> getRawMessage(String correlationId, Map<String,Object> threadContext) throws ListenerException, TimeoutException {
		RawMessageWrapper<javax.jms.Message> msg = getRawMessageFromDestination(correlationId, threadContext);
		if (msg==null) {
			throw new TimeoutException(getLogPrefix()+" timed out waiting for message with correlationId ["+correlationId+"]");
		}
		if (log.isDebugEnabled()) {
			log.debug("JmsListener ["+getName()+"] received for correlationId ["+correlationId+"] replymessage ["+msg+"]");
		}
		return msg;
	}


	private boolean sessionNeedsToBeSavedForAfterProcessMessage(Object result) {
		return isJmsTransacted() &&
				!isTransacted() &&
				isSessionsArePooled()&&
				result != null;
	}

	/**
	 * Retrieves messages from queue or other channel under transaction control, but does no processing on it.
	 */
	private RawMessageWrapper<javax.jms.Message> getRawMessageFromDestination(String correlationId, Map<String,Object> threadContext) throws ListenerException {
		Session session=null;
		javax.jms.Message msg = null;
		String messageId = null;
		checkTransactionManagerValidity();
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getReceiver(threadContext,session,correlationId);
				msg = mc.receive(getTimeOut());
				while (msg==null && correlationId==null && canGoOn() && !isTransacted()) {
					msg = mc.receive(getTimeOut());
				}
				if (msg == null) {
					return null;
				}
				messageId = msg.getJMSMessageID();
				return new RawMessageWrapper<>(msg, messageId, correlationId == null ? msg.getJMSCorrelationID() : correlationId);
			} catch (JMSException e) {
				throw new ListenerException(getLogPrefix()+"exception retrieving message",e);
			} finally {
				releaseReceiver(mc,correlationId);
			}
		} finally {
			if (sessionNeedsToBeSavedForAfterProcessMessage(msg)) {
				threadContext.put(IListenerConnector.THREAD_CONTEXT_SESSION_KEY, session);
			} else {
				releaseSession(session);
			}
		}
	}

	/**
	 * @see IPostboxListener#retrieveRawMessage(String, Map)
	 */
	@Override
	public RawMessageWrapper<Message> retrieveRawMessage(String messageSelector, Map<String,Object> threadContext) throws ListenerException {
		Session session=null;
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getMessageConsumer(session, getDestination(), messageSelector);
				javax.jms.Message result = (getTimeOut()<0) ? mc.receiveNoWait() : mc.receive(getTimeOut());
				return new RawMessageWrapper<>(result, result.getJMSMessageID(), messageSelector);
			} finally {
				if (mc != null) {
					try {
						mc.close();
					} catch(JMSException e) {
						log.warn(getLogPrefix()+"exception closing messageConsumer",e);
					}
				}
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"exception preparing to retrieve message", e);
		} finally {
			releaseSession(session);
		}
	}


	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.getRunState()==RunState.STARTED;
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}

}
