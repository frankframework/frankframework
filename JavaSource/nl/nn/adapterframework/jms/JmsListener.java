package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.receivers.JmsReceiver;

import org.apache.commons.lang.StringUtils;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;

/**
 * A true multi-threaded {@link nl.nn.adapterframework.core.IPullingListener Listener}-class for {@link JmsReceiver JmsReceiver}.
 * <br/>

 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.JmsMessageReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) listener.destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) listener.destinationType}</td><td>"QUEUE" or "TOPIC"</td><td>"QUEUE"</td></tr>
 * <tr><td>{@link #setPersistent(String) listener.persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) listener.acknowledgeMode}</td><td>"auto", "dups" or "client"</td><td>"auto"</td></tr>
 * <tr><td>{@link #setTransacted(boolean) listener.transacted}</td><td>when true, sessions are explicitly committed (exit-state equals commitOnState) or rolled-back (other exit-states) </td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) listener.commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link #setTimeOut(long) listener.timeOut}</td><td>receiver timeout, in milliseconds</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) listener.useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) listener.jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 *</p><p><b>Using transacted() and acknowledgement</b><br/>
 * If transacted() is true: it should ensure that a message is received and processed on a both or nothing basis. IBIS will commit
 * the the message, otherwise perform rollback. However, IBIS does not bring transactions within the adapters under transaction
 * control, compromising the idea of atomic transactions. If the Adapter returns a "success" state, the message will be acknowledged
 * or rolled back. In the roll-back situation messages sent to other destinations within the Pipeline are NOT rolled back! In the 
 * failure situation the message is therefore completely processed, and the roll back does not mean that the processing is rolled back!</p>
 *<p>
 * Setting {@link #setAcknowledgeMode(String) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the defined state for committing (specified by {@link #setCommitOnState(String) listener.commitOnState}).
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages. In cases where the client
 * is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode, since a session has lower overhead in trying to
 * prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(String) listener.acknowledgeMode} will only be processed if 
 * the setting for {@link #setTransacted(boolean) listener.transacted} is false.</p>
 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * <code>javax.jms.TextMessage</code>s <br/><br/>
 * </p>
 * <p>$Id: JmsListener.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $</p>
 * @author Gerrit van Brakel
 * @since 4.0.1
 */
public class JmsListener extends JMSFacade implements ICorrelatedPullingListener, HasSender {
	public static final String version="$Id: JmsListener.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $";


  private long timeOut = 3000;
  private boolean useReplyTo=true;
  private ISender sender;
	
  private final static String THREAD_CONTEXT_SESSION_KEY="session";
  private final static String THREAD_CONTEXT_MESSAGECONSUMER_KEY="messageConsumer";
 
  private String commitOnState="success";
public JmsListener() {
	super();
}
/**
 * in the case of a reply to address, the receiver sends the message. Otherwise the sending is delegated
 * to the <code>sender</code> object.
 */
public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, HashMap threadContext)
    throws ListenerException {

    String cid = (String) threadContext.get("cid");

    try {
        Destination replyTo = (Destination) threadContext.get("replyTo");
		Session session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);

        if (getUseReplyTo() && (replyTo != null)) {

			log.debug(
                "sending reply message with correlationID["
                    + cid
                    + "], replyTo ["
                    + replyTo.toString()
                    + "]");
            send(session, replyTo, createTextMessage(session, cid, plr.getResult()));
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
   		String successState = getCommitOnState();
	    if (getTransacted()) {

       		if (successState!=null && successState.equals(plr.getState())) {
        		session.commit();
       		} else {
	       		log.warn("Listener ["+getName()+"] message ["+ (String)threadContext.get("id") +"] not committed nor rolled back either");
           		// session.rollback();
       		}
    	} else {
       		if (getAckMode() == session.CLIENT_ACKNOWLEDGE) {
	       		if (successState!=null && successState.equals(plr.getState())) {
        	  		((TextMessage)rawMessage).acknowledge();
	       		}
       		}
    	}
    } catch (Exception e) {
        throw new ListenerException(e);
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

        MessageConsumer mc = (MessageConsumer) threadContext.remove(THREAD_CONTEXT_MESSAGECONSUMER_KEY);
        if (mc != null) {
            mc.close();
        }

        Session session = (Session) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
        if (session != null) {
            session.close();
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
public String getCommitOnState() {
	return commitOnState;
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
            cid = message.getJMSCorrelationID();
            if (cid==null) {
              cid = id;
              log.debug("Setting correlation ID to MessageId");
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
 * Retrieves a message with the specified correlationId from queue or other channel, but does no processing on it.
 */
public Object getRawMessage(String correlationId,HashMap threadContext) throws ListenerException, TimeOutException {
	
	TextMessage msg = null;
	
	try {
		Session session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
		MessageConsumer mc = getMessageConsumer(session, getDestination(), "JMSCorrelationID='" + correlationId + "'");

		msg = (TextMessage)mc.receive(timeOut);
		mc.close();
	} catch (Exception e) {
		throw new ListenerException("exception while waiting for message with correlationId ["+correlationId+"]");
    }
	if (msg==null) {
  	  	throw new TimeOutException("waiting for message with correlationId ["+correlationId+"]");
	}
    return msg;
	  
}
/**
 * Retrieves messages from queue or other channel, but does no processing on it.
 */
public Object getRawMessage(HashMap threadContext) throws ListenerException {
    try {
	    MessageConsumer mc = (MessageConsumer)threadContext.get(THREAD_CONTEXT_MESSAGECONSUMER_KEY);

	    return mc.receive(timeOut);
    } catch (JMSException e) {
        throw new ListenerException(e);
    }
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
/**
 * Insert the method's description here.
 * Creation date: (24-11-2003 17:54:35)
 * @return long
 */
public long getTimeOut() {
	return timeOut;
}
public boolean getUseReplyTo() {
	return useReplyTo;
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
	Session session = createSession();
	threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);

	MessageConsumer mc = getMessageConsumer(session, getDestination(), null);
	threadContext.put(THREAD_CONTEXT_MESSAGECONSUMER_KEY, mc);

	return threadContext;
	} catch (Exception e) {
		throw new ListenerException("exception in ["+getName()+"]", e);
	}
}
public void setCommitOnState(String newCommitOnState) {
	commitOnState = newCommitOnState;
}
public void setSender(ISender newSender) {
	sender = newSender;
	    log.debug("["+getName()+"] ** registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    
}
public void setTimeOut(long newTimeOut) {
	timeOut = newTimeOut;
}
public void setUseReplyTo(boolean newUseReplyTo) {
	useReplyTo = newUseReplyTo;
}
}
