package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.ISender;

import nl.nn.adapterframework.configuration.ConfigurationException;


import javax.jms.*;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.StringUtils;


/**
 * {@link ISender} that sends a message to an IFSA service and, in case the messageprotocol is RR (Request-Reply)
 * it waits for an reply-message.
 * <br>
 * <p>The property <code>messageProtocol</code>
 * should be set to indicate whether a request/reply or fire &amp; forget
 * service is to be called. When you use this class the property <code>clientName</code> 
 * should be set, the property <code>serverName</code> should not be used (left to null).</p>
 *
 * <p><b>Configuration when used in IfsaClientPipe:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>name of the service to be called</td><td></td></tr>
 * <tr><td>{@link #setClientName(String) clientName}</td><td>name of the client application, on which behalf the service is called</td><td></td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td></td></tr>

 * </table>
 * @author Johan Verrips IOS
 * @version Id
 */
public class IfsaClient extends IfsaFacade implements ISender {
  private QueueSession session;
  private QueueSender sender;
  public static final String version="$Id: IfsaClient.java,v 1.3 2004-06-30 08:31:00 L190409 Exp $";	
/**
 * Stop the sender and deallocate resources.
 */
public void close() throws SenderException {
	String prefix="["+ getName() + "] clientName ["+getClientName()+"]";
    try {
        if (sender != null) {
            sender.close();
        }
        if (session != null) {
            session.close();
        }
        closeService();
    } catch (Throwable e) {
        throw new SenderException(prefix + "got error occured stopping sender", e);
    } finally {
        sender = null;
        session = null;
    }
}
/**
 * This method performs some basic checks. After this
 * method is called the open method may be called.
 */
public void configure() throws ConfigurationException {
    // perform some basic checks
    try {
	    configure(true);
    } catch (IfsaException e) {
	    throw new ConfigurationException("IfsaClient [" + getName() + "] misconfigured", e);
    }
}
/**
 * Retrieves a message with the specified correlationId from queue or other channel, but does no processing on it.
 */
private TextMessage getRawReplyMessage(TextMessage sentMessage) throws SenderException, TimeOutException {

	String correlationID;
    TextMessage msg = null;
	QueueSession replySession;
	QueueReceiver replyReceiver;
    try {
		correlationID = sentMessage.getJMSMessageID();
	    replySession = createSession();
	    replyReceiver = getReplyReceiver(replySession, sentMessage);

	    msg = (TextMessage) replyReceiver.receive(getExpiry());
    } catch (Exception e) {
        throw new SenderException("IfsaClient ["+ getClientName()+ "] got exception retrieving reply", e);
    }
	try {
        replyReceiver.close();
        replySession.close();
    } catch (JMSException e) {
        log.error("error closing replyreceiver or replysession", e);
    }
    if (msg == null) {
        throw new TimeOutException(
            "waiting for reply with correlationID [" + correlationID + "]");
    }
    return msg;
    

}
	public boolean isSynchronous() {
		return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY);
	}
public void open() throws SenderException {
    try {
        openService();
        session = createSession();
        sender = createSender(session, getServiceQueue());

    } catch (Exception e) {
        throw new SenderException(e);
    }
}
/**
 * Send a message to the destination for this service.
 * This method may be called after the <code>init() </code>
 * method is called.
 * <p>As IFSA does not allow setting the correlationID</p>
 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
 */
public String sendMessage(String message)
    throws SenderException, TimeOutException {
    String result = null;
	    
	
	try {
    TextMessage sentMessage;

	synchronized (sender) {
		// TODO: handle UDZs
		sentMessage=sendMessage(session, sender, message, null);
	}
	if (isSynchronous()){

		TextMessage msg=null;
	    msg=getRawReplyMessage(sentMessage);
		result=msg.getText();
			
    }
	} catch (JMSException e) {
		throw new SenderException("Error sending message ["+ message +"]",e);
	} catch (IfsaException e) {
		throw new SenderException("Error sending message ["+ message +"]",e);
	}
    return result;

}
/**
 * Send a message to the destination for this service.
 * This method may be called after the <code>init() </code>
 * method is called.
 * <p>As IFSA does not allow setting the correlationID</p>
 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
 */
public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
    if (StringUtils.isNotEmpty(dummyCorrelationId)) {
	    log.warn("sendMessage() ignoring correlationId ["+dummyCorrelationId+"]");
    }
    return sendMessage(message);
}
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        result += ts.toString();
        return result;

	}
}
