/*
 * $Log: IfsaFacade.java,v $
 * Revision 1.6  2004-07-05 14:29:45  L190409
 * restructuring to align with IFSA naming scheme
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import com.ing.ifsa.*;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;

import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.*;
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
 * <tr><td>{@link #setServiceId(String) serviceId}</td><td></td><td>only for Requesters: the ServiceID, in the form of "IFSA://<i>ServiceID</i>"</td><td>&nbsp;</td></tr>
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
public class IfsaFacade implements INamedObject {
	public static final String version="$Id: IfsaFacade.java,v 1.6 2004-07-05 14:29:45 L190409 Exp $";
    protected Logger log = Logger.getLogger(this.getClass());
    
	private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private final static String IFSA_PROVIDER_URL="IFSA APPLICATION BUS";

	private String name;
	private String applicationId;
	private String serviceId;
	private IfsaMessageProtocolEnum messageProtocol;

    private IFSAQueueConnectionFactory ifsaQueueConnectionFactory = null;
    private IFSAContext context = null;
    private QueueConnection connection = null;
	private Queue queue;
	
	private boolean requestor=false;
	private boolean provider=false;

	private int ackMode = Session.AUTO_ACKNOWLEDGE;


	public IfsaFacade(boolean asProvider) {
		super();
		if (asProvider) {
			provider=true;
		}
		else
			requestor=true;
	}
	
	public String getLogPrefix() {
		return this.getClass().getName() + "["+ getName()+ "] of application ["+getApplicationId()+"] serviceId ["+getServiceId()+"] ";
	}

	/**
	 * This method performs some basic checks.
	 */
	public void configure() throws ConfigurationException {
		// perform some basic checks
		try {
			if (isRequestor() && StringUtils.isEmpty(getServiceId()))
				throw new ConfigurationException(getLogPrefix()+"serviceId is not specified");
			if (getMessageProtocolEnum() == null)
				throw new ConfigurationException(getLogPrefix()+
					"invalid messageProtocol specified ["
						+ getMessageProtocolEnum()
						+ "], should be one of the following "
						+ IfsaMessageProtocolEnum.getNames());
		} catch (IfsaException e) {
			throw new ConfigurationException(getLogPrefix()+"exception checking configuration",e);
		}
	}

	public void openService() throws IfsaException {
		try {
			connection = getConnection();
			connection.start();
			queue = getServiceQueue();
		} catch (Exception e) {
			throw new IfsaException(e);
		}
	}

	public void closeService() throws IfsaException {
	    try {
	        if (connection != null) {
	            connection.close();
	            if (log.isDebugEnabled()) {
	                log.debug(getLogPrefix()+"closed connection for service");
	            }
	        }
	    } catch (JMSException e) {
	        throw new IfsaException(e);
	    } finally {
	        queue = null;
	        connection = null;
	    }
	}
	
	
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
			throw new IfsaException(e);
		}
	}

	/**
	 * Looks up the <code>serviceId</code> in the <code>IFSAContext</code>.<br/>
	 * <p>The method is knowledgable of Provider versus Requester processing.
	 * When the request concerns a Provider <code>lookupProviderInput</code> is used,
	 * when it concerns a Requester <code>lookupService(serviceId)</code> is used.
	 * This method distinguishes a server-input queue and a client-input queue
	 */
	protected Queue getServiceQueue() throws IfsaException {
		if (queue == null) {
			try {
				if (isRequestor()) {
					queue = (Queue) getContext().lookupService(getServiceId());
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix()+ "got Queue to send messages on");
					}
				} else {
					queue = (Queue) getContext().lookupProviderInput();
					if (log.isDebugEnabled()) {
						log.debug(getLogPrefix()+ "got Queue to receive messages from");
					}
				}
	
			} catch (NamingException e) {
				throw new IfsaException(e);
			}
		}
		return queue;
	}

	/**
	 * Returns a connection for the queue corresponding to the service
	 */
	protected QueueConnection getConnection() throws IfsaException {
		try {
			if (connection == null) {
				connection = getIfsaQueueConnectionFactory().createQueueConnection();
			}
			return connection;
		} catch (Exception e) {
			throw new IfsaException(e);
		}
	}

	/**
	 *  Create a session on the connection to the service
	 */
	public QueueSession createSession() throws IfsaException {
		try {
			//TODO: incorporate IFSA_MODE for IFSA-compliant TimeOut
			return connection.createQueueSession(isTransacted(), ackMode);
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
	 * @see IfsaBase#getQueue()
	 * @return                                   The queueReceiver value
	 * @exception  javax.naming.NamingException  Description of the Exception
	 * @exception  javax.jms.JMSException                  Description of the Exception
	 */
	protected QueueReceiver getServiceReceiver(
		QueueSession session)
		throws IfsaException {
	
		try {
		QueueReceiver queueReceiver;
		    
		if (isProvider()) {
			queueReceiver = session.createReceiver(getServiceQueue());
		} else {
			throw new IfsaException(getLogPrefix()+ "cannot obtain ServiceReceiver: Requestor cannot act as Provider");
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
	
	
	/**
	 * Retrieves the reply queue for a <b>client</b> connection. If the
	 * client is transactional the replyqueue is retrieved from IFSA,
	 * otherwise a temporary (dynamic) queue is created.
	 */
	private Queue getClientReplyQueue(QueueSession session) throws IfsaException {
	    Queue replyQueue = null;
	
	    try {
	        /*
	         * if we don't know if we're using a dynamic reply queue, we can
	         * check this using the function IsClientTransactional
	         * Yes -> we're using a static reply queue
	         * No -> dynamic reply queue
	         */
	        if (getIfsaQueueConnectionFactory().IsClientTransactional()) { // Static
	            replyQueue = (Queue) getContext().lookupReply(getApplicationId());
	            log.debug(getLogPrefix()+"got static reply queue [" +replyQueue.getQueueName()+"]");            
	        } else { // Temporary Dynamic
	            replyQueue =  session.createTemporaryQueue();
	            log.debug(getLogPrefix()+"got dynamic reply queue [" +replyQueue.getQueueName()+"]");
	        }
	        return replyQueue;
	    } catch (Exception e) {
	        throw new IfsaException(e);
	    }
	}
	public long getExpiry() throws IfsaException {
	    try {
	        return ((IFSAQueue) getServiceQueue()).getExpiry();
	    } catch (JMSException e) {
	        throw new IfsaException(getLogPrefix()+"error retrieving timeOut value", e);
	    }
	}
	/**
	 * Depending on wether <code>serverName</code> or <code>clientName</code> is used the busConnection is looked up.
	 */
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
	 * @see javax.jms.QueueReceiver
	 * @see IfsaBase#getQueue()
	 * @return                                   The queueReceiver value
	 * @exception  javax.naming.NamingException  Description of the Exception
	 * @exception  javax.jms.JMSException                  Description of the Exception
	 */
	public QueueReceiver getReplyReceiver(
	    QueueSession session, Message sentMessage)
	    throws IfsaException {
	
		QueueReceiver queueReceiver;
		    
	    if (isProvider()) {
	        throw new IfsaException(getLogPrefix()+"cannot get ReplyReceiver: Provider cannot act as Requestor");
	    } 
	
	    String correlationId;
	    Queue replyQueue;
	    try {
			correlationId=sentMessage.getJMSCorrelationID();
			replyQueue=(Queue)sentMessage.getJMSReplyTo();
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
		
		try {
	//		timeOut=((IFSAQueue) getServiceQueue()).getExpiry();
		} catch (Exception e) {throw new IfsaException("error retrieving timeOut value", e);}
		
		try {
	
			if (getIfsaQueueConnectionFactory().IsClientTransactional()) {
						String selector="JMSCorrelationID='" + correlationId + "'";
			            queueReceiver = session.createReceiver(replyQueue, selector);
			            log.debug("** transactional client - selector ["+selector+"]");
	            	} else {
	            		queueReceiver = session.createReceiver(replyQueue);
	            		log.debug("** non-transactional client" );
	            	}	
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
	
	    return queueReceiver;
	}
	/**
	 * Indicates whether the object at hand represents a Client (returns <code>True</code>) or
	 * a Server (returns <code>False</code>).
	 */
	public boolean isRequestor() throws IfsaException {
			
		if (requestor && provider) {
	        throw new IfsaException(getLogPrefix()+"cannot be both Requestor and Provider");
		}
		if (!requestor && !provider) {
	        throw new IfsaException(getLogPrefix()+"not configured as Requestor or Provider");
		}
		return requestor;
	}
	/**
	 * Indicates whether the object at hand represents a Client (returns <code>False</code>) or
	 * a Server (returns <code>True</code>).
	 *
	 * @see #isClient()
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
	        if (!isRequestor()) {
		        throw new IfsaException(getLogPrefix()+ "Provider cannot use sendMessage, should use sendReply");
	        }
	        if (messageProtocol.equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
	            // set reply-to address
	            Queue replyTo=getClientReplyQueue(session);
	            msg.setJMSReplyTo((Destination)replyTo);
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
	            if (log.isDebugEnabled()) {
		            log.debug(getLogPrefix()+ "committing (send) transaction");
	            }
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
	        QueueSender tqs =
	            session.createSender((Queue) received_message.getJMSReplyTo());
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
		ts.append("IFSA_INITIAL_CONTEXT_FACTORY", IFSA_INITIAL_CONTEXT_FACTORY);
		ts.append("IFSA_PROVIDER_URL", IFSA_PROVIDER_URL);
		ts.append("applicationId", applicationId);
	    ts.append("serviceId", serviceId);
	    if (messageProtocol != null)
	        ts.append("messageProtocol", messageProtocol.getName());
	    else
	        ts.append("messageProtocol", "null!");
	
	    ts.append("transacted", isTransacted());
	    result += ts.toString();
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
}
