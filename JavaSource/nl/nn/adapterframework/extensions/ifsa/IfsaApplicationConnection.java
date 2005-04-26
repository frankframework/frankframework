/*
 * $Log: IfsaApplicationConnection.java,v $
 * Revision 1.1  2005-04-26 09:36:16  L190409
 * introduction of IfsaApplicationConnection
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.util.HashMap;
import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

import com.ing.ifsa.IFSAContext;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAQueueConnectionFactory;
/**
 * 
 * 
 * @author Gerrit van Brakel
 * @version Id
 */
public class IfsaApplicationConnection  {
	public static final String version="$Id: IfsaApplicationConnection.java,v 1.1 2005-04-26 09:36:16 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private final static String IFSA_INITIAL_CONTEXT_FACTORY="com.ing.ifsa.IFSAContextFactory";
	private final static String IFSA_PROVIDER_URL="IFSA APPLICATION BUS";
	
	private int referenceCount;
	
	static private HashMap connectionMap = new HashMap();

	private String applicationId;
	private IFSAContext context = null;
	private IFSAQueueConnectionFactory ifsaQueueConnectionFactory = null;
	private QueueConnection queueConnection=null;
	
	private IfsaApplicationConnection(String applicationId) {
		super();
		referenceCount=0;
		this.applicationId = applicationId;
	}

	public static synchronized IfsaApplicationConnection getConnection(String applicationId) throws IfsaException {
		IfsaApplicationConnection result = (IfsaApplicationConnection)connectionMap.get(applicationId);
		if (result != null) {
			result = new IfsaApplicationConnection(applicationId);
			connectionMap.put(applicationId, result);
		}
		result.referenceCount++;
		return result;
	}
	
	public synchronized void close() throws IfsaException
	{
		if (--referenceCount<=0) {
			log.debug(getLogPrefix()+" reference count ["+referenceCount+"], closing connection");
			connectionMap.remove(this);
			try {
				if (queueConnection != null) { 
					queueConnection.close();
				}
				if (context != null) {
					context.close(); 
				}
			} catch (Exception e) {
				throw new IfsaException(getLogPrefix()+"exception closing connection", e);
			} finally {
				ifsaQueueConnectionFactory = null;
				queueConnection=null;
				context = null;
			}
		} else {
			log.debug(getLogPrefix()+" not closing, reference count ["+referenceCount+"]");
		}
		
	}

	private synchronized IFSAContext getContext() throws IfsaException {
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
			throw new IfsaException(getLogPrefix()+"could not obtain context", e);
		}
	}

	private synchronized IFSAQueueConnectionFactory getIfsaQueueConnectionFactory() throws IfsaException {
	
		try {	     
			if (ifsaQueueConnectionFactory == null) {
		
				ifsaQueueConnectionFactory =
					(IFSAQueueConnectionFactory) getContext().lookupBusConnection(applicationId);
		
				if (log.isDebugEnabled()) {
					log.debug("IfsaConnection for application ["+applicationId+"] got ifsaQueueConnectionFactory with properties:" 
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
			throw new IfsaException(getLogPrefix(),e);
		}
	}

	private synchronized QueueConnection getQueueConnection() throws IfsaException {
		if (queueConnection == null) {
			try {
				queueConnection = getIfsaQueueConnectionFactory().createQueueConnection();
				queueConnection.start();
			} catch (JMSException e) {
				throw new IfsaException(getLogPrefix(),e); 
			}
		}
		return queueConnection;
	}

	public QueueSession createQueueSession(boolean transacted, int acknowledgeMode) throws IfsaException, JMSException {
		return getQueueConnection().createQueueSession(transacted, acknowledgeMode);
	}

	private boolean hasStaticReplyQueue() throws IfsaException {
		/*
		 * if we don't know if we're using a dynamic reply queue, we can
		 * check this using the function IsClientTransactional
		 * Yes -> we're using a static reply queue
		 * No -> dynamic reply queue
		 */
		return getIfsaQueueConnectionFactory().IsClientTransactional();  // Static
	}
	
	/**
	 * Retrieves the reply queue for a <b>client</b> connection. If the
	 * client is transactional the replyqueue is retrieved from IFSA,
	 * otherwise a temporary (dynamic) queue is created.
	 */
	public Queue getClientReplyQueue(QueueSession session) throws IfsaException {
		Queue replyQueue = null;
	
		try {
			/*
			 * if we don't know if we're using a dynamic reply queue, we can
			 * check this using the function IsClientTransactional
			 * Yes -> we're using a static reply queue
			 * No -> dynamic reply queue
			 */
			if (hasStaticReplyQueue()) { // Static
				replyQueue = (Queue) getContext().lookupReply(getApplicationId());
				log.debug("got static reply queue [" +replyQueue.getQueueName()+"]");            
			} else { // Temporary Dynamic
				replyQueue =  session.createTemporaryQueue();
				log.debug("got dynamic reply queue [" +replyQueue.getQueueName()+"]");
			}
			return replyQueue;
		} catch (Exception e) {
			throw new IfsaException(getLogPrefix(),e);
		}
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
	
		QueueReceiver queueReceiver;
		    
		String correlationId;
		Queue replyQueue;
		try {
			correlationId=sentMessage.getJMSCorrelationID();
			replyQueue=(Queue)sentMessage.getJMSReplyTo();
		} catch (JMSException e) {
			throw new IfsaException(getLogPrefix(),e);
		}
		
		try {
	
			if (hasStaticReplyQueue()) {
				String selector="JMSCorrelationID='" + correlationId + "'";
				queueReceiver = session.createReceiver(replyQueue, selector);
				log.debug("** transactional client - selector ["+selector+"]");
			} else {
				queueReceiver = session.createReceiver(replyQueue);
				log.debug("** non-transactional client" );
			}	
		} catch (JMSException e) {
			throw new IfsaException(getLogPrefix(),e);
		}
		return queueReceiver;
	}

  	public IFSAQueue lookupService(String serviceId) throws IfsaException {
		try {
			return (IFSAQueue) getContext().lookupService(serviceId);
		} catch (NamingException e) {
			throw new IfsaException(getLogPrefix()+"cannot lookup queue for service ["+serviceId+"]");
		}
  	}
  	
	public IFSAQueue lookupProviderInput() throws IfsaException {
		try {
			return (IFSAQueue) getContext().lookupProviderInput();
		} catch (NamingException e) {
			throw new IfsaException(getLogPrefix()+"cannot lookup provider queue");
		}
	}
	
	protected String getLogPrefix() {
		return "["+getApplicationId()+"] "; 
	}
	

	public String getApplicationId() {
		return applicationId;
	}

}
