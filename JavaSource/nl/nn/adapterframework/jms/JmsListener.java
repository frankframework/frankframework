/*
 * $Log: JmsListener.java,v $
 * Revision 1.13  2005-01-04 13:16:51  L190409
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.Date;
import java.util.HashMap;

/**
 * A true multi-threaded {@link nl.nn.adapterframework.core.IPullingListener Listener}-class for {@link nl.nn.adapterframework.receivers.JmsReceiver JmsReceiver}.
 * <br/>

 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.JmsMessageReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>"QUEUE" or "TOPIC"</td><td>"QUEUE"</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>when true, the processing joins a transaction set up by the Pipeline or Receiver</td><td>false</td></tr>
 * <tr><td>{@link #setJmsTransacted(boolean) jmsTransacted}</td><td>when true, sessions are explicitly committed (exit-state equals commitOnState) or rolled-back (other exit-states) </td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>"auto", "dups" or "client"</td><td>"auto"</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>Possible values: 'MQ' or 'JMS'. Setting to 'MQ' informs the MQ-server that the replyto queue is not JMS compliant.</td><td>JMS</td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>forces that not the Correlation ID of the received message is used in a reply, but the Message ID. Through the logging you also see the messageID instead of the correlationID.</td><td>false</td></tr>
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
public class JmsListener extends JMSFacade implements IPostboxListener, ICorrelatedPullingListener, HasSender {
	public static final String version="$Id: JmsListener.java,v 1.13 2005-01-04 13:16:51 L190409 Exp $";


  private long timeOut = 3000;
  private boolean useReplyTo=true;
  private ISender sender;
	
  private final static String THREAD_CONTEXT_SESSION_KEY="session";
  private final static String THREAD_CONTEXT_MESSAGECONSUMER_KEY="messageConsumer";
  private boolean forceMessageIdAsCorrelationId=false;
 
  private String commitOnState="success";
  
  /**
   * By default, the JmsListener takes the Correlation ID (if present) as the ID that has to be put in the
   * correlation id of the reply. When you set ForceMessageIdAsCorrelationId to <code>true</code>,
   * the messageID set in the correlationID of the reply.
   * @param force
   */
  public void setForceMessageIdAsCorrelationId(boolean force){
  	 forceMessageIdAsCorrelationId=force;
  }
  public boolean getForceMessageIdAsCorrelationId(){
  	return forceMessageIdAsCorrelationId;
  }
  
public JmsListener() {
	super();
}


public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, HashMap threadContext) throws ListenerException {
    String cid = (String) threadContext.get("cid");

    try {
        Destination replyTo = (Destination) threadContext.get("replyTo");

		// handle reply
        if (getUseReplyTo() && (replyTo != null)) {
			Session session=null;

			log.debug(
                "sending reply message with correlationID["
                    + cid
                    + "], replyTo ["
                    + replyTo.toString()
                    + "]");
            if (threadContext!=null) {
				session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
            }
            if (session==null) { 
            	session=createSession();
				send(session, replyTo, createTextMessage(session, cid, plr.getResult()));
				session.close();            	
            }  else {
				send(session, replyTo, createTextMessage(session, cid, plr.getResult()));
            }
        } else {
			if (sender==null) {
				log.info("["+getName()+"] has no sender, not sending the result.");
			} else {
				if (log.isDebugEnabled()) {
			        log.debug(
		                "["+getName()+"] no replyTo address found or not configured to use replyTo, using default destination" 
		                + "sending message with correlationID[" + cid + "] [" + plr.getResult() + "]");
				}
				sender.sendMessage(cid, plr.getResult());
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
				}
	    	} else {
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

public void open() throws ListenerException {
	try {
		super.open();
	} catch (Exception e) {
		throw new ListenerException("error opening listener [" + getName() + "]", e);
	}

	try {
		if (sender != null)
			sender.open();
	} catch (SenderException e) {
		throw new ListenerException("error opening sender [" + sender.getName() + "]", e);
	}
}

public HashMap openThread() throws ListenerException {
	HashMap threadContext = new HashMap();

	try {
		if (!isTransacted()) { 
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



public void close() throws ListenerException {
    try {
	    super.close();

        if (sender != null) {
            sender.close();
        }
    } catch (Exception e) {
        throw new ListenerException(e);
    }
}
public void closeThread(HashMap threadContext) throws ListenerException {

    try {
		if (!isTransacted()) {
	        MessageConsumer mc = (MessageConsumer) threadContext.remove(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
	        if (mc != null) {
	            mc.close();
	        }
	
	        Session session = (Session) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
	        if (session != null) {
	            session.close();
	        }
		}
    } catch (Exception e) {
        throw new ListenerException("exception in [" + getName() + "]", e);
    }
}

public void configure() throws ConfigurationException {
    ISender sender = getSender();
    if (sender != null) {
        sender.configure();
    }
}

/**
 * Extracts ID-string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return ID-string of message for adapter.
 */
public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
    TextMessage message = null;
    String cid = "unset";
    try {
        message = (TextMessage) rawMessage;
    } catch (ClassCastException e) {
        log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
        return null;
    }
         String mode = "unknown";
        String id = "unset";
        Date dTimeStamp = null;
		Destination replyTo=null;
        try {
            if (message.getJMSDeliveryMode() == DeliveryMode.NON_PERSISTENT) {
                mode = "NON_PERSISTENT";
            } else
                if (message.getJMSDeliveryMode() == DeliveryMode.PERSISTENT) {
                    mode = "PERSISTENT";
                }
        } catch (JMSException ignore) {
	        log.debug("ignoring JMSException in getJMSDeliveryMode()", ignore);
        }
		// --------------------------
		// retrieve MessageID
		// --------------------------
        try {
            id = message.getJMSMessageID();
        } catch (JMSException ignore) {
	        log.debug("ignoring JMSException in getJMSMessageID()", ignore);
        }
		// --------------------------
		// retrieve CorrelationID
		// --------------------------
        try {
        	if (getForceMessageIdAsCorrelationId()){
        		if (log.isDebugEnabled()) log.debug("forcing the messageID to be the correlationID");
				cid =id;
        	}
        	else {
	            cid = message.getJMSCorrelationID();
	            if (cid==null) {
	              cid = id;
	              log.debug("Setting correlation ID to MessageId");
	            }
	        }
        } catch (JMSException ignore) {
	        log.debug("ignoring JMSException in getJMSCorrelationID()", ignore);
        }
		// --------------------------
		// retrieve TimeStamp
		// --------------------------
        try {
            long lTimeStamp = message.getJMSTimestamp();
            dTimeStamp = new Date(lTimeStamp);

        } catch (JMSException ignore) {
	        log.debug("ignoring JMSException in getJMSTimestamp()", ignore);
        }
		// --------------------------
		// retrieve ReplyTo address
		// --------------------------
        try {
            replyTo = message.getJMSReplyTo();

        } catch (JMSException ignore) {
	        log.debug("ignoring JMSException in getJMSReplyTo()", ignore);
        }

        log.info(
            "listener on ["
                + getDestinationName()
                + "] got message with JMSDeliveryMode=["
                + mode
                + "] \n  JMSMessageID=["
                + id
                + "] \n  JMSCorrelationID=["
                + cid
                + "] \n  Timestamp=["
                + dTimeStamp.toString()
                + "] \n  ReplyTo=["
                + ((replyTo==null)?"none" : replyTo.toString())
                + "] \n Message=["
                + message.toString()
                + "]");

        threadContext.put("id",id);
        threadContext.put("cid",cid);
        threadContext.put("timestamp",dTimeStamp);
        threadContext.put("replyTo",replyTo);
        try {
            if (getAckMode() == Session.CLIENT_ACKNOWLEDGE) {
                message.acknowledge();
                log.debug("Listener on [" + getDestinationName() + "] acknowledged message");
            }
        } catch (JMSException exception) {
            log.error("Warning in ack " + exception);
        }
    return cid;
}




/**
 * Retrieves messages from queue or other channel, but does no processing on it.
 */
public Object getRawMessage(HashMap threadContext) throws ListenerException {
	return getRawMessageFromDestination(null, threadContext);
}

public Object getRawMessage(String correlationId, HashMap threadContext) throws ListenerException, TimeOutException {
	Object msg = getRawMessageFromDestination(correlationId, threadContext);
	if (msg==null) {
		throw new TimeOutException("waiting for message with correlationId ["+correlationId+"]");
	}
	return msg;
}

/**
 * Retrieves messages from queue or other channel under transaction control, but does no processing on it.
 */
private Object getRawMessageFromDestination(String correlationId, HashMap threadContext) throws ListenerException {
	Session session;
	MessageConsumer mc;
	try {
		if (!isTransacted() && threadContext!=null ) {
			session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY); 		
			if (threadContext!=null && correlationId==null) {
				mc = (MessageConsumer)threadContext.get(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
			} else {
				mc = getMessageConsumerForCorrelationId(session, getDestination(), correlationId);
			}
		} else {
			session = createSession();
			mc = getMessageConsumerForCorrelationId(session, getDestination(), correlationId);
		}
	} catch (Exception e) {
		throw new ListenerException("["+getName()+"] exception preparing to retrieve message", e);
	}
	Object msg = null;
	try {
		msg = mc.receive(getTimeOut());
		if (isTransacted() || threadContext==null || correlationId!=null) {
			mc.close();
			if (isTransacted() || threadContext==null ) {
				session.close();
			}
		}
	} catch (Exception e) {
		throw new ListenerException("["+getName()+"] exception in retrieving message", e);
	}
	return msg;
}



public ISender getSender() {
	return sender;
}
/**
 * Extracts string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return String  input message for adapter.
 */
public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
    TextMessage message = null;
    try {
        message = (TextMessage) rawMessage;
    } catch (ClassCastException e) {
        log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
        return null;
    }
    try {
    	return message.getText();
    } catch (JMSException e) {
	    throw new ListenerException(e);
    }
}



public void setSender(ISender newSender) {
	sender = newSender;
	    log.debug("["+getName()+"] ** registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    
}


/**
 * Controls when the JmsListener will commit it's local transacted session, that is created when
 * jmsTransacted = <code>true</code>. This is probably not what you want. 
 * @deprecated consider using XA transactions, controled by the <code>transacted</code>-attribute, rather than
 * local transactions controlled by the <code>jmsTransacted</code>-attribute.
 */
public void setCommitOnState(String newCommitOnState) {
	commitOnState = newCommitOnState;
}
public String getCommitOnState() {
	return commitOnState;
}

public void setTimeOut(long newTimeOut) {
	timeOut = newTimeOut;
}
public long getTimeOut() {
	return timeOut;
}

public void setUseReplyTo(boolean newUseReplyTo) {
	useReplyTo = newUseReplyTo;
}
public boolean getUseReplyTo() {
	return useReplyTo;
}

	/** 
	 * @see nl.nn.adapterframework.core.IPostboxListener#retrieveRawMessage(java.lang.String, java.util.HashMap)
	 */
	public Object retrieveRawMessage(String messageSelector, HashMap threadContext) throws ListenerException {
		Session newSession = null, session = null;
		MessageConsumer mc = null;
		try {
			// check to see if session in threadcontext can be reused, otherwise create new
			if (!isTransacted() && threadContext!=null ) {
				session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY); 		
			} 
			else {
				newSession = session = createSession();
			}
			mc = getMessageConsumer(session, getDestination(), messageSelector);
			Object result = (timeOut<0) ? mc.receiveNoWait() : mc.receive(timeOut);
			return result;
		} 
		catch (Exception e) {
			throw new ListenerException("["+getName()+"] exception preparing to retrieve message", e);
		}
		finally {
			if (mc != null) try { mc.close(); } catch(JMSException e) { }
			if (newSession != null) try { newSession.close(); } catch(JMSException e) { }
		}
	}

}
