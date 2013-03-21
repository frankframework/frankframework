/*
   Copyright 2013 Nationale-Nederlanden

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
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Generic Source for JMS connection, to be shared for JMS Objects that can use the same. 
 * 
 * @author  Gerrit van Brakel
 * @version $Id$
 */
public class MessagingSource  {
	protected Logger log = LogUtil.getLogger(this);

	private int referenceCount;
	private boolean connectionsArePooledStore = AppConstants.getInstance().getBoolean("jms.connectionsArePooled", false);
	private boolean sessionsArePooledStore = AppConstants.getInstance().getBoolean("jms.sessionsArePooled", false);
	private boolean useSingleDynamicReplyQueueStore = AppConstants.getInstance().getBoolean("jms.useSingleDynamicReplyQueue", true);
	private boolean cleanUpOnClose = AppConstants.getInstance().getBoolean("jms.cleanUpOnClose", true);
	private boolean createDestination;
	private boolean useJms102;

	private String authAlias;

	private Counter openConnectionCount = new Counter(0);
	private Counter openSessionCount = new Counter(0);
	
	private String id;
	
	private Context context = null;
	private ConnectionFactory connectionFactory = null;
	private Connection globalConnection=null; // only used when connections are not pooled
	
	private Map siblingMap;
	private Hashtable connectionTable; // hashtable is synchronized and does not permit nulls

	private Queue globalDynamicReplyQueue = null;
	
	protected MessagingSource(String id, Context context,
			ConnectionFactory connectionFactory, Map siblingMap,
			String authAlias, boolean createDestination, boolean useJms102) {
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
			connectionTable = new Hashtable();
		}
		log.debug(getLogPrefix()+"set id ["+id+"] context ["+context+"] connectionFactory ["+connectionFactory+"] authAlias ["+authAlias+"]");
	}
		
	public synchronized boolean close() throws IbisException
	{
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
		} else {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"reference count ["+referenceCount+"], no cleanup");
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

	protected ConnectionFactory getConnectionFactoryDelegate() throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		return getConnectionFactory();
	}

	public String getPhysicalName() { 
		String result="";
		try {
			ConnectionFactory qcf = getConnectionFactoryDelegate();
			Object managedConnectionFacory;
			try {
				managedConnectionFacory = ClassUtils.invokeGetter(qcf, "getManagedConnectionFactory", true);
			} catch (Exception e) {
				// In case of BTM.
				managedConnectionFacory = ClassUtils.invokeGetter(qcf, "getResource", true);
			}
			result+=ClassUtils.reflectionToString(managedConnectionFacory, "perties"); //catches properties as well as Properties... 
			// result+=ClassUtils.reflectionToString(qcf, "factory");
			if (result.contains("activemq")) {
				result += "[" + ClassUtils.invokeGetter(managedConnectionFacory,"getBrokerURL",true) + "]";
			}
		} catch (Exception e) {
			result+= ClassUtils.nameOf(connectionFactory)+".getManagedConnectionFactory() "+ClassUtils.nameOf(e)+": "+e.getMessage();
		}
		return result;
	}
	
	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(authAlias)) {
			CredentialFactory cf = new CredentialFactory(authAlias,null,null);
			if (log.isDebugEnabled()) log.debug("using userId ["+cf.getUsername()+"] to create Connection");
			if (useJms102()) {
				if (connectionFactory instanceof QueueConnectionFactory) {
					return ((QueueConnectionFactory)connectionFactory).createQueueConnection(cf.getUsername(),cf.getPassword());
				} else {
					return ((TopicConnectionFactory)connectionFactory).createTopicConnection(cf.getUsername(),cf.getPassword());
				}
			} else {
				return connectionFactory.createConnection(cf.getUsername(),cf.getPassword());
			}
		}
		if (useJms102()) {
			if (connectionFactory instanceof QueueConnectionFactory) {
				return ((QueueConnectionFactory)connectionFactory).createQueueConnection();
			} else {
				return ((TopicConnectionFactory)connectionFactory).createTopicConnection();
			}
		} else {
			return connectionFactory.createConnection();
		}
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
		} else {
			synchronized (this) {
				if (globalConnection == null) {
					globalConnection = createAndStartConnection();
				}
			}
			return globalConnection;
		}
	}

	private void releaseConnection(Connection connection) {
		if (connection != null && connectionsArePooled()) {
			try {
				// do not log, as this may happen very often
//				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"closing Connection, openConnectionCount will become ["+(openConnectionCount.getValue()-1)+"]");
				connection.close();
				openConnectionCount.decrease();
			} catch (JMSException e) {
				log.error(getLogPrefix()+"Exception closing Connection", e);
			}
		}
	}

	public Session createSession(boolean transacted, int acknowledgeMode) throws IbisException {
		Connection connection=null;;
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
		if (session != null) {
			if (connectionsArePooled()) {
				Connection connection = (Connection)connectionTable.remove(session);
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
			} else {
				try {
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"closing Session");
					session.close();
				} catch (JMSException e) {
					log.error(getLogPrefix()+"Exception closing Session", e);
				}
			}
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

	public Queue getDynamicReplyQueue(QueueSession session) throws JMSException {
		Queue result;
		if (useSingleDynamicReplyQueue()) {
			synchronized (this) {
				if (globalDynamicReplyQueue==null) {
					globalDynamicReplyQueue=session.createTemporaryQueue();
					log.info(getLogPrefix()+"created dynamic replyQueue ["+globalDynamicReplyQueue.getQueueName()+"]");
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


	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

}
