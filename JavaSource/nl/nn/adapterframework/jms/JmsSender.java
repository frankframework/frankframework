/*
 * $Log: JmsSender.java,v $
 * Revision 1.3  2004-03-23 18:22:39  L190409
 * enabled Transaction control
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.jms.Session;
import javax.jms.MessageProducer;
import javax.jms.Message;

/**
 * This class sends messages with JMS.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.JmsMessageReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) listener.destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) listener.destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPersistent(String) listener.persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) listener.acknowledgeMode}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) listener.transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) listener.commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link #setReplyToName(String) listener.ReplyToName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) listener.jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p>$Id: JmsSender.java,v 1.3 2004-03-23 18:22:39 L190409 Exp $</p>
 *
 * @author Gerrit van Brakel
 */

public class JmsSender extends JMSFacade implements ISender {
	public static final String version="$Id: JmsSender.java,v 1.3 2004-03-23 18:22:39 L190409 Exp $";

  private String replyToName=null;

  private Session session;
  private MessageProducer messageProducer;
	public JmsSender() {
		super();
	}


	public void open() throws SenderException {
		try {
			super.open();
			/*
			if (!isTransacted()) {
				session = createSession();
				messageProducer = getMessageProducer(session, getDestination());
			}
			*/
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	
/**
 * Stops the sender and resets all dynamic data.
 */
public void close() throws SenderException {
    String prefix = "JmsMessageSender [" + getName() + "] ";
    try {
        if (messageProducer != null) {
            messageProducer.close();
        }
        if (session != null) {
            session.close();
        }
        super.close();
    } catch (Throwable e) {
        throw new SenderException(prefix + "got error occured stopping sender", e);
    } finally {
        messageProducer = null;
        session = null;
    }
}
public void configure() throws ConfigurationException {

}
    public String getReplyTo(){
        return replyToName;
    }
    public boolean isSynchronous() {
	    return false;
    }
/*
public synchronized String sendMessage(String correlationID, String message) throws SenderException {
	return sendMessage(correlationID, message, session, messageProducer);
}
*/
public String sendMessage(String correlationID, String message) throws SenderException{
	//if (isTransacted()) {
		try {
			Session s = createSession();
			MessageProducer mp = getMessageProducer(s, getDestination());

			String result = sendMessage(correlationID, message, s, mp);
			
			mp.close();
			s.close();
			return result; 
//		} else {
//			String result = sendMessage(correlationID, message, session, messageProducer);
//		}
	} catch (Exception e) {
		throw new SenderException("JmsSender ["+ getName()+ "] got exception sending message", e);
	}
}
/**
 * Sends the message
 * @returns the messageId of the message
 */
public String sendMessage(String correlationID, String message, Session s, MessageProducer mp)
    throws SenderException {

    try {
        Message msg = createTextMessage(s, correlationID, message);
        if (null != replyToName) {
            msg.setJMSReplyTo(getDestination(replyToName));
            log.debug("replyTo set to [" + msg.getJMSReplyTo().toString() + "]");
        }
        send(mp, msg);
        log.info(
            "[" + getName() + "] " +
            "sent Message: [" + message + "] " +
            "to [" + getDestinationName() + "] " +
            "msgID ["+ msg.getJMSMessageID() + "] " +
            "correlationID ["+ msg.getJMSCorrelationID() + "] " +
            "using "+ (getPersistent() ? "persistent" : "non-persistent") + " mode " +
            ((replyToName != null) ? "replyTo:" + replyToName : ""));
        return msg.getJMSMessageID();
    } catch (Throwable e) {
        log.error(
            "JmsSender ["
                + getName()
                + "] got exception: "
                + ToStringBuilder.reflectionToString(e),
            e);
        throw new SenderException(e);
    }

}
    public void setReplyToName(String replyTo){
        this.replyToName=replyTo;
    }
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        ts.append("version", version);
        ts.append("replyToName", replyToName);
        result += ts.toString();
        return result;

	}
}
