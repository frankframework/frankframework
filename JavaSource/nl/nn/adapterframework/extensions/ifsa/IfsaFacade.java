/*
 * $Log: IfsaFacade.java,v $
 * Revision 1.20  2005-04-26 15:17:28  L190409
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

import com.ing.ifsa.*;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;

import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;

import javax.jms.*;

/**
 * Base class for IFSA 2.0 functions.
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
 * <tr><td>{@link #setTimeOut(long) listener.timeOut}</td><td>receiver timeout, in milliseconds</td><td>defined by IFSA expiry</td></tr>
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author Johan Verrips / Gerrit van Brakel
 * @since 4.2
 */
public class IfsaFacade implements INamedObject, HasPhysicalDestination {
	public static final String version="$Id: IfsaFacade.java,v 1.20 2005-04-26 15:17:28 L190409 Exp $";
    protected Logger log = Logger.getLogger(this.getClass());
    
	//private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	//private final static String IFSA_PROVIDER_URL="IFSA APPLICATION BUS";

	private String name;
	private String applicationId;
	private String serviceId;
	private IfsaMessageProtocolEnum messageProtocol;

	private long timeOut = -1; // when set (>=0), overrides IFSA-expiry


//    private IFSAQueueConnectionFactory ifsaQueueConnectionFactory = null;
//    private IFSAContext context = null;
//    private QueueConnection connection = null;
    private IFSAQueue queue;

	private IfsaApplicationConnection connection=null;
	
	private boolean requestor=false;
	private boolean provider=false;

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
		try {
			if (StringUtils.isEmpty(getApplicationId()))
				throw new ConfigurationException(getLogPrefix()+"applicationId is not specified");
			if (isRequestor() && StringUtils.isEmpty(getServiceId()))
				throw new ConfigurationException(getLogPrefix()+"serviceId is not specified");
			if (getMessageProtocolEnum() == null)
				throw new ConfigurationException(getLogPrefix()+
					"invalid messageProtocol specified ["
						+ getMessageProtocolEnum()
						+ "], should be one of the following "
						+ IfsaMessageProtocolEnum.getNames());
		} catch (IfsaException e) {
			cleanUpAfterException();
			throw new ConfigurationException("exception checking configuration",e);
		}
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
	            connection.close();
                log.debug(getLogPrefix()+"closed connection for service");
	        }
/*			if (context != null) {
				try {
					context.close();
					log.debug(getLogPrefix()+"closed context for service");
				} catch (NamingException e) {
					throw new IfsaException("exception closing context of service",e);
				}
			}
*/			
	    } finally {
	    	// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
	        queue = null;
	        connection = null;
//			ifsaQueueConnectionFactory = null;
//			context = null;
	    }
	}
	
/*	
	private IFSAContext getContext() throws IfsaException {
		try {
			if (context == null) {
				Hashtable env = new Hashtable(11);
				env.put(Context.INITIAL_CONTEXT_FACTORY, IFSA_INITIAL_CONTEXT_FACTORY);
				env.put(Context.PROVIDER_URL, IFSA_PROVIDER_URL);
				// Create context as required by IFSA 2.0. Ignore the deprecation....
				context = new IFSAContext((Context) new InitialContext(env));
			}
			return context;
		} catch (NamingException e) {
			throw new IfsaException("could not obtain context", e);
		}
	}
*/
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
				queue = getConnection().lookupService(getServiceId());
				if (log.isDebugEnabled()) {
					log.info(getLogPrefix()+ "got Queue to send messages on "+getPhysicalDestinationName());
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

	/**
	 * Returns a connection for the queue corresponding to the service
	 */
/*	
	protected QueueConnection getConnection() throws IfsaException {
		try {
			if (connection == null) {
				connection = getIfsaQueueConnectionFactory().createQueueConnection();
				connection.start();
			}
			return connection;
		} catch (Exception e) {
			throw new IfsaException("could not obtain connection", e);
		}
	}
*/
	protected IfsaApplicationConnection getConnection() throws IfsaException {
		if (connection == null) {
			synchronized (this) {
				if (connection == null) {
					connection = IfsaApplicationConnection.getConnection(getApplicationId());
				}
			}
		}
		return connection;
	}
	
	/**
	 *  Create a session on the connection to the service
	 */
	public QueueSession createSession() throws IfsaException {
		try {
			int mode = Session.AUTO_ACKNOWLEDGE; 
			if (isRequestor()) {
				mode += IFSAConstants.QueueSession.IFSA_MODE; // let requestor receive IFSATimeOutMessages
			}
			return connection.createQueueSession(isTransacted(), mode);
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
	}

	
	protected QueueSender createSender(QueueSession session, Queue queue)
	    throws IfsaException {
	
	    try {
	        QueueSender queueSender = session.createSender(queue);
	        if (log.isDebugEnabled()) {
	            log.debug(getLogPrefix()+ " got queueSender ["
	                            + ToStringBuilder.reflectionToString((IFSAQueueSender) queueSender)+ "]");
	        }
	        return queueSender;
	    } catch (Exception e) {
	        throw new IfsaException(e);
	    }
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
			queueReceiver = session.createReceiver(getServiceQueue());
		} else {
			throw new IfsaException("cannot obtain ServiceReceiver: Requestor cannot act as Provider");
		}
		if (log.isDebugEnabled()) {
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
		long expiry = getTimeOut();
		if (expiry>=0) {
			return expiry;
		}
	    try {
	        return ((IFSAQueue) getServiceQueue()).getExpiry();
	    } catch (JMSException e) {
	        throw new IfsaException("error retrieving timeOut value", e);
	    }
	}
	/**
	 * Depending on wether <code>serverName</code> or <code>clientName</code> is used the busConnection is looked up.
	 */
/*	
	private IFSAQueueConnectionFactory getIfsaQueueConnectionFactory()
	    throws IfsaException {
	
		try {	     
	    if (ifsaQueueConnectionFactory == null) {
	
            ifsaQueueConnectionFactory =
                (IFSAQueueConnectionFactory) getContext().lookupBusConnection(getApplicationId());
	
		    if (log.isDebugEnabled()) {
			    log.debug(getLogPrefix()+"got ifsaQueueConnectionFactory with properties:" 
		            + ToStringBuilder.reflectionToString(ifsaQueueConnectionFactory) +"\n" 
		        	+ " isServer: " +ifsaQueueConnectionFactory.IsServer()+"\n"  
		        	+ " isClientNonTransactional:" +ifsaQueueConnectionFactory.IsClientNonTransactional()+"\n" 
		          	+ " isClientTransactional:" +ifsaQueueConnectionFactory.IsClientTransactional()+"\n" 
		          	+ " isClientServerNonTransactional:" +ifsaQueueConnectionFactory.IsClientServerNonTransactional()+"\n" 
		          	+ " isServerTransactional:" +ifsaQueueConnectionFactory.IsClientServerTransactional()+"\n" 
			        
		        );        
	        }
	    }
	    return ifsaQueueConnectionFactory;
	    } catch (NamingException e) {
	        throw new IfsaException(e);
	    }
	}
*/	
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
			if (udzMap != null) {
				// TODO: Handle UDZs
				log.warn(getLogPrefix()+"IfsaClient: processing of UDZ maps not yet implemented");
				/*
				// process the udzMap
				IFSAUDZ udzObject = ((IFSAMessage) msg).getOutgoingUDZObject();
				udzObject.putAll(udzMap)			;
				*/
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
	        if (isTransacted()) {
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
	            log.debug(getLogPrefix()
	            		+ "] sending reply to ["
	                    + received_message.getJMSReplyTo()
	                    + "]");
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
    
    public boolean isTransacted() {
    	return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET);
    }
    
	public String toString() {
	    String result = super.toString();
	    ToStringBuilder ts = new ToStringBuilder(this);
//		ts.append("IFSA_INITIAL_CONTEXT_FACTORY", IFSA_INITIAL_CONTEXT_FACTORY);
//		ts.append("IFSA_PROVIDER_URL", IFSA_PROVIDER_URL);
		ts.append("applicationId", applicationId);
	    ts.append("serviceId", serviceId);
	    if (messageProtocol != null) {
			ts.append("messageProtocol", messageProtocol.getName());
			ts.append("transacted", isTransacted());
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
		return serviceId;
	}


	public void setApplicationId(String newApplicationId) {
		applicationId = newApplicationId;
	}
	public String getApplicationId() {
		return applicationId;
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

}
