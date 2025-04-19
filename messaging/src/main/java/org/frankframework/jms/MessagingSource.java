/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.jms;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.IbisException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

/**
 * Generic Source for JMS connection, to be shared for JMS Objects that can use the same.
 *
 * @author  Gerrit van Brakel
 */
public class MessagingSource {
	protected Logger log = LogUtil.getLogger(this);

	private int referenceCount;
	private final boolean connectionsArePooledStore = AppConstants.getInstance().getBoolean("jms.connectionsArePooled", false);
	private final boolean sessionsArePooledStore = AppConstants.getInstance().getBoolean("jms.sessionsArePooled", false);
	private final boolean useSingleDynamicReplyQueueStore = AppConstants.getInstance().getBoolean("jms.useSingleDynamicReplyQueue", true);
	private final boolean cleanUpOnClose = AppConstants.getInstance().getBoolean("jms.cleanUpOnClose", true);
	private final boolean createDestination;

	private @Getter @Setter String authAlias;

	private final AtomicInteger openConnectionCount = new AtomicInteger();
	private final AtomicInteger openSessionCount = new AtomicInteger();

	private final @Getter String id;

	private Context context;
	private ConnectionFactory connectionFactory;
	private Connection globalConnection=null; // only used when connections are not pooled

	private final Map<String,MessagingSource> siblingMap;
	private Hashtable<Session,Connection> connectionTable; // hashtable is synchronized and does not permit nulls

	private Queue globalDynamicReplyQueue = null;

	protected MessagingSource(String id, Context context, ConnectionFactory connectionFactory, Map<String,MessagingSource> siblingMap, String authAlias, boolean createDestination) {
		referenceCount=0;
		this.id=id;
		this.context=context;
		this.connectionFactory=connectionFactory;
		this.siblingMap=siblingMap;
		siblingMap.put(id, this);
		this.authAlias=authAlias;
		this.createDestination=createDestination;
		if (connectionsArePooled()) {
			connectionTable = new Hashtable<>();
		}
		log.debug("{}set id [{}] context [{}] connectionFactory [{}] authAlias [{}]", getLogPrefix(), id, context, connectionFactory, authAlias);
	}

	public synchronized boolean close() throws IbisException {
		if (--referenceCount<=0 && cleanUpOnClose()) {
			log.debug("{}reference count [{}], cleaning up global objects", this::getLogPrefix, () -> referenceCount);
			siblingMap.remove(getId());
			try {
				deleteDynamicQueue(globalDynamicReplyQueue);
				if (globalConnection != null) {
					log.debug("{}closing global Connection", this::getLogPrefix);
					globalConnection.close();
					openConnectionCount.decrementAndGet();
				}
				if (openSessionCount.get()!=0) {
					log.warn("{}open session count after closing [{}]", this::getLogPrefix, openSessionCount::get);
				}
				if (openConnectionCount.get()!=0) {
					log.warn("{}open connection count after closing [{}]", this::getLogPrefix, openConnectionCount::get);
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
		log.debug("{}reference count [{}], no cleanup", this::getLogPrefix, () -> referenceCount);
		return false;
	}

	public synchronized void increaseReferences() {
		referenceCount++;
	}

	protected Context getContext() {
		return context;
	}

	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(authAlias)) {
			CredentialFactory cf = new CredentialFactory(authAlias);
			log.debug("using userId [{}] to create Connection", cf::getUsername);
			return connectionFactory.createConnection(cf.getUsername(),cf.getPassword());
		}
		return connectionFactory.createConnection();
	}

	private Connection createAndStartConnection() throws JMSException {
		Connection connection;
		connection = createConnection();
		openConnectionCount.incrementAndGet();
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
				openConnectionCount.decrementAndGet();
			} catch (JMSException e) {
				log.error("{}Exception closing Connection", getLogPrefix(), e);
			}
		}
	}

	public Session createSession(boolean transacted, int acknowledgeMode) throws IbisException {
		Connection connection;
		Session session;
		try {
			connection = getConnection();
		} catch (JMSException e) {
			throw new JmsException("could not obtain Connection", e);
		}
		try {
			session = connection.createSession(transacted, acknowledgeMode);
			openSessionCount.incrementAndGet();
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
				session.close();
				openSessionCount.decrementAndGet();
			} catch (JMSException e) {
				log.error("{}Exception closing Session", getLogPrefix(), e);
			} finally {
				releaseConnection(connection);
			}
			return;
		}

		try {
			log.debug("{}closing Session", this::getLogPrefix);
			session.close();
		} catch (JMSException e) {
			log.error("{}Exception closing Session", getLogPrefix(), e);
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

	private void deleteDynamicQueue(Queue queue) throws JmsException {
		if (queue!=null) {
			try {
				if (!(queue instanceof TemporaryQueue tqueue)) {
					throw new JmsException("Queue ["+queue.getQueueName()+"] is not a TemporaryQueue");
				}
				tqueue.delete();
			} catch (JMSException e) {
				throw new JmsException("cannot delete temporary queue",e);
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
					if(log.isInfoEnabled())
						log.info("{}created dynamic replyQueue [{}]", getLogPrefix(), globalDynamicReplyQueue.getQueueName());
				}
			}
			log.trace("Got global dynamic reply queue, lock released on {}", this);
			result = globalDynamicReplyQueue;
		} else {
			result = session.createTemporaryQueue();
		}
		return result;
	}

	public void releaseDynamicReplyQueue(Queue replyQueue) throws JmsException {
		if (!useSingleDynamicReplyQueue()) {
			deleteDynamicQueue(replyQueue);
		}
	}

	protected String getLogPrefix() {
		return "["+getId()+"]";
	}
}
