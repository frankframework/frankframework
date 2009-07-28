/*
 * $Log: PullingJmsListener.java,v $
 * Revision 1.6  2009-07-28 12:44:24  L190409
 * enable SOAP over JMS
 *
 * Revision 1.5  2008/09/01 15:13:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made common baseclass for pushing and pulling jms listeners
 *
 * Revision 1.4  2008/02/28 16:23:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use PipeLineSession.setListenerParameters()
 *
 * Revision 1.3  2007/12/17 08:56:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved documentation
 *
 * Revision 1.2  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.27  2007/10/03 08:30:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.26  2007/09/27 12:53:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * clarified warning
 *
 * Revision 1.25  2007/05/23 09:15:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error message
 *
 * Revision 1.24  2006/10/13 08:15:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.23  2006/03/08 13:56:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * getRawMessage now returns null again if no message received if transacted, 
 * to avoid transaction time out
 *
 * Revision 1.22  2006/01/05 14:30:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.21  2005/12/28 08:46:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.20  2005/12/20 16:59:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.19  2005/10/27 08:47:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced RunStateEnquiries
 *
 * Revision 1.18  2005/10/26 08:23:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make use of isSessionsArePooled
 *
 * Revision 1.17  2005/10/24 15:16:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented session pooling
 *
 * Revision 1.16  2005/10/20 15:44:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JMS-classes to use shared connections
 * open()/close() became openFacade()/closeFacade()
 *
 * Revision 1.15  2005/08/02 07:13:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.14  2005/08/02 06:51:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * deliveryMode to String and vv
 * method to send to (reply) destination with msgtype, priority and timetolive
 * reformatted code
 *
 * Revision 1.13  2005/01/04 13:16:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.12  2004/05/21 10:47:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox retriever implementation
 *
 * Revision 1.11  2004/05/03 07:11:50  Johan Verrips <johan.verrips@ibissource.org>
 * Updated message selector behaviour
 *
 * Revision 1.10  2004/03/31 15:01:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.9  2004/03/31 12:04:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.8  2004/03/30 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2004/03/26 11:01:43  Johan Verrips <johan.verrips@ibissource.org>
 * added forceMessageIdAsCorrelationId
 *
 * Revision 1.6  2004/03/26 10:42:55  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.5  2004/03/24 08:26:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabled XA transactions
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.StringUtils;

/**
 * A true multi-threaded {@link nl.nn.adapterframework.core.IPullingListener Listener}-class for {@link nl.nn.adapterframework.receivers.JmsReceiver JmsReceiver}.
 * <br/>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jms.JmsListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>when true, the processing joins a transaction set up by the Pipeline or Receiver</td><td>false</td></tr>
 * <tr><td>{@link #setJmsTransacted(boolean) jmsTransacted}</td><td>when true, sessions are explicitly committed (exit-state equals commitOnState) or rolled-back (other exit-states) </td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>"auto", "dups" or "client"</td><td>"auto"</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setReplyMessageTimeToLive(long) replyMessageTimeToLive}</td><td>time that replymessage will live</td><td>0 [ms]</td></tr>
 * <tr><td>{@link #setReplyMessageType(String) replyMessageType}</td><td>value of the JMSType field of the reply message</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyDeliveryMode(String) replyDeliveryMode}</td><td>controls mode that reply messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyPriority(int) replyPriority}</td><td>sets the priority that is used to deliver the reply message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>Possible values: 'MQ' or 'JMS'. Setting to 'MQ' informs the MQ-server that the replyto queue is not JMS compliant.</td><td>JMS</td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>
 * forces that the CorrelationId that is received is ignored and replaced by the messageId that is received. Use this to create a new, globally unique correlationId to be used downstream. It also
 * forces that not the Correlation ID of the received message is used in a reply as CorrelationId, but the MessageId.</td><td>false</td></tr>
 * </table>
 *</p><p><b>Using transactions</b><br/>
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
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.0.1
 */
public class PullingJmsListener extends JmsListenerBase implements IPostboxListener, ICorrelatedPullingListener, HasSender, RunStateEnquiring {
	public static final String version="$RCSfile: PullingJmsListener.java,v $ $Revision: 1.6 $ $Date: 2009-07-28 12:44:24 $";

	private final static String THREAD_CONTEXT_SESSION_KEY="session";
	private final static String THREAD_CONTEXT_MESSAGECONSUMER_KEY="messageConsumer";
	private RunStateEnquirer runStateEnquirer=null;
	
  

	protected Session getSession(Map threadContext) throws ListenerException {
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

	protected MessageConsumer getReceiver(Map threadContext, Session session, String correlationId) throws ListenerException {
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

	public Map openThread() throws ListenerException {
		Map threadContext = new HashMap();
	
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
	
	
	public void closeThread(Map threadContext) throws ListenerException {
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




	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		String cid = (String) threadContext.get(PipeLineSession.technicalCorrelationIdKey);

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"in PullingJmsListener.afterMessageProcessed()");
	
		try {
			Destination replyTo = (Destination) threadContext.get("replyTo");
	
			// handle reply
			if (isUseReplyTo() && (replyTo != null)) {
				Session session=null;
				
	
				log.debug(getLogPrefix()+"sending reply message with correlationID [" + cid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				if (timeToLive == 0) {
					Message messageSent=(Message)rawMessage;
					long expiration=messageSent.getJMSExpiration();
					if (expiration!=0) {
						timeToLive=expiration-new Date().getTime();
						if (timeToLive<=0) {
							log.warn(getLogPrefix()+"message ["+cid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
							timeToLive=1000;
						}
					}
				}
				if (threadContext!=null) {
					session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
				}
				if (session==null) { 
					try {
						session=getSession(threadContext);
						send(session, replyTo, cid, prepareReply(plr.getResult(),threadContext), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority());
					} finally {
						releaseSession(session);					 
					}
				}  else {
					send(session, replyTo, cid, plr.getResult(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority()); 
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
					getSender().sendMessage(cid, plr.getResult());
				}
			}
	        
			// handle transaction details
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
						((TextMessage)rawMessage).acknowledge();
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
	public Object getRawMessage(Map threadContext) throws ListenerException {
		return getRawMessageFromDestination(null, threadContext);
	}
	
	public Object getRawMessage(String correlationId, Map threadContext) throws ListenerException, TimeOutException {
		Object msg = getRawMessageFromDestination(correlationId, threadContext);
		if (msg==null) {
			throw new TimeOutException("waiting for message with correlationId ["+correlationId+"]");
		}
		if (log.isDebugEnabled()) {
			log.debug("JmsListener ["+getName()+"] received for correlationId ["+correlationId+"] replymessage ["+msg+"]");
		}
		return msg;
	}


	private boolean sessionNeedsToBeSavedForAfterProcessMessage(Object result)
	{
		return isJmsTransacted() &&
				!isTransacted() && 
				isSessionsArePooled()&&
				result != null;
	}

	/**
	 * Retrieves messages from queue or other channel under transaction control, but does no processing on it.
	 */
	private Object getRawMessageFromDestination(String correlationId, Map threadContext) throws ListenerException {
		Session session=null;
		Object msg = null;
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
	 * @see nl.nn.adapterframework.core.IPostboxListener#retrieveRawMessage(java.lang.String, java.util.Map)
	 */
	public Object retrieveRawMessage(String messageSelector, Map threadContext) throws ListenerException {
		Session session=null;
		try {
			session = getSession(threadContext);
			MessageConsumer mc=null;
			try {
				mc = getMessageConsumer(session, getDestination(), messageSelector);
				Object result = (getTimeOut()<0) ? mc.receiveNoWait() : mc.receive(getTimeOut());
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

	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}





}
