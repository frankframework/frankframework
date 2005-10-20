/*
 * $Log: ConnectionBase.java,v $
 * Revision 1.1  2005-10-20 15:34:11  europe\L190409
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
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;

import nl.nn.adapterframework.core.IbisException;

import org.apache.log4j.Logger;

/**
 * Generic JMS connection, to be shared for JMS Objects that can use the same. 
 * 
 * @author  Gerrit van Brakel
 * @version Id
 */
public class ConnectionBase  {
	public static final String version="$RCSfile: ConnectionBase.java,v $ $Revision: 1.1 $ $Date: 2005-10-20 15:34:11 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private int referenceCount;
	
	private String id;
	
	private Context context = null;
	private ConnectionFactory connectionFactory = null;
	private Connection connection=null;
	
	private HashMap connectionMap;
	
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
				if (connection != null) { 
					connection.close();
				}
				if (context != null) {
					context.close(); 
				}
			} catch (Exception e) {
				throw new IbisException("exception closing connection", e);
			} finally {
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

	protected Connection getConnection() throws IbisException {
		if (connection == null) {
			synchronized (this) {
				if (connection == null) {
					try {
						connection = createConnection();
						connection.start();
					} catch (JMSException e) {
						throw new IbisException("could not obtain Connection", e);
					}
				}
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

	protected String getLogPrefix() {
		return "["+getId()+"] "; 
	}

	public String getId() {
		return id;
	}



}
