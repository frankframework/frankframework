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
 * <p>$Id: JmsSender.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $</p>
 *
 * @author Gerrit van Brakel
 */

public class JmsSender extends JMSFacade implements ISender {
	public static final String version="$Id: JmsSender.java,v 1.2 2004-02-04 10:02:07 a1909356#db2admin Exp $";

  private String replyToName=null;

  private Session session;
  private MessageProducer messageProducer;
	public JmsSender() {
		super();
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
public void open() throws SenderException {
    try {
	    super.open();
        session = createSession();
        messageProducer = getMessageProducer(session, getDestination());

    } catch (Exception e) {
        throw new SenderException(e);
    }
}
public synchronized String sendMessage(String correlationID, String message)
    throws SenderException {

    try {

        Message msg = createTextMessage(session, correlationID, message);
        if (null != replyToName) {
            msg.setJMSReplyTo(getDestination(replyToName));
            log.debug("replyTo set to [" + msg.getJMSReplyTo().toString() + "]");
        }
        send(messageProducer, msg);
        log.info(
            "["
                + getName()
                + "] sent Message: ["
                + message
                + "] to ["
                + this.getDestinationName()
                + "] correlationID["
                + correlationID
                + "] using "
                + (getPersistent() ? "persistent" : "non-persistent")
                + " mode"
                + ((replyToName != null) ? "replyTo:" + replyToName : "")
                + "");
        return null;
    } catch (Throwable e) {
        log.error(
            "JmsMessageSender ["
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
