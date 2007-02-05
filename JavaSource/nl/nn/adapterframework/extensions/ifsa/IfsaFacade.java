/*
 * $Log: IfsaFacade.java,v $
 * Revision 1.43  2007-02-05 14:56:29  europe\L190409
 * make isJmsTransacted() protected
 *
 * Revision 1.42  2006/11/06 08:15:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modifications for dynamic serviceId
 *
 * Revision 1.41  2006/10/13 08:08:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modify comments
 *
 * Revision 1.40  2006/08/21 15:08:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.39  2006/07/17 08:54:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * documented custom property ifsa.provider.useSelectors
 *
 * Revision 1.38  2006/02/09 07:59:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restored compatibility with IFSA releases without provider selection mechanism
 *
 * Revision 1.37  2006/01/23 08:55:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use providerSelector (when available in ifsajms)
 *
 * Revision 1.36  2005/12/28 08:47:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.35  2005/12/20 16:59:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.34  2005/11/02 09:08:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * ifsa-mode connection not for single dynamic reply queue
 *
 * Revision 1.33  2005/10/26 08:23:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.32  2005/10/24 15:10:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made sessionsArePooled configurable via appConstant 'jms.sessionsArePooled'
 *
 * Revision 1.31  2005/10/18 07:04:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * better handling of dynamic reply queues
 *
 * Revision 1.30  2005/09/26 11:44:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Jms-commit only if not XA-transacted
 *
 * Revision 1.29  2005/09/13 15:48:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed acknowledge mode back to AutoAcknowledge
 *
 * Revision 1.28  2005/08/31 16:32:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected code for static reply queues
 *
 * Revision 1.27  2005/07/28 07:31:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change default acknowledge mode to CLIENT
 *
 * Revision 1.26  2005/07/19 12:33:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implements IXAEnabled 
 * polishing of serviceIds, to work around problems with ':' and '/'
 *
 * Revision 1.25  2005/06/20 09:12:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set sessionsArePooled false by default
 *
 * Revision 1.24  2005/06/13 15:07:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid excessive logging in debug mode
 *
 * Revision 1.23  2005/06/13 11:59:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.22  2005/06/13 11:57:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for pooled sessions and for XA-support
 *
 * Revision 1.21  2005/05/03 15:58:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of shared connection code
 *
 * Revision 1.20  2005/04/26 15:17:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework, using IfsaApplicationConnection resulting in shared usage of connection objects
 *
 * Revision 1.19  2005/01/13 08:15:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made queue type IfsaQueue
 *
 * Revision 1.18  2004/08/23 13:12:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.17  2004/08/09 08:46:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * small changes
 *
 * Revision 1.16  2004/08/03 13:07:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved closing
 *
 * Revision 1.15  2004/07/22 13:19:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * let requestor receive IFSATimeOutMessages
 *
 * Revision 1.14  2004/07/22 11:01:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configurable timeOut
 *
 * Revision 1.13  2004/07/20 16:37:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * toch maar niet IFSA-mode timeout
 *
 * Revision 1.12  2004/07/20 13:28:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented IFSA timeout mode
 *
 * Revision 1.11  2004/07/19 13:20:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased logging + close connection on 'close'
 *
 * Revision 1.10  2004/07/15 07:35:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.9  2004/07/08 12:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging refinements
 *
 * Revision 1.8  2004/07/08 08:56:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show physical destination after configure
 *
 * Revision 1.7  2004/07/06 14:50:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included PhysicalDestination
 *
 * Revision 1.6  2004/07/05 14:29:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restructuring to align with IFSA naming scheme
 *
 */
package nl.nn.adapterframework.extensions.ifsa;


import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.AppConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.ing.ifsa.IFSAConstants;
import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAQueueSender;
import com.ing.ifsa.IFSAServerQueueSender;

/**
 * Base class for IFSA 2.0/2.2 functions.
 * <br/>
 * <p>Descenderclasses must set either Requester or Provider behaviour in their constructor.</p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaFacade</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td>only for Requesters: the ServiceID, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>must be set <code>true</true> for FF senders/listeners in transacted mode</td><td>false</td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>defined by IFSA expiry</td></tr>
 * </table>
 * 
 * N.B. 
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on 
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 * 
 * <code>    ifsa.provider.useSelectors=false</code>
 * 
 * @author Johan Verrips / Gerrit van Brakel
 * @since 4.2
 */
public class IfsaFacade implements INamedObject, HasPhysicalDestination, IXAEnabled {
	public static final String version = "$RCSfile: IfsaFacade.java,v $ $Revision: 1.43 $ $Date: 2007-02-05 14:56:29 $";
    protected Logger log = Logger.getLogger(this.getClass());
    
    private static int BASIC_ACK_MODE = Session.AUTO_ACKNOWLEDGE;
 
 	private final static String USE_SELECTOR_FOR_PROVIDER_KEY="ifsa.provider.useSelectors";
	private static Boolean useSelectorsStore=null; 

    
	private String name;
	private String applicationId;
	private String serviceId;
	private String polishedServiceId=null;;
	private IfsaMessageProtocolEnum messageProtocol;

	private long timeOut = -1; // when set (>=0), overrides IFSA-expiry

    private IFSAQueue queue;

	private IfsaConnection connection=null;
	
	private boolean requestor=false;
	private boolean provider=false;
		
	private boolean transacted=false; 

	private String providerSelector=null;

	public IfsaFacade(boolean asProvider) {
		super();
		if (asProvider) {
			provider=true;
		}
		else
			requestor=true;
	}
	
	protected String getLogPrefix() {
		
		String objectType;
		String serviceInfo="";
		try {
			if (isRequestor()) {
				objectType = "IfsaRequester";
				serviceInfo = "of Application ["+getApplicationId()+"] "; 
			} else {
				objectType = "IfsaProvider";				
				serviceInfo = "for Application ["+getApplicationId()+"] "; 
			} 
		} catch (IfsaException e) {
			log.debug("Exception determining objectType in getLogPrefix",e);
			objectType="Object";
			serviceInfo = "of Application ["+getApplicationId()+"]"; 
		}
		
		return objectType + "["+ getName()+ "] " + serviceInfo;  
	}

	/**
	 * Checks if messageProtocol and serviceId (only for Requestors) are specified
	 */
	public void configure() throws ConfigurationException {

		// perform some basic checks
		if (StringUtils.isEmpty(getApplicationId()))
			throw new ConfigurationException(getLogPrefix()+"applicationId is not specified");
		if (getMessageProtocolEnum() == null)
			throw new ConfigurationException(getLogPrefix()+
				"invalid messageProtocol specified ["
					+ getMessageProtocolEnum()
					+ "], should be one of the following "
					+ IfsaMessageProtocolEnum.getNames());
		// TODO: check if serviceId is specified, either as attribute or as parameter
	}

	protected void cleanUpAfterException() {
		try {
			closeService();
		} catch (IfsaException e) {
			log.warn("exception closing ifsaConnection after previous exception, current:",e);
		}
	}

	/** 
	 * Prepares object for communication on the IFSA bus.
	 * Obtains a connection and a serviceQueue.
	 */
	public void openService() throws IfsaException {
		try {
			log.debug(getLogPrefix()+"opening connection for service");
			getConnection();   // obtain and cache connection, then start it.
			getServiceQueue(); // obtain and cache service queue
		} catch (IfsaException e) {
			cleanUpAfterException();
			throw e;
		}
	}

	/** 
	 * Stops communication on the IFSA bus.
	 * Releases references to serviceQueue and connection.
	 */
	public void closeService() throws IfsaException {
	    try {
	        if (connection != null) {
	            try {
					connection.close();
				} catch (IbisException e) {
					if (e instanceof IfsaException) {
						throw (IfsaException)e;
					}
					throw new IfsaException(e);
	            }
                log.debug(getLogPrefix()+"closed connection for service");
	        }
	    } finally {
	    	// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
	        queue = null;
	        connection = null;
	    }
	}
	

	/**
	 * Looks up the <code>serviceId</code> in the <code>IFSAContext</code>.<br/>
	 * <p>The method is knowledgable of Provider versus Requester processing.
	 * When the request concerns a Provider <code>lookupProviderInput</code> is used,
	 * when it concerns a Requester <code>lookupService(serviceId)</code> is used.
	 * This method distinguishes a server-input queue and a client-input queue
	 */
	protected IFSAQueue getServiceQueue() throws IfsaException {
		if (queue == null) {
			if (isRequestor()) {
				if (getServiceId() != null) {
					queue = getConnection().lookupService(getServiceId());
					if (log.isDebugEnabled()) {
						log.info(getLogPrefix()+ "got Queue to send messages on "+getPhysicalDestinationName());
					}
				}
			} else {
				queue = getConnection().lookupProviderInput();
				if (log.isDebugEnabled()) {
					log.info(getLogPrefix()+ "got Queue to receive messages from "+getPhysicalDestinationName());
				}
			}
		}
		return queue;
	}

	protected IfsaConnection getConnection() throws IfsaException {
		if (connection == null) {
			synchronized (this) {
				if (connection == null) {
					log.debug(getLogPrefix()+"instantiating IfsaConnectionFactory");
					IfsaConnectionFactory ifsaConnectionFactory = new IfsaConnectionFactory();
					try {
						log.debug(getLogPrefix()+"creating IfsaConnection");
						connection = (IfsaConnection)ifsaConnectionFactory.getConnection(getApplicationId());
					} catch (IbisException e) {
						if (e instanceof IfsaException) {
							throw (IfsaException)e;
						}
						throw new IfsaException(e);
					}
				}
			}
		}
		return connection;
	}
	
	/**
	 *  Create a session on the connection to the service
	 */
	protected QueueSession createSession() throws IfsaException {
		try {
			int mode = BASIC_ACK_MODE; 
			if (isRequestor() && connection.canUseIfsaModeSessions()) {
				mode += IFSAConstants.QueueSession.IFSA_MODE; // let requestor receive IFSATimeOutMessages
			}
			return (QueueSession) connection.createSession(isJmsTransacted(), mode);
		} catch (IbisException e) {
			if (e instanceof IfsaException) {
				throw (IfsaException)e;
			}
			throw new IfsaException(e);
		}
	}

	protected void closeSession(Session session) {
		try {
			getConnection().releaseSession(session);
		} catch (IfsaException e) {
			log.warn("Exception releasing session", e);
		}
	}

	
	protected QueueSender createSender(QueueSession session, Queue queue)
	    throws IfsaException {
	
	    try {
	        QueueSender queueSender = session.createSender(queue);
	        if (log.isDebugEnabled()) {
	            log.debug(getLogPrefix()+ "got queueSender ["
	                            + ToStringBuilder.reflectionToString((IFSAQueueSender) queueSender)+ "]");
	        }
	        return queueSender;
	    } catch (Exception e) {
	        throw new IfsaException(e);
	    }
	}

	protected synchronized String getProviderSelector() {
		if (providerSelector==null && useSelectorsForProviders()) {
			try {
				providerSelector=""; // set default, also to avoid re-evaluation time and time again for lower ifsa-versions.
				if (messageProtocol.equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
					providerSelector=IFSAConstants.QueueReceiver.SELECTOR_RR;
				}
				if (messageProtocol.equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET)) {
					providerSelector=IFSAConstants.QueueReceiver.SELECTOR_FF;
				}
			} catch (Throwable t) {
				log.debug(getLogPrefix()+"exception determining selector, probably lower ifsa version, ignoring");
			}
		}
		return providerSelector;
	}

	/**
	 * Gets the queueReceiver, by utilizing the <code>getInputQueue()</code> method.<br/>
	 * For serverside getQueueReceiver() the creating of the QueueReceiver is done
	 * without the <code>selector</code> information, as this is not allowed
	 * by IFSA.<br/>
	 * For a clientconnection, the receiver is done with the <code>getClientReplyQueue</code>
	 * @see javax.jms.QueueReceiver
	 */
	protected QueueReceiver getServiceReceiver(
		QueueSession session)
		throws IfsaException {
	
		try {
			QueueReceiver queueReceiver;
			    
			if (isProvider()) {
				String selector = getProviderSelector();			
				if (StringUtils.isEmpty(selector)) {
					queueReceiver = session.createReceiver(getServiceQueue());
				} else {
					log.debug(getLogPrefix()+"using selector ["+selector+"]");
					try {
						queueReceiver = session.createReceiver(getServiceQueue(), selector);
					} catch (JMSException e) {
						log.warn("caught exception, probably due to use of selector ["+selector+"], falling back to non-selected mode",e);
						queueReceiver = session.createReceiver(getServiceQueue());
					}
				}
			} else {
				throw new IfsaException("cannot obtain ServiceReceiver: Requestor cannot act as Provider");
			}
			if (log.isDebugEnabled() && !isSessionsArePooled()) {
				log.debug(getLogPrefix()+ "got receiver for queue ["
						+ queueReceiver.getQueue().getQueueName()
						+ "] "+ ToStringBuilder.reflectionToString(queueReceiver));
			}
			return queueReceiver;
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
	}
	
	public long getExpiry() throws IfsaException {
		return getExpiry((IFSAQueue) getServiceQueue());
	}
	
	public long getExpiry(IFSAQueue queue) throws IfsaException {
		long expiry = getTimeOut();
		if (expiry>=0) {
			return expiry;
		}
		try {
			return queue.getExpiry();
		} catch (JMSException e) {
			throw new IfsaException("error retrieving timeOut value", e);
		}
	}

    public String getMessageProtocol() {
        return messageProtocol.getName();
    }
    public IfsaMessageProtocolEnum getMessageProtocolEnum() {
        return messageProtocol;
    }
    
	/**
	 * Gets the queueReceiver, by utilizing the <code>getInputQueue()</code> method.<br/>
	 * For serverside getQueueReceiver() the creating of the QueueReceiver is done
	 * without the <code>selector</code> information, as this is not allowed
	 * by IFSA.<br/>
	 * For a clientconnection, the receiver is done with the <code>getClientReplyQueue</code>
	 */
	public QueueReceiver getReplyReceiver(QueueSession session, Message sentMessage)
	    throws IfsaException {
		    
	    if (isProvider()) {
	        throw new IfsaException("cannot get ReplyReceiver: Provider cannot act as Requestor");
	    } 
	
		return getConnection().getReplyReceiver(session, sentMessage);
	}

	public void closeReplyReceiver(QueueReceiver receiver) throws IfsaException {
		log.debug(getLogPrefix()+"closing replyreceiver");
		getConnection().closeReplyReceiver(receiver);
	}
	
	/**
	 * Indicates whether the object at hand represents a Client (returns <code>True</code>) or
	 * a Server (returns <code>False</code>).
	 */
	public boolean isRequestor() throws IfsaException {
			
		if (requestor && provider) {
	        throw new IfsaException("cannot be both Requestor and Provider");
		}
		if (!requestor && !provider) {
	        throw new IfsaException("not configured as Requestor or Provider");
		}
		return requestor;
	}
	/**
	 * Indicates whether the object at hand represents a Client (returns <code>False</code>) or
	 * a Server (returns <code>True</code>).
	 *
	 * @see #isRequestor()
	 */
	public boolean isProvider() throws IfsaException {
		return ! isRequestor();
	}
    /**
     * Sends a message,and if transacted, the queueSession is committed.
     * <p>This method is intended for <b>clients</b>, as <b>server</b>s
     * will use the <code>sendReply</code>.
     * @return the correlationID of the sent message
     */
    public TextMessage sendMessage(QueueSession session, QueueSender sender, String message, Map udzMap)
        throws IfsaException {

	    try {
			if (!isRequestor()) {
				throw new IfsaException(getLogPrefix()+ "Provider cannot use sendMessage, should use sendReply");
			}
	        TextMessage msg = session.createTextMessage();
	        msg.setText(message);
			if (udzMap != null && msg instanceof IFSAMessage) {
				// Handle UDZs
				log.debug(getLogPrefix()+"add UDZ map to IFSAMessage");
				// process the udzMap
				Map udzObject = (Map)((IFSAMessage) msg).getOutgoingUDZObject();
				udzObject.putAll(udzMap);
			}
			String replyToQueueName="-"; 
	        //Client side
	        if (messageProtocol.equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
	            // set reply-to address
	            Queue replyTo=getConnection().getClientReplyQueue(session);
	            msg.setJMSReplyTo(replyTo);
	            replyToQueueName=replyTo.getQueueName();
	        }
	        if (messageProtocol.equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET)) {
	         	// not applicable
	        }
	
	        log.info(getLogPrefix()
	        	    + " messageProtocol ["
	                + messageProtocol
	                + "] replyToQueueName ["
	                + replyToQueueName
	                + "] sending message ["
	                + message
	                + "]");
	
	        // send the message
	        sender.send(msg);
	
	        // perform commit
	        if (isJmsTransacted() && !isTransacted()) {
	            session.commit();
	            log.debug(getLogPrefix()+ "committing (send) transaction");
	        }
	
	        return msg;
		    
	 	} catch (JMSException e) {
			throw new IfsaException(e);
		}
	}
	
	/**
	 * Intended for server-side reponse sending and implies that the received
	 * message *always* contains a reply-to address.
	 */
	public void sendReply(QueueSession session, Message received_message, String response)
	    throws IfsaException {
	    try {
	        TextMessage answer = session.createTextMessage();
	        answer.setText(response);
			Queue replyQueue = (Queue)received_message.getJMSReplyTo();
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+"obtained replyQueue ["+replyQueue.getQueueName()+"]");
			}
	        QueueSender tqs = session.createSender(replyQueue );
	        if (log.isDebugEnabled()) {
	            log.debug(getLogPrefix()+ "sending reply to ["+ received_message.getJMSReplyTo()+ "]");
	        }
	        ((IFSAServerQueueSender) tqs).sendReply(received_message, answer);
	        tqs.close();
	    } catch (JMSException e) {
	        throw new IfsaException(e);
	    }
	}

    /**
     * Method logs a warning when the newMessageProtocol is not FF or RR.
     * <p>When the messageProtocol equals to FF, transacted is set to true</p>
     * <p>Creation date: (08-05-2003 9:03:53)</p>
     * @see IfsaMessageProtocolEnum
     * @param newMessageProtocol String
     */
    public void setMessageProtocol(String newMessageProtocol) {
	    if (null==IfsaMessageProtocolEnum.getEnum(newMessageProtocol)) {
        	throw new IllegalArgumentException(getLogPrefix()+
                "illegal messageProtocol ["
                    + newMessageProtocol
                    + "] specified, it should be one of the values "
                    + IfsaMessageProtocolEnum.getNames());

        	}
        messageProtocol = IfsaMessageProtocolEnum.getEnum(newMessageProtocol);
        log.debug(getLogPrefix()+"message protocol set to "+messageProtocol.getName());
    }
 
	public boolean isSessionsArePooled() {
		try {
			return getConnection().sessionsArePooled();
		} catch (IfsaException e) {
			log.error(getLogPrefix()+"could not get session",e);
			return false;
		}
	}
    
    protected boolean isJmsTransacted() {
    	return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET);
    }
    
	public String toString() {
	    String result = super.toString();
	    ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("applicationId", applicationId);
	    ts.append("serviceId", serviceId);
	    if (messageProtocol != null) {
			ts.append("messageProtocol", messageProtocol.getName());
//			ts.append("transacted", isTransacted());
			ts.append("jmsTransacted", isJmsTransacted());
	    }
	    else
	        ts.append("messageProtocol", "null!");
	
	    result += ts.toString();
	    return result;
	
	}

	public String getPhysicalDestinationName() {
	
		String result = null;
	
		try {
			if (isRequestor()) {
				result = getServiceId();
			} else {
				result = getApplicationId();
			}
			log.debug("obtaining connection and servicequeue for "+result);
			if (getConnection()!=null && getServiceQueue() != null) {
				result += " ["+ getServiceQueue().getQueueName()+"]";
			}
		} catch (Throwable t) {
			log.warn(getLogPrefix()+"got exception in getPhysicalDestinationName", t);
		}
		return result;
	}


	/**
	 * set the IFSA service Id, for requesters only
	 * @param newServiceId the name of the service, e.g. IFSA://SERVICE/CLAIMINFORMATIONMANAGEMENT/NLDFLT/FINDCLAIM:01
	 */
	public void setServiceId(String newServiceId) {
		serviceId = newServiceId;
	}

	public String getServiceId() {
		if (polishedServiceId==null && serviceId!=null) {
			try {
				IfsaConnection conn = getConnection();
				polishedServiceId = conn.polishServiceId(serviceId);
			} catch (IfsaException e) {
				log.warn("could not obtain connection, no polishing of serviceId",e);
				polishedServiceId = serviceId;
			}
		}
		return polishedServiceId;
	}


	public void setApplicationId(String newApplicationId) {
		applicationId = newApplicationId;
	}
	public String getApplicationId() {
		return applicationId;
	}

	protected synchronized boolean useSelectorsForProviders() {
		if (useSelectorsStore==null) {
			boolean pooled=AppConstants.getInstance().getBoolean(USE_SELECTOR_FOR_PROVIDER_KEY, true);
			useSelectorsStore = new Boolean(pooled);
		}
		return useSelectorsStore.booleanValue();
	}


	public void setName(String newName) {
		name = newName;
	}
	public String getName() {
		return name;
	}

	public long getTimeOut() {
		return timeOut;
	}
	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}

	public boolean isTransacted() {
		return transacted;
	}

	public void setTransacted(boolean b) {
		transacted = b;
	}

}
