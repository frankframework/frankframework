/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jms;

import java.util.Hashtable;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Generic Source for JMS connection, to be shared for JMS Objects that can use the same.
 *
 * @author  Gerrit van Brakel
 */
public class MessagingSource  {
	public static final String CLOSE = "], ";
	protected Logger log = LogUtil.getLogger(this);

	private int referenceCount;
	private final boolean connectionsArePooledStore = AppConstants.getInstance().getBoolean("jms.connectionsArePooled", false);
	private final boolean sessionsArePooledStore = AppConstants.getInstance().getBoolean("jms.sessionsArePooled", false);
	private final boolean useSingleDynamicReplyQueueStore = AppConstants.getInstance().getBoolean("jms.useSingleDynamicReplyQueue", true);
	private final boolean cleanUpOnClose = AppConstants.getInstance().getBoolean("jms.cleanUpOnClose", true);
	private final boolean createDestination;
	private final boolean useJms102;

	private @Getter @Setter String authAlias;

	private final Counter openConnectionCount = new Counter(0);
	private final Counter openSessionCount = new Counter(0);

	private @Getter String id;

	private Context context = null;
	private ConnectionFactory connectionFactory = null;
	private Connection globalConnection=null; // only used when connections are not pooled

	private final Map<String,MessagingSource> siblingMap;
	private Hashtable<Session,Connection> connectionTable; // hashtable is synchronized and does not permit nulls

	private Queue globalDynamicReplyQueue = null;

	protected MessagingSource(String id, Context context, ConnectionFactory connectionFactory, Map<String,MessagingSource> siblingMap, String authAlias, boolean createDestination, boolean useJms102) {
		super();
		referenceCount=0;
		this.id=id;
		this.context=context;
		this.connectionFactory=connectionFactory;
		this.siblingMap=siblingMap;
		siblingMap.put(id, this);
		this.authAlias=authAlias;
		this.createDestination=createDestination;
		this.useJms102=useJms102;
		if (connectionsArePooled()) {
			connectionTable = new Hashtable<>();
		}
		log.debug(getLogPrefix()+"set id ["+id+"] context ["+context+"] connectionFactory ["+connectionFactory+"] authAlias ["+authAlias+"]");
	}

	public synchronized boolean close() throws IbisException {
		if (--referenceCount<=0 && cleanUpOnClose()) {
			log.debug(getLogPrefix()+"reference count ["+referenceCount+"], cleaning up global objects");
			siblingMap.remove(getId());
			try {
				deleteDynamicQueue(globalDynamicReplyQueue);
				if (globalConnection != null) {
					log.debug(getLogPrefix()+"closing global Connection");
					globalConnection.close();
					openConnectionCount.decrease();
				}
				if (openSessionCount.getValue()!=0) {
					log.warn(getLogPrefix()+"open session count after closing ["+openSessionCount.getValue()+"]");
				}
				if (openConnectionCount.getValue()!=0) {
					log.warn(getLogPrefix()+"open connection count after closing ["+openConnectionCount.getValue()+"]");
				}
				if (context != null) {
					context.close();
				}
			} catch (Exception e) {
				throw new IbisException("exception closing connection", e);
			} finally {
				globalDynamicReplyQueue=null;
				connectionFactory = null;
				globalConnection=null;
				context = null;
			}
			return true;
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"reference count ["+referenceCount+"], no cleanup");
		return false;
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

	/** The QCF is wrapped in a Spring TransactionAwareConnectionFactoryProxy, this should always be the most outer wrapped QCF. */
	private ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		if(getConnectionFactory() instanceof TransactionAwareConnectionFactoryProxy) {
			return (ConnectionFactory)ClassUtils.getDeclaredFieldValue(getConnectionFactory(), "targetConnectionFactory");
		}
		return getConnectionFactory();
	}

	/** Retrieve the 'original' ConnectionFactory, used by the console (to get the Tibco QCF) in order to display queue message count. */
	public Object getManagedConnectionFactory() {
		ConnectionFactory qcf = null;
		try {
			qcf = getConnectionFactoryDelegate();
			if (qcf instanceof PoolingConnectionFactory) { //BTM
				return ((PoolingConnectionFactory)qcf).getXaConnectionFactory();
			}
			if (qcf instanceof JmsPoolConnectionFactory) { //Narayana with pooling
				return ((JmsPoolConnectionFactory)qcf).getConnectionFactory();
			}
			if (qcf instanceof ConnectionFactoryProxy) { // Narayana without pooling
				return ClassUtils.getDeclaredFieldValue(qcf, ConnectionFactoryProxy.class, "xaConnectionFactory");
			}
			return ClassUtils.invokeGetter(qcf, "getManagedConnectionFactory", true);
		} catch (Exception e) {
			if (qcf != null) {
				return qcf;
			}
			log.warn(getLogPrefix() + "could not determine managed connection factory", e);
			return null;
		}
	}

	public String getPhysicalName() {
		StringBuilder result = new StringBuilder();

		Object managedConnectionFactory=null;
		try {
			managedConnectionFactory = getManagedConnectionFactory();
			if (managedConnectionFactory != null) {
				result.append(ToStringBuilder.reflectionToString(managedConnectionFactory, ToStringStyle.SHORT_PREFIX_STYLE));
			}
		} catch (Exception | NoClassDefFoundError e) {
			result.append(" ").append(ClassUtils.nameOf(connectionFactory)).append(".getManagedConnectionFactory() (").append(ClassUtils.nameOf(e)).append("): ").append(e.getMessage());
		}

		try {
			ConnectionFactory qcfd = getConnectionFactoryDelegate();
			if (qcfd != managedConnectionFactory) {
				result.append(getConnectionPoolInfo(qcfd));
			}
		} catch (Exception e) {
			result.append(ClassUtils.nameOf(connectionFactory)).append(".getConnectionFactoryDelegate() (").append(ClassUtils.nameOf(e)).append("): ").append(e.getMessage());
		}

		return result.toString();
	}

	/** Return pooling info if present */
	private StringBuilder getConnectionPoolInfo(ConnectionFactory qcfd) {
		StringBuilder result = new StringBuilder(" managed by [").append(ClassUtils.classNameOf(qcfd)).append(CLOSE);
		if (qcfd instanceof PoolingConnectionFactory) {
			PoolingConnectionFactory poolcf = ((PoolingConnectionFactory)qcfd);
			result.append("idle connections [").append(poolcf.getInPoolSize()).append(CLOSE);
			result.append("min poolsize ["+poolcf.getMinPoolSize()).append(CLOSE);
			result.append("max poolsize [").append(poolcf.getMaxPoolSize()).append(CLOSE);
			result.append("max idle time [").append(poolcf.getMaxIdleTime()).append(CLOSE);
			result.append("max life time [").append(poolcf.getMaxLifeTime()).append(CLOSE);
		}
		if (qcfd instanceof JmsPoolConnectionFactory) {
			JmsPoolConnectionFactory poolcf = ((JmsPoolConnectionFactory)qcfd);
			result.append("idle connections [").append(poolcf.getNumConnections()).append(CLOSE);
			result.append("max connections [").append(poolcf.getMaxConnections()).append(CLOSE);
			result.append("max sessions per connection [").append(poolcf.getMaxSessionsPerConnection()).append(CLOSE);
			result.append("block if session pool is full [").append(poolcf.isBlockIfSessionPoolIsFull()).append(CLOSE);
			result.append("block if session pool is full timeout [").append(poolcf.getBlockIfSessionPoolIsFullTimeout()).append(CLOSE);
			result.append("connection check interval (ms) [").append(poolcf.getConnectionCheckInterval()).append(CLOSE);
			result.append("connection idle timeout (s) [").append(poolcf.getConnectionIdleTimeout() / 1000).append(CLOSE);
		}
		return result;
	}

	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(authAlias)) {
			CredentialFactory cf = new CredentialFactory(authAlias);
			if (log.isDebugEnabled()) log.debug("using userId ["+cf.getUsername()+"] to create Connection");
			if (useJms102()) {
				if (connectionFactory instanceof QueueConnectionFactory) {
					return ((QueueConnectionFactory)connectionFactory).createQueueConnection(cf.getUsername(),cf.getPassword());
				}
				return ((TopicConnectionFactory)connectionFactory).createTopicConnection(cf.getUsername(),cf.getPassword());
			}
			return connectionFactory.createConnection(cf.getUsername(),cf.getPassword());
		}
		if (useJms102()) {
			if (connectionFactory instanceof QueueConnectionFactory) {
				return ((QueueConnectionFactory)connectionFactory).createQueueConnection();
			}
			return ((TopicConnectionFactory)connectionFactory).createTopicConnection();
		}
		return connectionFactory.createConnection();
	}

	private Connection createAndStartConnection() throws JMSException {
		Connection connection;
		// do not log, as this may happen very often
//		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"creating Connection, openConnectionCount before ["+openConnectionCount.getValue()+"]");
		connection = createConnection();
		openConnectionCount.increase();
		connection.start();
		return connection;
	}

	private Connection getConnection() throws JMSException {
		if (connectionsArePooled()) {
			return createAndStartConnection();
		}
		log.trace("Get/create global connection - synchronize (lock) on {}", this);
		synchronized (this) {
			if (globalConnection == null) {
				globalConnection = createAndStartConnection();
			}
		}
		log.trace("Got global connection, lock released on {}", this);
		return globalConnection;
	}

	private void releaseConnection(Connection connection) {
		if (connection != null && connectionsArePooled()) {
			try {
				connection.close();
				openConnectionCount.decrease();
			} catch (JMSException e) {
				log.error(getLogPrefix()+"Exception closing Connection", e);
			}
		}
	}

	public Session createSession(boolean transacted, int acknowledgeMode) throws IbisException {
		Connection connection=null;
		Session session;
		try {
			connection = getConnection();
		} catch (JMSException e) {
			throw new JmsException("could not obtain Connection", e);
		}
		try {
			// do not log, as this may happen very often
//			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"creating Session, openSessionCount before ["+openSessionCount.getValue()+"]");
			if (useJms102()) {
				if (connection instanceof QueueConnection) {
					session = ((QueueConnection)connection).createQueueSession(transacted, acknowledgeMode);
				} else {
					session = ((TopicConnection)connection).createTopicSession(transacted, acknowledgeMode);
				}
			} else {
				session = connection.createSession(transacted, acknowledgeMode);
			}
			openSessionCount.increase();
			if (connectionsArePooled()) {
				connectionTable.put(session,connection);
			}
			return session;
		} catch (JMSException e) {
			releaseConnection(connection);
			throw new JmsException("could not create Session", e);
		}
	}

	public void releaseSession(Session session) {
		if (session == null) {
			return;
		}
		if (connectionsArePooled()) {
			Connection connection = connectionTable.remove(session);
			try {
				// do not log, as this may happen very often
//					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"closing Session, openSessionCount will become ["+(openSessionCount.getValue()-1)+"]");
					session.close();
					openSessionCount.decrease();
				} catch (JMSException e) {
					log.error(getLogPrefix()+"Exception closing Session", e);
				} finally {
					releaseConnection(connection);
				}
			return;
		}

		try {
			if (log.isDebugEnabled()) log.debug(getLogPrefix() + "closing Session");
			session.close();
		} catch (JMSException e) {
			log.error(getLogPrefix() + "Exception closing Session", e);
		}
	}

	protected boolean connectionsArePooled() {
		return connectionsArePooledStore;
	}

	public boolean sessionsArePooled() {
		return sessionsArePooledStore;
	}

	protected boolean useSingleDynamicReplyQueue() {
		if (connectionsArePooled()) {
			return false; // dynamic reply queues are connection-based.
		}
		return useSingleDynamicReplyQueueStore;
	}

	public boolean cleanUpOnClose() {
		return cleanUpOnClose;
	}

	public boolean createDestination() {
		return createDestination;
	}

	public boolean useJms102() {
		return useJms102;
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

	public Queue getDynamicReplyQueue(Session session) throws JMSException {
		Queue result;
		if (useSingleDynamicReplyQueue()) {
			log.trace("Get/create global dynamic reply queue, synchronize (lock) on {}", this);
			synchronized (this) {
				if (globalDynamicReplyQueue==null) {
					globalDynamicReplyQueue=session.createTemporaryQueue();
					log.info(getLogPrefix()+"{} created dynamic replyQueue ["+globalDynamicReplyQueue.getQueueName()+"]");
				}
			}
			log.trace("Got global dynamic reply queue, lock released on {}", this);
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
		return "["+getId()+ CLOSE;
	}
}