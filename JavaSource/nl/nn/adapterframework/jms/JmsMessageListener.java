package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.receivers.JmsMessageReceiver; // import for documentation...

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;

/**
 * {@link IPullingListener Listener}-class for {@link JmsMessageReceiver JmsMessageReceiver}.
 * <br/>
 
 * The JmsMessageListener registers itself to a Queue or Topic as a listener when 
 * {@link #configure()} is called. <p>

 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsMessageListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 * <p><b>Notice:</b> the JmsMessageListener is ONLY capable of processing
 * <code>javax.jms.TextMessage</code>s <br/><br/>
 * </p>
 *
 * <p>Note: at this moment the state of the adapter is not handled. Only the result
 * from calling the adapter (<code>PipeLineResult.getResult()</code> is retrieved, not
 * <code>PipeLineResult.getState()</code>. Possibilities are: not to acknowledge the received
 * message when the state not equals (bla). This may be implemented in by extending this class
 * and override the method {@link #afterMessageProcessed(PipeLineResult, Object, HashMap) afterMessage}</p>
 * <p>Note 2: the mechanism for receiving is changed to messageReceiver.receive() as from version
 * 3.2.6, as from Websphere 5.0 / j2ee 1.4 setMessageListener is not allowed within a container.</p>
 * <p>As from version 4.0 The receiver will, when a reply-to address is specified and useReplyTo is true, by itself
 * send the answer to the reply-to queue. This makes sure that the JNDI properties are the same.</p>
 *
 * @deprecated This class is deprecated, as it extends the deprecated class {@link JMSBase}. Please use 
 *             {@link JmsListener} instead.
 *
 * @author     Johan Verrips
 * @since 4.0
 */
public class JmsMessageListener extends JMSBase implements ICorrelatedPullingListener, HasSender {
	public static final String version="$Id: JmsMessageListener.java,v 1.1 2004-02-04 08:36:13 a1909356#db2admin Exp $";

	private MessageConsumer mc = null;	
	private long timeOut = 3000;
    private boolean useReplyTo=true;
    private ISender sender;

public JmsMessageListener() {
	super();
	log.warn("Deprecated class JmsMessageListener is used. Use JmsListener instead");
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

        if (getUseReplyTo() && (replyTo != null)) {
            log.debug(
                "sending reply message with correlationID["
                    + cid
                    + "], replyTo ["
                    + replyTo.toString()
                    + "]");
            send(replyTo, createTextMessage(cid, plr.getResult()));
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
    } catch (Exception e) {
        throw new ListenerException(e);
    }
    // could implement committing here....
}
public void close() throws ListenerException {
    try {
        closeReceiver();
        closeConnection();
        reset();
        if (sender!=null)   sender.close();
    } catch (Exception e) {
        throw new ListenerException(e);
    }
}
public void closeThread(HashMap threadContext) throws ListenerException {
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

	TextMessage msg=null; 
    for (long teller = timeOut >> 7; msg == null && teller >= 0; teller--) {

	    msg = receiveMessageByCorrelationId(correlationId);
    }
    if (msg==null) {
    	throw new TimeOutException("waiting for message with correlationId ["+correlationId+"]");
    }
    return msg;
    
}
/**
 * Retrieves messages from queue or other channel, but does no processing on it.
 */
public synchronized Object getRawMessage(HashMap threadContext) throws ListenerException {
    try {
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
        startConnection();
	    mc = getReceiver();
  	    try {
		    if (sender!=null) sender.open();
	    } catch (SenderException e) {
		    throw new SenderException("error opening sender ["+sender.getName()+"]",e);
	    }

	} catch (Exception e) {
		throw new ListenerException(e);
	}
}
public HashMap openThread() throws ListenerException {
	return null;
}
private synchronized TextMessage receiveMessageByCorrelationId(String correlationId)
    throws ListenerException {

    try {
        closeReceiver();
        setSelector("JMSCorrelationID='" + correlationId + "'");
        return (TextMessage) getReceiver().receive(100);
    } catch (Exception e) {
        throw new ListenerException("getting message for correlationId [" + correlationId + "]", e);
    }
}
public void setSender(ISender newSender) {
	sender = newSender;
	    log.debug("["+getName()+"] ** registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    
}
/**
 * Insert the method's description here.
 * Creation date: (24-11-2003 17:54:35)
 * @param newTimeOut long
 */
public void setTimeOut(long newTimeOut) {
	timeOut = newTimeOut;
}
public void setUseReplyTo(boolean newUseReplyTo) {
	useReplyTo = newUseReplyTo;
}
}
