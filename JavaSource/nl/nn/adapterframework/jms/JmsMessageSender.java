package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.jms.Destination;
import javax.jms.Message;

/**
 * This class sends messages with JMS.
 *
 * @deprecated This class is deprecated, as it extends the deprecated class {@link JMSBase}. Please use 
 *             {@link JmsSender} instead.
 *
 */

public class JmsMessageSender extends JMSBase implements ISender {
	public static final String version="$Id: JmsMessageSender.java,v 1.1 2004-02-04 08:36:15 a1909356#db2admin Exp $";


    private String replyToName=null;

	public JmsMessageSender() {
		super();
		log.warn("Deprecated class JmsMessageSender is used. Use JmsSender instead");
	}
/**
 * Stops the sender and resets all dynamic data.
 */
public void close() throws SenderException {
	String prefix ="JmsMessageSender ["+getName() +"] "; 
	try{
		closeSender();           
		log.debug(prefix+"closed sender on "+getDestinationType()+" ["+getDestinationName()+"]");

		closeSession();
		log.debug(prefix+"closed session on "+getDestinationType()+" ["+getDestinationName() + "]");
		closeConnection();
		log.debug(prefix+"closed connection on "+getDestinationType()+" ["+ getDestinationName()+"]");
	} catch (Throwable e) {
		throw new SenderException(prefix+"got error occured stopping sender", e);
	} finally {
        reset();
	}
}
	public void configure() throws ConfigurationException {
		try {
			log.debug("JmsMessageSender ["+getName() +"] initializing with configuration "+toString());
			this.getMessageSender();
		} catch (Exception e) {
			log.error("JmsMessageSender ["+getName() +"], definition ["+toString()+"] : error occured initializing "+ToStringBuilder.reflectionToString(e), e);
			throw new ConfigurationException ( "["+getName() +"], definition ["+toString()+"] : error occured initializing "+ToStringBuilder.reflectionToString(e)+" got error "+e.getMessage(), e);
		}

	}
    public String getReplyTo(){
        return replyToName;
    }
    public boolean isSynchronous() {
	    return false;
    }
public void open() {}
	public synchronized void sendMessage(String message) throws SenderException{
		sendMessage(null, message);
	}
public synchronized String sendMessage(String correlationID, String message) throws SenderException {

try{

        Message msg=createTextMessage(correlationID, message);
        if (null!=replyToName){
            msg.setJMSReplyTo(getDestination(replyToName));
            log.debug("replyTo set to ["+msg.getJMSReplyTo().toString()+"]");
        }
        send(msg);
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
                + ((replyToName!=null) ? "replyTo:"+replyToName:"")
	            + "");
        return null;
} catch (Throwable e) {
    log.error("JmsMessageSender [" + getName()  + "] got exception: "+ToStringBuilder.reflectionToString(e), e);
    
	throw new SenderException(e);
}

}
    /**
     * This method is used when e.g. the JMS ReplyTo field is used.
     * @param dest      The destination
     * @param correlationID
     * @param message
     * @throws SendException
     */
	public synchronized void sendMessage(Destination dest, String correlationID, String message) throws SenderException {
		try {
			send(dest, createTextMessage(correlationID, message));
       log.info(
            "["
                + getName() 
                + "] sent Message: ["
                + message
                + "] to replyAddress["
                + dest.toString()
                + "] correlationID["
                + correlationID
                + "] using "
                + (getPersistent() ? "persistent" : "non-persistent")
                + " mode");
		} catch (Throwable  e) {
			String errorString=ToStringBuilder.reflectionToString(e);
			log.error("["+getName()+"] got exception "+errorString, e);
			throw new SenderException(errorString, e);
			
		}
		

	}
    public void setReplyToName(String replyTo){
        this.replyToName=replyTo;
    }
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        ts.append("replyToName", replyToName);
        result += ts.toString();
        return result;

	}
}
