package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.INamedObject;

import com.ing.ifsa.IFSAPoisonMessage;
import com.ing.ifsa.IFSAHeader;
import com.ing.ifsa.IFSAServiceName;
import com.ing.ifsa.IFSATextMessage;

import java.util.HashMap;
import java.util.Date;

import javax.jms.Message;
import javax.jms.QueueSession;
import javax.jms.QueueReceiver;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.TextMessage;
import javax.jms.JMSException;


import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Implementation of {@link IPullingListener} that acts as an IFSA-service.
 *
 * @author Johan Verrips
 * @version Id
 */
public class IfsaServiceListener extends IfsaFacade implements IPullingListener, INamedObject {

    private final static String THREAD_CONTEXT_SESSION_KEY = "session";
    private final static String THREAD_CONTEXT_RECEIVER_KEY = "receiver";
	public static final String version="$Id: IfsaServiceListener.java,v 1.2 2004-03-11 09:23:52 NNVZNL01#L180564 Exp $";

    private String commitOnState;
    private long timeOut = 3000;
public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, HashMap threadContext)
    throws ListenerException {

    String cid = (String) threadContext.get("cid");
    QueueSession session = (QueueSession) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
    
    /* 
     * Message are only committed in the Fire & Forget scenario when the outcome
     * of the adapter equals the getCommitOnResult value
     */
    if (getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET)) {
        if (getCommitOnState().equals(plr.getState())) {
            try {
                session.commit();
            } catch (JMSException e) {
                log.error("[" + getName() + "] got error committing the received message", e);
            }
        } else {
            log.warn(
                "["
                    + getName()
                    + "] message with correlationID ["
                    + cid
                    + " message ["
                    + getStringFromRawMessage(rawMessage, threadContext)
                    + "]"
                    + " is NOT committed. The result-state of the adapter is ["
                    + plr.getState()
                    + "] while the state for committing is set to ["
                    + getCommitOnState()
                    + "]");

        }
    }
    // on request-reply send the reply. On error: halt the listener
    if (getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
        try {
            sendReply(session, (Message) rawMessage, plr.getResult());
        } catch (IfsaException e) {
            log.error("[" + getName() + "] got error sending result", e);
            throw new ListenerException(
                "[" + getName() + "] got error sending result",
                e);
        }
    }

}
public void close() throws ListenerException {
    try {
	    closeService();
    } catch (Exception e) {
        throw new ListenerException(e);
    }
}
public void closeThread(HashMap threadContext) throws ListenerException {

    try {

        QueueReceiver receiver = (QueueReceiver) threadContext.remove(THREAD_CONTEXT_RECEIVER_KEY);
        if (receiver != null) {
            receiver.close();
        }

        QueueSession session = (QueueSession) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
        if (session != null) {
            session.close();
        }
    } catch (Exception e) {
        throw new ListenerException("exception in [" + getName() + "]", e);
    }
}
/**
 * This method performs some basic checks. After this
 * method is called the open method may be called.
 */
public void configure() throws ConfigurationException {
    // perform some basic checks
    try {
        configure(false);
    } catch (IfsaException e) {
	    throw new ConfigurationException("IfsaListener [" + getName() + "] misconfigured", e);
    }
}
public java.lang.String getCommitOnState() {
	return commitOnState;
}
/**
 * Extracts ID-string from message obtained from {@link #getRawMessage(HashMap)}. 
 * Puts also the following parameters  in the threadContext:
 * <ul>
 * <li>id</li>
 * <li>cid</li>
 * <li>timestamp</li>
 * <li>replyTo</li>
 * <li>messageText</li>
 * <li>serviceDestination</li>
 * </ul>
 * @return ID-string of message for adapter.
 */
public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {

	TextMessage message = null;
 
    try {
        message = (TextMessage) rawMessage;
    } catch (ClassCastException e) {
        log.error(
            "message received by receiver ["
                + getName()
                + "] was not of type TextMessage, but ["
                + rawMessage.getClass().getName()
                + "]",
            e);
        return null;
    }
    String mode = "unknown";
    String id = "unset";
    String cid = "unset";
    Date dTimeStamp = null;
    Destination replyTo = null;
    String messageText = null;
    IFSAServiceName ifsaServiceDestination = null;
    String serviceDestination = null;
    try {
        if (message.getJMSDeliveryMode() == DeliveryMode.NON_PERSISTENT) {
            mode = "NON_PERSISTENT";
        } else
            if (message.getJMSDeliveryMode() == DeliveryMode.PERSISTENT) {
                mode = "PERSISTENT";
            }
    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve MessageID
    // --------------------------
    try {
        id = message.getJMSMessageID();
    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve CorrelationID
    // --------------------------
    try {
        cid = message.getJMSCorrelationID();
        if (cid == null) {
            cid = id;
            log.debug("Setting correlation ID to MessageId");
        }
    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve TimeStamp
    // --------------------------
    try {
        long lTimeStamp = message.getJMSTimestamp();
        dTimeStamp = new Date(lTimeStamp);

    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve ReplyTo address
    // --------------------------
    try {
        replyTo = message.getJMSReplyTo();

    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve message text
    // --------------------------
    try {
        messageText = message.getText();
    } catch (JMSException ignore) {
    }
    // --------------------------
    // retrieve ifsaServiceDestination
    // --------------------------
    try {
        ifsaServiceDestination = ((IFSATextMessage) message).getService();
        serviceDestination = ToStringBuilder.reflectionToString(ifsaServiceDestination);
    } catch (JMSException e) {
        log.error("[" + getName() + "] got error getting serviceDestination", e);
    }

    log.info(
        "Receiver ["
            + getName()
            + "] got message with JMSDeliveryMode=["
            + mode
            + "] \n  JMSMessageID=["
            + id
            + "] \n  JMSCorrelationID=["
            + cid
            + "] \n  Timestamp=["
            + dTimeStamp.toString()
            + "] \n  ReplyTo=["
            + ((replyTo == null) ? "none" : replyTo.toString())
            + "] \n Message=["
            + message.toString()
            + "]");

    threadContext.put("id", id);
    threadContext.put("cid", cid);
    threadContext.put("timestamp", dTimeStamp);
    threadContext.put("replyTo", replyTo);
    threadContext.put("messageText", messageText);
    threadContext.put("serviceDestination", serviceDestination);
    return id;
}
/**
 * Retrieves messages to be processed by the server, implementing an IFSA-service, but does no processing on it.
 */
public Object getRawMessage(HashMap threadContext) throws ListenerException {
	Object result;
    try {
	    QueueReceiver receiver = (QueueReceiver)threadContext.get(THREAD_CONTEXT_RECEIVER_KEY);

        result = receiver.receive(getTimeOut());
    } catch (JMSException e) {
        throw new ListenerException(e);
    }
    
    if (result instanceof IFSAPoisonMessage) {
        IFSAHeader header = ((IFSAPoisonMessage) result).getIFSAHeader();
        log.error(
            "["
                + getName()
                + "] received IFSAPoisonMessage "
                + "source ["
                + header.getIFSA_Source()
                + "]"
                + "content ["
                + ToStringBuilder.reflectionToString((IFSAPoisonMessage) result)
                + "]");
    }
    return result;
}
/**
 * Extracts string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
 * other parameters from the message and put those in the threadContext.
 * @return input message for adapter.
 */
public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
    TextMessage message = null;
    try {
        message = (TextMessage) rawMessage;
    } catch (ClassCastException e) {
        log.error("message received by receiver ["+ getName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
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
 * Creation date: (19-01-2004 13:03:51)
 * @return long
 */
public long getTimeOut() {
	return timeOut;
}
public void open() throws ListenerException {
    try {
        openService();
    } catch (IfsaException e) {
        throw new ListenerException("error opening listener [" + getName() + "]", e);
    }
}
public HashMap openThread() throws ListenerException {
	HashMap threadContext = new HashMap();

	try {
	QueueSession session = createSession();
	threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);

	QueueReceiver receiver;
	receiver = getServiceReceiver(session);
	threadContext.put(THREAD_CONTEXT_RECEIVER_KEY, receiver);

	return threadContext;
	} catch (IfsaException e) {
		throw new ListenerException("exception in ["+getName()+"]", e);
	}
}
public void setCommitOnState(java.lang.String newCommitOnState) {
	commitOnState = newCommitOnState;
}
/**
 * Insert the method's description here.
 * Creation date: (19-01-2004 13:03:51)
 * @param newTimeOut long
 */
public void setTimeOut(long newTimeOut) {
	timeOut = newTimeOut;
}
}
