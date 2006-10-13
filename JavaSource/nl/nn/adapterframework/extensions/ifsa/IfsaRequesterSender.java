/*
 * $Log: IfsaRequesterSender.java,v $
 * Revision 1.22  2006-10-13 08:13:36  europe\L190409
 * modify comments
 *
 * Revision 1.21  2006/02/23 11:39:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * correct handling of IfsaReport reply messages
 *
 * Revision 1.20  2006/01/05 13:55:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.19  2005/12/20 16:59:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.18  2005/10/26 08:48:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.17  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.16  2005/10/18 07:04:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of dynamic reply queues
 *
 * Revision 1.15  2005/09/13 15:56:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed acknowledge mode back to AutoAcknowledge
 * provided option to set serviceId dynamically from a parameter
 *
 * Revision 1.14  2005/09/01 11:19:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no ack in case of timeout
 *
 * Revision 1.13  2005/08/31 16:33:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use same session for sending and receiving of reply
 *
 * Revision 1.12  2005/08/24 15:45:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * acknowledge receipt of reply-message
 *
 * Revision 1.11  2005/04/26 09:24:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * put closings in finally clause
 *
 * Revision 1.10  2005/04/20 14:21:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;


import org.apache.commons.lang.builder.ToStringBuilder;

import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAReportMessage;
import com.ing.ifsa.IFSATimeOutMessage;


/**
 * {@link ISender} that sends a message to an IFSA service and, in case the messageprotocol is RR (Request-Reply)
 * it waits for an reply-message.
 * <br>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td>the ServiceID of the service to be called, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td><td>&nbsp;</td></td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>must be set <code>true</true> for FF senders in transacted mode</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>serviceId (or any other name)</td><td>string</td>
 * <td>When a parameter is present, it is used to determine the serviceId to be called. However, a serviceId must still be specified as an attribute, for compatibility reasons</td></tr>
 * </table>
 *
 * @author Johan Verrips / Gerrit van Brakel
 * @since  4.2
 */
public class IfsaRequesterSender extends IfsaFacade implements ISenderWithParameters {
	public static final String version="$RCSfile: IfsaRequesterSender.java,v $ $Revision: 1.22 $ $Date: 2006-10-13 08:13:36 $";
 
	protected ParameterList paramList = null;
  
	public IfsaRequesterSender() {
  		super(false); // instantiate IfsaFacade as a requestor	
	}

	public void configure() throws ConfigurationException {
		super.configure();
		if (paramList!=null) {
			paramList.configure();
		}
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
	private Message getRawReplyMessage(QueueSession session, TextMessage sentMessage) throws SenderException, TimeOutException {
	
		String selector=null;
	    Message msg = null;
		QueueReceiver replyReceiver=null;
		try {
		    replyReceiver = getReplyReceiver(session, sentMessage);
			selector=replyReceiver.getMessageSelector();
			
			long timeout = getExpiry();
			log.debug(getLogPrefix()+"start waiting at most ["+timeout+"] ms for reply on message using selector ["+selector+"]");
		    msg = replyReceiver.receive(timeout);
		    if (msg!=null) {
		    	log.debug(getLogPrefix()+"received reply");
/*		    	
				try {
					if (!isTransacted() && !isJmsTransacted()) {
						msg.acknowledge();
						log.debug(getLogPrefix()+"acknowledged received message");
					}
				} catch (JMSException e) {
					log.error(getLogPrefix()+"exception in ack ", e);
				}
*/				
		    }

	    } catch (Exception e) {
	        throw new SenderException(getLogPrefix()+"got exception retrieving reply", e);
	    } finally {
			try {
				closeReplyReceiver(replyReceiver);
			} catch (IfsaException e) {
				log.error(getLogPrefix()+"error closing replyreceiver", e);
	        } 
		}
	    if (msg == null) {
	        throw new TimeOutException(getLogPrefix()+" timed out waiting for reply using selector ["+selector+"]");
	    }
		if (msg instanceof IFSATimeOutMessage) {
			throw new TimeOutException(getLogPrefix()+"received IFSATimeOutMessage waiting for reply using selector ["+selector+"]");
		}
		return msg;
//		try {
//			TextMessage result = (TextMessage)msg;
//			return result;
//		} catch (Exception e) {
//			throw new SenderException(getLogPrefix()+"reply received for message using selector ["+selector+"] cannot be cast to TextMessage ["+msg.getClass().getName()+"]",e);
//		}
	}

	public String sendMessage(String message) throws SenderException, TimeOutException {
		return sendMessage(null, message, null);
	}

	public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
		return sendMessage(dummyCorrelationId, message, null);
	}
	
	/**
	 * Execute a request to the IFSA service.
	 * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
	 */
	public String sendMessage(String dummyCorrelationId, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
	    String result = null;
		QueueSession session = null;
		QueueSender sender = null;
		    
		
		try {
			log.debug(getLogPrefix()+"creating session and sender");
			session = createSession();
			IFSAQueue queue;
			if (prc != null && paramList != null) {
				ParameterValueList paramValues = prc.getValues(paramList);
				String serviceId = paramValues.getParameterValue(0).getValue().toString();
				queue = getConnection().lookupService(getConnection().polishServiceId(serviceId));
				if (queue==null) {
					throw new SenderException(getLogPrefix()+"got null as queue for serviceId ["+serviceId+"]");
				}
				if (log.isDebugEnabled()) {
					log.info(getLogPrefix()+ "got Queue to send messages on ["+queue.getQueueName()+"]");
				}
			} else {
				queue = getServiceQueue();
			}
			sender = createSender(session, queue);

			// TODO: handle outgoing UDZs
			log.debug(getLogPrefix()+"sending message");
		    TextMessage sentMessage=sendMessage(session, sender, message, null);
			log.debug(getLogPrefix()+"message sent");

			if (isSynchronous()){
		
				log.debug(getLogPrefix()+"waiting for reply");
				Message msg=getRawReplyMessage(session, sentMessage);
				if (msg instanceof TextMessage) {
					result = ((TextMessage)msg).getText();
				} else {
					if (msg.getClass().getName().endsWith("IFSAReportMessage")) {
						if (msg instanceof IFSAReportMessage) {
							IFSAReportMessage irm = (IFSAReportMessage)msg;
							log.warn(getLogPrefix()+"received IFSAReportMessage ["+irm.toString()+"]");
							result = "<IFSAReport>"+
										"<NoReplyReason>"+irm.getNoReplyReason()+"</NoReplyReason>"+
									 "</IFSAReport>";
						 }
					} else {
						log.warn(getLogPrefix()+"received neither TextMessage nor IFSAReportMessage but ["+msg.getClass().getName()+"]");
						result = msg.toString();
					}
				}
				log.debug(getLogPrefix()+"reply received");
					
		    } else{
		    	 result=sentMessage.getJMSMessageID(); 
		    }
		} catch (JMSException e) {
			throw new SenderException(getLogPrefix()+"caught JMSException in sendMessage()",e);
		} catch (IfsaException e) {
			throw new SenderException(getLogPrefix()+"caught IfsaException in sendMessage()",e);
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"caught ParameterException in sendMessage() determining serviceId",e);
		} finally {
			if (sender != null) {
				try {
					log.debug(getLogPrefix()+"closing sender");
					sender.close();
				} catch (JMSException e) {
					log.debug(getLogPrefix()+"Exception closing sender", e);
				}
			}
			closeSession(session);
		}
	    return result;	
	}


	
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        result += ts.toString();
        return result;

	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}
}
