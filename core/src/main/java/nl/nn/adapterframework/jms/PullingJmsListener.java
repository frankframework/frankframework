/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
//import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;

/**
 * A true multi-threaded {@link nl.nn.adapterframework.core.IPullingListener Listener}-class.
 * <br/>
 *
 * Since version 4.1, Ibis supports distributed transactions using the XA-protocol. This feature is controlled by the 
 * {@link #setTransacted(boolean) transacted} attribute. If this is set to <code>true</code>, received messages are 
 * committed or rolled back, possibly together with other actions, by the receiver or the pipeline.
 * In case of a failure, all actions within the transaction are rolled back.
 * 
 *</p><p><b>Using jmsTransacted and acknowledgement</b><br/>
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
 * when the state of the Adapter equals the defined state for committing (specified by {@link #setCommitOnState(String) listener.commitOnState}).
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
 * @author Gerrit van Brakel
 * @since 4.0.1
 */
public class PullingJmsListener extends JmsListenerBase implements IPostboxListener<javax.jms.Message>, ICorrelatedPullingListener<javax.jms.Message>, HasSender, RunStateEnquiring {

	private final static String THREAD_CONTEXT_SESSION_KEY="session";
	private final static String THREAD_CONTEXT_MESSAGECONSUMER_KEY="messageConsumer";
	private RunStateEnquirer runStateEnquirer=null;
	
	public PullingJmsListener() {  
		setTimeOut(20000);
	}

	protected Session getSession(Map<String,Object> threadContext) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return createSession();
			} catch (JmsException e) {
				throw new ListenerException("exception creating QueueSession", e);
			}
		} else {
			return (Session) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
		}
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
			} else {
				if (isSessionsArePooled()) {
					return getMessageConsumer(session, getDestination());
				} else {
					return (MessageConsumer) threadContext.get(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
				}
			}
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

	@Override
	public Map<String,Object> openThread() throws ListenerException {
		Map<String,Object> threadContext = new HashMap<String,Object>();
	
		try {
			if (!isSessionsArePooled()) { 
				Session session = createSession();
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
			
				MessageConsumer mc = getMessageConsumer(session, getDestination());
				threadContext.put(THREAD_CONTEXT_MESSAGECONSUMER_KEY, mc);
			}
			return threadContext;
		} catch (Exception e) {
			throw new ListenerException("exception in ["+getName()+"]", e);
		}
	}
	
	
	@Override
	public void closeThread(Map<String,Object> threadContext) throws ListenerException {
		try {
			if (!isSessionsArePooled()) {
				MessageConsumer mc = (MessageConsumer) threadContext.remove(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
				releaseReceiver(mc,null);
		
				Session session = (Session) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
				closeSession(session);
			}
		} catch (Exception e) {
			throw new ListenerException("exception in [" + getName() + "]", e);
		}
	}




	@Override
	public void afterMessageProcessed(PipeLineResult plr, Object rawMessageOrWrapper, Map<String,Object> threadContext) throws ListenerException {
		String cid = (String) threadContext.get(IPipeLineSession.technicalCorrelationIdKey);

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"in PullingJmsListener.afterMessageProcessed()");
	
		try {
			Destination replyTo = (Destination) threadContext.get("replyTo");
	
			// handle reply
			if (isUseReplyTo() && (replyTo != null)) {
				Session session=null;
				
	
				log.debug(getLogPrefix()+"sending reply message with correlationID [" + cid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				boolean ignoreInvalidDestinationException = false;
				if (timeToLive == 0) {
					javax.jms.Message messageSent=(javax.jms.Message)rawMessageOrWrapper; // cast can be safely done because javax.jms.Message is serializable, so no MessageWrapper in this case
					long expiration=messageSent.getJMSExpiration();
					if (expiration!=0) {
						timeToLive=expiration-new Date().getTime();
						if (timeToLive<=0) {
							log.warn(getLogPrefix()+"message ["+cid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
							timeToLive=1000;
							// In case of a temporary queue it might already
							// have disappeared.
							ignoreInvalidDestinationException = true;
						}
					}
				}
				if (threadContext!=null) {
					session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
				}
				if (session==null) { 
					try {
						session=getSession(threadContext);
						send(session, replyTo, cid, prepareReply(plr.getResult(),threadContext).asString(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority(), ignoreInvalidDestinationException);
					} finally {
						releaseSession(session);					 
					}
				}  else {
					send(session, replyTo, cid, plr.getResult().asString(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority(), ignoreInvalidDestinationException); 
				}
			} else {
				if (getSender()==null) {
					log.debug(getLogPrefix()+"itself has no sender to send the result (An enclosing Receiver might still have one).");
				} else {
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix()+
							"no replyTo address found or not configured to use replyTo, using default destination" 
							+ "sending message with correlationID[" + cid + "] [" + plr.getResult() + "]");
					}
					getSender().sendMessage(plr.getResult(), null);
				}
			}

			// TODO Do we still need this? Should we rollback too? See
			// PushingJmsListener.afterMessageProcessed() too (which does a
			// rollback, but no commit).
			if (!isTransacted()) {
				if (isJmsTransacted()) {
					// the following if transacted using transacted sessions, instead of XA-enabled sessions.
					Session session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
					if (session == null) {
						log.warn("Listener ["+getName()+"] message ["+ (String)threadContext.get("id") +"] has no session to commit or rollback");
					} else {
						String successState = getCommitOnState();
						if (successState!=null && successState.equals(plr.getState())) {
							session.commit();
						} else {
							log.warn("Listener ["+getName()+"] message ["+ (String)threadContext.get("id") +"] not committed nor rolled back either");
							//TODO: enable rollback, or remove support for JmsTransacted altogether (XA-transactions should do it all)
							// session.rollback();
						}
						if (isSessionsArePooled()) {
							threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
							releaseSession(session);
						}
					}
				} else {
					// TODO: dit weghalen. Het hoort hier niet, en zit ook al in getIdFromRawMessage. Daar hoort het ook niet, overigens...
					if (getAckMode() == Session.CLIENT_ACKNOWLEDGE) {
						log.debug("["+getName()+"] acknowledges message with id ["+cid+"]");
						((TextMessage)rawMessageOrWrapper).acknowledge();
					}
				}
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
	
	
	
	/**
	 * Retrieves messages from queue or other channel, but does no processing on it.
	 */
	@Override
	public javax.jms.Message getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		return getRawMessageFromDestination(null, threadContext);
	}
	
	@Override
	public javax.jms.Message getRawMessage(String correlationId, Map<String,Object> threadContext) throws ListenerException, TimeOutException {
		javax.jms.Message msg = getRawMessageFromDestination(correlationId, threadContext);
		if (msg==null) {
			throw new TimeOutException(getLogPrefix()+" timed out waiting for message with correlationId ["+correlationId+"]");
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
	private javax.jms.Message getRawMessageFromDestination(String correlationId, Map<String,Object> threadContext) throws ListenerException {
		Session session=null;
		javax.jms.Message msg = null;
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getReceiver(threadContext,session,correlationId);
				msg = mc.receive(getTimeOut());
				while (msg==null && correlationId==null && canGoOn() && !isTransacted()) {
					msg = mc.receive(getTimeOut());
				}
			} catch (JMSException e) {
				throw new ListenerException(getLogPrefix()+"exception retrieving message",e);
			} finally {
				releaseReceiver(mc,correlationId);
			}
		} finally {
			if (sessionNeedsToBeSavedForAfterProcessMessage(msg)) {
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
			} else {
				releaseSession(session);
			}
		}		
		return msg;
	}

	/** 
	 * @see IPostboxListener#retrieveRawMessage(String, Map)
	 */
	@Override
	public javax.jms.Message retrieveRawMessage(String messageSelector, Map<String,Object> threadContext) throws ListenerException {
		Session session=null;
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getMessageConsumer(session, getDestination(), messageSelector);
				javax.jms.Message result = (getTimeOut()<0) ? mc.receiveNoWait() : mc.receive(getTimeOut());
				return result;
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
		return runStateEnquirer!=null && runStateEnquirer.isInState(RunStateEnum.STARTED);
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}





}
