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
package org.frankframework.jms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.HasSender;
import org.frankframework.core.ICorrelatedPullingListener;
import org.frankframework.core.IListenerConnector;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.RunState;
import org.frankframework.util.RunStateEnquirer;
import org.frankframework.util.RunStateEnquiring;

/**
 * A true multi-threaded {@link IPullingListener Listener}-class.
 * <br/>
 *
 * Since version 4.1, Ibis supports distributed transactions using the XA-protocol. This feature is controlled by the
 * {@link #setTransacted(boolean) transacted} attribute. If this is set to <code>true</code>, received messages are
 * committed or rolled back, possibly together with other actions, by the receiver or the pipeline.
 * In case of a failure, all actions within the transaction are rolled back.
 *<p>
 * Setting {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the success state.
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages.
 * In cases where the client is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode,
 * since a session has lower overhead in trying to prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(AcknowledgeMode) listener.acknowledgeMode} will only be processed if
 * the setting for {@link #setTransacted(boolean) listener.transacted}</p>
 *
 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 * </p>
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * <code>jakarta.jms.TextMessage</code>s <br/><br/>
 * </p>
 *
 * {@inheritDoc}
 *
 * @author Gerrit van Brakel
 * @since 4.0.1
 */
public class PullingJmsListener extends AbstractJmsListener implements IPullingListener<Message>, ICorrelatedPullingListener<Message>, HasSender, RunStateEnquiring {

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

	protected void releaseSession(Session session) {
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
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<Message> rawMessageWrapper, PipeLineSession pipeLineSession) throws ListenerException {
		super.afterMessageProcessed(plr, rawMessageWrapper, pipeLineSession);
		if (!isTransacted() && isSessionsArePooled()) {
			Session queueSession = (Session) pipeLineSession.remove(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
			if (queueSession!=null) {
				releaseSession(queueSession);
			}
		}
	}

	@Override
	protected void sendReply(PipeLineResult plr, Destination replyTo, String replyCid, long timeToLive, boolean ignoreInvalidDestinationException, PipeLineSession pipeLineSession, Map<String, Object> properties) throws SenderException, ListenerException, JMSException, IOException {
		Session session = (Session) pipeLineSession.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);
		if (session==null) {
			try {
				session=getSession(pipeLineSession);
				send(session, replyTo, replyCid, prepareReply(plr.getResult(), pipeLineSession), getReplyMessageType(), timeToLive, getReplyDeliveryMode().getDeliveryMode(), getReplyPriority(), ignoreInvalidDestinationException, properties);
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
	public RawMessageWrapper<Message> getRawMessage(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		return getRawMessageFromDestination(null, threadContext);
	}

	@Override
	public RawMessageWrapper<Message> getRawMessage(String correlationId, Map<String,Object> threadContext) throws ListenerException, TimeoutException {
		RawMessageWrapper<Message> msg = getRawMessageFromDestination(correlationId, threadContext);
		if (msg==null) {
			throw new TimeoutException(getLogPrefix()+" timed out waiting for message with correlationId ["+correlationId+"]");
		}
		if (log.isDebugEnabled()) {
			log.debug("JmsListener [{}] received for correlationId [{}] replymessage [{}]", getName(), correlationId, msg);
		}
		return msg;
	}

	private boolean sessionNeedsToBeSavedForAfterProcessMessage(Object result) {
		return !isTransacted()
				&& isSessionsArePooled()
				&& result != null;
	}

	/**
	 * Retrieves messages from queue or other channel under transaction control, but does no processing on it.
	 */
	private RawMessageWrapper<Message> getRawMessageFromDestination(String correlationId, Map<String,Object> threadContext) throws ListenerException {
		Session session=null;
		Message msg = null;
		String messageId;

		checkTransactionManagerValidity();
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getReceiver(threadContext,session,correlationId);
				msg = mc.receive(getTimeout());
				while (msg==null && correlationId==null && canGoOn() && !isTransacted()) {
					msg = mc.receive(getTimeout());
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

	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.getRunState()==RunState.STARTED;
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}
}
