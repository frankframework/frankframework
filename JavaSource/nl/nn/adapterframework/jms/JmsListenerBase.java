/*
 * $Log: JmsListenerBase.java,v $
 * Revision 1.2  2008-09-17 09:49:32  europe\L190409
 * implement interface HasSender
 *
 * Revision 1.1  2008/09/01 15:13:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made common baseclass for pushing and pulling jms listeners
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Date;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Common baseclass for Pulling and Pushing JMS Listeners.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class JmsListenerBase extends JMSFacade implements HasSender {

	private long timeOut = 3000;
	private boolean useReplyTo=true;
	private String replyMessageType=null;
	private long replyMessageTimeToLive=0;
	private int replyPriority=-1;
	private String replyDeliveryMode=MODE_NON_PERSISTENT;
	private ISender sender;
		
	private boolean forceMessageIdAsCorrelationId=false;
 
	private String commitOnState="success";



	public void configure() throws ConfigurationException {
		super.configure();
		ISender sender = getSender();
		if (sender != null) {
			sender.configure();
		}
	}

	public void open() throws ListenerException {
		try {
			openFacade();
		} catch (Exception e) {
			throw new ListenerException("error opening listener [" + getName() + "]", e);
		}
	
		try {
			if (getSender() != null)
				getSender().open();
		} catch (SenderException e) {
			throw new ListenerException("error opening sender [" + getSender().getName() + "]", e);
		}
	}
	
	public void close() throws ListenerException {
		try {
			closeFacade();
	
			if (getSender() != null) {
				getSender().close();
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}
	
   
	/**
	 * Fill in thread-context with things needed by the JMSListener code.
	 * This includes a Session. The Session object can be passed in
	 * externally.
	 * 
	 * @param rawMessage - Original message received, can not be <code>null</code>
	 * @param threadContext - Thread context to be populated, can not be <code>null</code>
	 * @param session - JMS Session under which message was received; can be <code>null</code>
	 */
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
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
		Date tsSent = null;
		Destination replyTo=null;
		try {
			mode = deliveryModeToString(message.getJMSDeliveryMode());
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
			if (isForceMessageIdAsCorrelationId()){
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
			tsSent = new Date(lTimeStamp);

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

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"listener on ["+ getDestinationName() 
				+ "] got message with JMSDeliveryMode=[" + mode
				+ "] \n  JMSMessageID=[" + id
				+ "] \n  JMSCorrelationID=[" + cid
				+ "] \n  Timestamp Sent=[" + DateUtils.format(tsSent) 
				+ "] \n  ReplyTo=[" + ((replyTo==null)?"none" : replyTo.toString())
				+ "] \n Message=[" + message.toString()
				+ "]");
		}    
		PipeLineSession.setListenerParameters(threadContext, id, cid, null, tsSent);
		threadContext.put("timestamp",tsSent);
		threadContext.put("replyTo",replyTo);
		try {
			if (getAckMode() == Session.CLIENT_ACKNOWLEDGE) {
				message.acknowledge();
				log.debug("Listener on [" + getDestinationName() + "] acknowledged message");
			}
		} catch (JMSException e) {
			log.error("Warning in ack", e);
		}
		return cid;
	}


	/**
	 * Extracts string from message obtained from {@link #getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
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
	public ISender getSender() {
		return sender;
	}

	/**
	 * By default, the JmsListener takes the Correlation ID (if present) as the ID that has to be put in the
	 * correlation id of the reply. When you set ForceMessageIdAsCorrelationId to <code>true</code>,
	 * the messageID set in the correlationID of the reply.
	 * @param force
	 */
	public void setForceMessageIdAsCorrelationId(boolean force){
	   forceMessageIdAsCorrelationId=force;
	}
	public boolean isForceMessageIdAsCorrelationId(){
	  return forceMessageIdAsCorrelationId;
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
	public boolean isUseReplyTo() {
		return useReplyTo;
	}

	
	public void setReplyMessageType(String string) {
		replyMessageType = string;
	}
	public String getReplyMessageType() {
		return replyMessageType;
	}


	public void setReplyDeliveryMode(String string) {
		replyDeliveryMode = string;
	}
	public String getReplyDeliveryMode() {
		return replyDeliveryMode;
	}


	public void setReplyPriority(int i) {
		replyPriority = i;
	}
	public int getReplyPriority() {
		return replyPriority;
	}


	public void setReplyMessageTimeToLive(long l) {
		replyMessageTimeToLive = l;
	}
	public long getReplyMessageTimeToLive() {
		return replyMessageTimeToLive;
	}

}
