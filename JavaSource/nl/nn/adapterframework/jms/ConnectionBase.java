/*
 * $Log: ConnectionBase.java,v $
 * Revision 1.4  2005-11-02 09:40:52  europe\L190409
 * made useSingleDynamicReplyQueue configurable from appConstants
 *
 * Revision 1.3  2005/10/26 08:18:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * pulled dynamic reply code out of IfsaConnection up to here
 *
 * Revision 1.2  2005/10/24 15:11:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made sessionsArePooled configurable via appConstant 'jms.sessionsArePooled'
 *
 * Revision 1.1  2005/10/20 15:34:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed JmsConnection into ConnectionBase
 *
 * Revision 1.3  2005/10/18 06:58:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.2  2005/10/18 06:57:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close returns true when underlying connection is acually closed
 *
 * Revision 1.1  2005/05/03 15:59:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of shared connection code
 *
 */
package nl.nn.adapterframework.jms;

import java.util.HashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.util.AppConstants;

import org.apache.log4j.Logger;

/**
 * Generic JMS connection, to be shared for JMS Objects that can use the same. 
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class ConnectionBase  {
	public static final String version="$RCSfile: ConnectionBase.java,v $ $Revision: 1.4 $ $Date: 2005-11-02 09:40:52 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private int referenceCount;
	private final static String SESSIONS_ARE_POOLED_KEY="jms.sessionsArePooled";
	private static Boolean sessionsArePooledStore=null; 
	private final static String USE_SINGLE_DYNAMIC_REPLY_QUEUE_KEY="jms.useSingleDynamicReplyQueue";
	private static Boolean useSingleDynamicReplyQueueStore=null; 

	
	private String id;
	
	private Context context = null;
	private ConnectionFactory connectionFactory = null;
	private Connection connection=null;
	
	private HashMap connectionMap;

	private Queue globalDynamicReplyQueue = null;
	
	protected ConnectionBase(String id, Context context, ConnectionFactory connectionFactory, HashMap connectionMap) {
		super();
		referenceCount=0;
		this.id=id;
		this.context=context;
		this.connectionFactory=connectionFactory;
		this.connectionMap=connectionMap;
		connectionMap.put(id, this);
		log.debug("set id ["+id+"] context ["+context+"] connectionFactory ["+connectionFactory+"] ");
	}
		
	public synchronized boolean close() throws IbisException
	{
		if (--referenceCount<=0) {
			log.debug(getLogPrefix()+" reference count ["+referenceCount+"], closing connection");
			connectionMap.remove(getId());
			try {
				deleteDynamicQueue(globalDynamicReplyQueue);
				if (connection != null) { 
					connection.close();
				}
				if (context != null) {
					context.close(); 
				}
			} catch (Exception e) {
				throw new IbisException("exception closing connection", e);
			} finally {
				globalDynamicReplyQueue=null;
				connectionFactory = null;
				connection=null;
				context = null;
				return true;
			}
		} else {
			log.debug(getLogPrefix()+" not closing, reference count ["+referenceCount+"]");
			return false;
		}
	}

	public synchronized void increaseReferences() {
		referenceCount++;
	}
	
	public synchronized void decreaseReferences() {
		referenceCount--;
	}

	public Context getContext() {
		return context;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	
	protected Connection createConnection() throws JMSException {
		if (connectionFactory instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory)connectionFactory).createQueueConnection();
		} else {
			return ((TopicConnectionFactory)connectionFactory).createTopicConnection();
		}
	}

	protected synchronized Connection getConnection() throws IbisException {
		if (connection == null) {
			try {
				connection = createConnection();
				connection.start();
			} catch (JMSException e) {
				throw new IbisException("could not obtain Connection", e);
			}
		}
		return connection;
	}


	public Session createSession(boolean transacted, int acknowledgeMode) throws IbisException {
		Connection connection = getConnection();
		try {
			if (connection instanceof QueueConnection) {
				return ((QueueConnection)connection).createQueueSession(transacted, acknowledgeMode);
			} else {
				return ((TopicConnection)connection).createTopicSession(transacted, acknowledgeMode);
			}
		} catch (JMSException e) {
			throw new IbisException("could not create Session", e);
		}
	}

	public synchronized boolean sessionsArePooled() {
		if (sessionsArePooledStore==null) {
			boolean pooled=AppConstants.getInstance().getBoolean(SESSIONS_ARE_POOLED_KEY, false);
			sessionsArePooledStore = new Boolean(pooled);
		}
		return sessionsArePooledStore.booleanValue();
	}

	protected synchronized boolean useSingleDynamicReplyQueue() {
		if (useSingleDynamicReplyQueueStore==null) {
			boolean useSingleQueue=AppConstants.getInstance().getBoolean(USE_SINGLE_DYNAMIC_REPLY_QUEUE_KEY, true);
			useSingleDynamicReplyQueueStore = new Boolean(useSingleQueue);
		}
		return useSingleDynamicReplyQueueStore.booleanValue();
	}


	private void deleteDynamicQueue(Queue queue) throws IfsaException {
		if (queue!=null) {
			try {
				if (!(queue instanceof TemporaryQueue)) {
					throw new IfsaException("Queue ["+queue.getQueueName()+"] is not a TemporaryQueue");
				}
				TemporaryQueue tqueue = (TemporaryQueue)queue;
				tqueue.delete();
			} catch (JMSException e) {
				throw new IfsaException("cannot delete temporary queue",e);
			}
		}
	}

	public Queue getDynamicReplyQueue(QueueSession session) throws JMSException {
		Queue result;
		if (useSingleDynamicReplyQueue()) {
			synchronized (this) {
				if (globalDynamicReplyQueue==null) {
					globalDynamicReplyQueue=session.createTemporaryQueue();
					log.info("created dynamic replyQueue ["+globalDynamicReplyQueue.getQueueName()+"]");
				}
			}
			result = globalDynamicReplyQueue;
		} else {
			result = session.createTemporaryQueue();
		}
		return result;
	}
	
	public void releaseDynamicReplyQueue(Queue replyQueue) throws IfsaException {
		if (!useSingleDynamicReplyQueue()) {
			deleteDynamicQueue(replyQueue);
		}
	}


	protected String getLogPrefix() {
		return "["+getId()+"] "; 
	}

	public String getId() {
		return id;
	}



}
