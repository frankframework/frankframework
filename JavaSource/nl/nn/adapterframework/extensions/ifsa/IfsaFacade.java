package nl.nn.adapterframework.extensions.ifsa;

import com.ing.ifsa.*;

import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Hashtable;
import javax.naming.*;
import javax.jms.*;

/**
 * Base class for IFSA 1.1 functions.
 * <br/>
 * <p>When clientName is filled, a client connection is assumed, when serverName
 * is used a server connection is assumed.</p>
 * <p>messageProtocol indicates wether to use Fire &amp; Forget or Request/Reply</p>
 * 
 * @author Johan Verrips / Gerrit van Brakel
 * @version Id
 */
public class IfsaFacade {
    protected Logger log = Logger.getLogger(this.getClass());;
    private boolean transacted;
    private String jndiPath;
    private IFSAQueueConnectionFactory ifsaQueueConnectionFactory = null;
    private IFSAContext context = null;
    private QueueConnection connection = null;

    private String name;
	private int ackMode = 1;
	public static final String version="$Id: IfsaFacade.java,v 1.3 2004-03-26 09:50:51 NNVZNL01#L180564 Exp $";
 
    /**
     * the Queue object, as looked up from <code>serviceName</code>
     */
    private Queue queue;

    /**
     * the queue or servicename, e.g. <code>"IFSA://aServiceId"</code>
     */
    private String serviceName;

    private String clientName;
    private String serverName;
    /**
     * messageProtocol
     */
    private IfsaMessageProtocolEnum messageProtocol;

public void closeService() throws IfsaException {
    try {
        if (connection != null) {
            connection.close();
            if (log.isDebugEnabled()) {
                log.debug("["+ getName()+ "]"+(isClient() ? "client [" + getClientName() + "]" : "serverName [" + getServerName() + "]")
                            + " closed connection for service"
                            + " of service [" +getServiceName()+ "]");
            }
        }
    } catch (JMSException e) {
        throw new IfsaException(e);
    } finally {
        queue = null;
        connection = null;
    }
}
/**
 * This method performs some basic checks.
 */
public void configure(boolean asClient) throws IfsaException {
    // perform some basic checks
    if (asClient && !isClient()) {
        throw new IfsaException("["+ getName()+ "] "+
            "Server [" + getServerName() + "] cannot act as a client");
    }
    if (!asClient && isClient()) {
        throw new IfsaException("["+ getName()+ "] "+
            "Client [" + getClientName() + "] cannot act as a server");
    }

    if (StringUtils.isEmpty(getServiceName()))
        throw new IfsaException("["+ getName()+ "] serviceName is not specified");
    if (getMessageProtocolEnum() == null)
        throw new IfsaException("["+ getName()+ "]"+
            "invalid MessageProtocol specified ["
                + getMessageProtocolEnum()
                + "], should be one of the following "
                + IfsaMessageProtocolEnum.getNames());
}
protected QueueSender createSender(QueueSession session, Queue queue)
    throws IfsaException {

    try {
        QueueSender queueSender = session.createSender(queue);
        if (log.isDebugEnabled()) {
            log.debug("["+ getName()+ "]"+
	                        (isClient() ? "client [" + getClientName() + "]" : "serverName [" + getServerName() + "]")
                            + " got queueSender for"
                            + " service [" +getServiceName()+ "]"
                            + ToStringBuilder.reflectionToString((IFSAQueueSender) queueSender));
        }
        return queueSender;
    } catch (Exception e) {
        throw new IfsaException(e);
    }
}
/**
 *  Create a session on the connection to the service
 */
public QueueSession createSession() throws IfsaException {
    try {
        return connection.createQueueSession(isTransacted(), ackMode);
    } catch (JMSException e) {
        throw new IfsaException(e);
    }
}
    public String getClientName() {
        return clientName;
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
            replyQueue = (Queue) getContext().lookupReply(clientName);
            log.debug("[" +name+"] got static reply queue [" +replyQueue.getQueueName()+"]");            
        } else { // Temporary Dynamic
            replyQueue =  session.createTemporaryQueue();
            log.debug("[" +name+"] got dynamic reply queue [" +replyQueue.getQueueName()+"]");
        }
        return replyQueue;
    } catch (Exception e) {
        throw new IfsaException(e);
    }
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
private IFSAContext getContext() throws IfsaException {
    try {
        if (context == null) {
            Hashtable env = new Hashtable(11);
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.ing.ifsa.IFSAContextFactory");
            env.put(Context.PROVIDER_URL, "file:" + jndiPath);
            context = new IFSAContext((Context) new InitialContext(env));
        }
        return context;
    } catch (NamingException e) {
        throw new IfsaException(e);
    }
}
public long getExpiry() throws IfsaException {
    try {
        return ((IFSAQueue) getServiceQueue()).getExpiry();
    } catch (JMSException e) {
        throw new IfsaException("error retrieving timeOut value", e);
    }
}
/**
 * Depending on wether <code>serverName</code> or <code>clientName</code> is used the busConnection is looked up.
 */
private IFSAQueueConnectionFactory getIfsaQueueConnectionFactory()
    throws IfsaException {

	try {	     
    if (ifsaQueueConnectionFactory == null) {

	    if (isClient()) {
            ifsaQueueConnectionFactory =
                (IFSAQueueConnectionFactory) getContext().lookupBusConnection(getClientName());
            log.debug("["+name+"] got ifsaQueueConnectionFactory for client [" + clientName + "]");
	    } else {
            ifsaQueueConnectionFactory =
                (IFSAQueueConnectionFactory) getContext().lookupBusConnection(getServerName());
            log.debug("["+name+"] got ifsaQueueConnectionFactory for server [" + serverName + "]");
        }

	    if (log.isDebugEnabled()) {
		    log.debug("["+name+"] got ifsaQueueConnectionFactory with properties:" 
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
    public String getJndiPath() {
        return jndiPath;
    }
    public String getMessageProtocol() {
        return messageProtocol.getName();
    }
    public IfsaMessageProtocolEnum getMessageProtocolEnum() {
        return messageProtocol;
    }
public String getName() {
	return name;
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
	    
    if (isServer()) {
        throw new IfsaException("Server ["+serverName+"] cannot act as Client");
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
public String getServerName() {
    return serverName;
}
public String getServiceName() {
    return serviceName;
}
/**
 * Looks up the <code>serviceName</code> in the <code>IFSAContext</code>.<br/>
 * <p>The method is knowledgable of Server versus Client processing.
 * When the request concerns a Server <code>lookupServerInput</code> is used,
 * when it concerns a Client <code>lookupService</code> is used.
 * This method distinguishes a server-input queue and a client-input queue
 */
protected Queue getServiceQueue() throws IfsaException {
    if (queue == null) {
        try {
            if (isClient()) {
                queue = (Queue) getContext().lookupService(serviceName);
                if (log.isDebugEnabled()) {
                    log.debug(
                        "["
                            + name
                            + "] got Queue for serviceName ["
                            + serviceName
                            + "] for client ["
                            + clientName
                            + "]");
                }
            } else {
                queue = (Queue) getContext().lookupServerInput(serviceName);
                if (log.isDebugEnabled()) {
                    log.debug(
                        "["
                            + name
                            + "] got Queue for serviceName ["
                            + serviceName
                            + "] for server ["
                            + serverName
                            + "]");
                }
            }

        } catch (NamingException e) {
            throw new IfsaException(e);
        }
    }
    return queue;
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
	    
    if (isServer()) {
        queueReceiver = session.createReceiver(getServiceQueue());
    } else {
    	throw new IfsaException("Client ["+getClientName()+"] cannot act as Server");
    }
    if (log.isDebugEnabled()) {
        log.debug(
            "["
                + name
                + "] got receiver for queue ["
                + queueReceiver.getQueue().getQueueName()
                + "]"
                + " serviceName ["
                + serviceName
                + "]"
                + "serverName [" + serverName + "] "
                + ToStringBuilder.reflectionToString(queueReceiver));
    }

    return queueReceiver;
    } catch (JMSException e) {
        throw new IfsaException(e);
    }
}
/**
 * Indicates whether the object at hand represents a Client (returns <code>True</code>) or
 * a Server (returns <code>False</code>).
 */
public boolean isClient() throws IfsaException {
	boolean client = StringUtils.isNotEmpty(getClientName());
	boolean server = StringUtils.isNotEmpty(getServerName());

	if (client && server) {
        throw new IfsaException("Object ["+getName()+"] cannot be both client ["+ getClientName() + "] and server ["+ getServerName() +"]");
	}
	if (!client && !server) {
        throw new IfsaException("Object ["+getName()+"] not configured as client or server");
	}
	return client;
}
/**
 * Indicates whether the object at hand represents a Client (returns <code>False</code>) or
 * a Server (returns <code>True</code>).
 *
 * @see #isClient()
 */
public boolean isServer() throws IfsaException {
	return ! isClient();
}
public boolean isTransacted() {

    return transacted;
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
    /**
     * Sends a message,and if transacted, the queueSession is committed.
     * <p>This method is intended for <b>clients</b>, as <b>server</b>s
     * will use the <code>sendReply</code>.
     * @return the correlationID of the sent message
     */
    public TextMessage sendMessage(QueueSession session, QueueSender sender, String message)
        throws IfsaException {

	    try {
        TextMessage msg = session.createTextMessage();
        msg.setText(message);
		String replyToQueueName="-"; 
        //Client side
        if (!isClient()) {
	        throw new IfsaException("Server ["+ getServerName() + "] cannot use sendMessage, should use sendReply");
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

        log.info(
            "["+name+"] serviceName ["
                + serviceName
                + "]"
                + (StringUtils.isNotEmpty(clientName)
                    ? "clientName [" + clientName + "]"
                    : "serverName [" + serverName + "]")
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
            log.debug(
                "["+name+"]  serviceName ["
                    + serviceName
                    + "]"
                    + (StringUtils.isNotEmpty(clientName)
                        ? "clientName [" + clientName + "]"
                        : "serverName [" + serverName + "]")
                    + " committing (send) transaction");
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
            log.debug(
                "["
                    + name
                    + "] ["
                    + serverName
                    + "] service ["
                    + serviceName
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
public void setClientName(java.lang.String newClientName) {
    if (!(StringUtils.isEmpty(serverName))) {
        log.error("[" + name + "] trying to set clientName, while serverName is already set");
    }
    clientName = newClientName;
}
/**
 * The name of the path to the jndi (.bindings file).
 * Be sure to give only the path!
 */
public void setJndiPath(String newJndiPath) {
    jndiPath = newJndiPath;
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
        	throw new IllegalArgumentException(
                "illegal messageProtocol ["
                    + newMessageProtocol
                    + "] specified, it should be one of the values "
                    + IfsaMessageProtocolEnum.getNames());

        	}
        messageProtocol = IfsaMessageProtocolEnum.getEnum(newMessageProtocol);
        log.debug("["+name+"] message protocol set to "+messageProtocol.getName());

        if (messageProtocol.equals(IfsaMessageProtocolEnum.FIRE_AND_FORGET)) {
            transacted = true;
            log.debug("["+name+"] set transacted to true");
        } else {
            transacted = false;
            log.debug("["+name+"] set transacted to false");
        }
    }
public void setName(String newName) {
	name = newName;
}
    /**
     * <p>Set the name of the server.</p>
     * This also takes care that server side lookups are performed.
     * Creation date: (08-05-2003 9:03:53)
     * @param newServerName java.lang.String the URI of the server
     */
    public void setServerName(String newServerName) {
        if (!(StringUtils.isEmpty(clientName))) {
            log.error("["+name+"] trying to set serverName, while clientName is already set");
        }
        serverName = newServerName;
    }
    /**
     * set the IFSA service name
     * @param newServiceName the name of the service, e.g. IFSA://appID
     */
    public void setServiceName(String newServiceName) {
        serviceName = newServiceName;
    }
public String toString() {
    String result = super.toString();
    ToStringBuilder ts = new ToStringBuilder(this);
    ts.append("jndiPath", jndiPath);
    ts.append("serviceName", serviceName);
    ts.append("serverName", serverName);
    ts.append("clientName", clientName);
    if (messageProtocol != null)
        ts.append("messageProtocol", messageProtocol.getName());
    else
        ts.append("messageProtocol", "null!");

    ts.append("transacted", isTransacted());
    result += ts.toString();
    return result;

}
}
