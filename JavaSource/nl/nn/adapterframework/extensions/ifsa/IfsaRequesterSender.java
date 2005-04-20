/*
 * $Log: IfsaRequesterSender.java,v $
 * Revision 1.10  2005-04-20 14:21:53  L190409
 * removed rather useless warning
 *
 * Revision 1.9  2004/08/26 11:12:00  Johan Verrips <johan.verrips@ibissource.org>
 * Aangepast dat wanneer niet synchroon het messageid wordt teruggeven
 *
 * Revision 1.8  2004/07/22 11:03:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging of non-TextMessages
 *
 * Revision 1.7  2004/07/20 13:28:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented IFSA timeout mode
 *
 * Revision 1.6  2004/07/19 13:20:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes to logging
 *
 * Revision 1.5  2004/07/19 09:52:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made multi-threading, like JmsSender
 *
 * Revision 1.4  2004/07/15 07:43:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/07/08 12:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging refinements
 *
 * Revision 1.2  2004/07/07 13:58:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.1  2004/07/05 14:28:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Firs version, converted from IfsaClient
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.configuration.ConfigurationException;

import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;


import org.apache.commons.lang.builder.ToStringBuilder;

import com.ing.ifsa.IFSATimeOutMessage;


/**
 * {@link ISender} that sends a message to an IFSA service and, in case the messageprotocol is RR (Request-Reply)
 * it waits for an reply-message.
 * <br>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td>the ServiceID of the service to be called, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td><td>&nbsp;</td></td></tr>
 * </table>
 *
 * @author Johan Verrips / Gerrit van Brakel
 * @since 4.2
 */
public class IfsaRequesterSender extends IfsaFacade implements ISender {
	public static final String version="$Id: IfsaRequesterSender.java,v 1.10 2005-04-20 14:21:53 L190409 Exp $";
  
	public IfsaRequesterSender() {
  		super(false); // instantiate IfsaFacade as a requestor	
	}

	public void configure() throws ConfigurationException {
		super.configure();
		log.info(getLogPrefix()+" configured sender on "+getPhysicalDestinationName());
	}
	
  	public void open() throws SenderException {
	  	try {
		 	openService();
		} catch (IfsaException e) {
			throw new SenderException(getLogPrefix()+"could not start Sender", e);
	  	}
  	}
	/**
	 * Stop the sender and deallocate resources.
	 */
	public void close() throws SenderException {
	    try {
	        closeService();
	    } catch (Throwable e) {
	        throw new SenderException(getLogPrefix() + "got error occured stopping sender", e);
	    }
	}

	/**
	 * returns true for Request/Reply configurations
	 */
	public boolean isSynchronous() {
		return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY);
	}

	
	/**
	 * Retrieves a message with the specified correlationId from queue or other channel, but does no processing on it.
	 */
	private TextMessage getRawReplyMessage(TextMessage sentMessage) throws SenderException, TimeOutException {
	
		String correlationID;
	    Object msg = null;
		QueueSession replySession;
		QueueReceiver replyReceiver;
	    try {
			correlationID = sentMessage.getJMSMessageID();
		    replySession = createSession();
		    replyReceiver = getReplyReceiver(replySession, sentMessage);
	
			long timeout = getExpiry();
			log.debug(getLogPrefix()+"start waiting at most ["+timeout+"] ms for reply on message with correlation ID ["+correlationID+"]");
		    msg = replyReceiver.receive(timeout);
	    } catch (Exception e) {
	        throw new SenderException(getLogPrefix()+"got exception retrieving reply", e);
	    }
		try {
	        replyReceiver.close();
	        replySession.close();
	    } catch (JMSException e) {
	        log.error(getLogPrefix()+"error closing replyreceiver or replysession", e);
	    }
	    if (msg == null) {
	        throw new TimeOutException(getLogPrefix()+" timed out waiting for reply with correlationID [" + correlationID + "]");
	    }
		if (msg instanceof IFSATimeOutMessage) {
			throw new TimeOutException(getLogPrefix()+"received IFSATimeOutMessage waiting for reply with correlationID [" + correlationID + "]");
		}
		try {
			TextMessage result = (TextMessage)msg;
			return result;
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"reply received for message with correlationId [" + correlationID + "] cannot be cast to TextMessage ["+msg.getClass().getName()+"]",e);
		}
	
	}
	/**
	 * Execute a request to the IFSA service.
	 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
	 */
	public String sendMessage(String message)
	    throws SenderException, TimeOutException {
	    String result = null;
		QueueSession session = null;
		QueueSender sender = null;
		    
		
		try {
			log.debug(getLogPrefix()+"creating session and sender");
			session = createSession();
			sender = createSender(session, getServiceQueue());

			// TODO: handle UDZs
			log.debug(getLogPrefix()+"sending message");
		    TextMessage sentMessage=sendMessage(session, sender, message, null);
			log.debug(getLogPrefix()+"message sent");

			if (isSynchronous()){
		
				log.debug(getLogPrefix()+"waiting for reply");
				TextMessage msg=null;
			    msg=getRawReplyMessage(sentMessage);
				result=msg.getText();
				log.debug(getLogPrefix()+"reply received");
					
		    } else{
		    	 result=sentMessage.getJMSMessageID(); 
		    }
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix()+"caught JMSException in sendMessage()",e);
		} catch (IfsaException e) {
			throw new SenderException(getLogPrefix()+"caught IfsaException in sendMessage()",e);
		} finally {
			if (sender != null) {
				try {
					sender.close();
				} catch (JMSException e) {
					log.debug(getLogPrefix()+"Exception closing sender", e);
				}
			}
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
					log.debug(getLogPrefix()+"Exception closing session", e);
				}
			}
		}
	    return result;
	
	}
	/**
	 * Execute a request to the IFSA service.
	 * <p>As IFSA does not allow setting the correlationID, the value supplied is not used</p>
	 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
	 */
	public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
	
	    // if (StringUtils.isNotEmpty(dummyCorrelationId)) {
		//     log.warn(getLogPrefix()+"sendMessage() ignoring correlationId ["+dummyCorrelationId+"]");
	    // }
	    return sendMessage(message);
	}
	
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        result += ts.toString();
        return result;

	}
}
