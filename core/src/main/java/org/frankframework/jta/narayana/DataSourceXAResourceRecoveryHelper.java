/*
   Copyright 2012-2016 the original author or authors, 2021-2025 WeAreFrank!

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
package org.frankframework.jta.narayana;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.narayana.jta.jms.XAResourceConsumer;
import org.jboss.narayana.jta.jms.XAResourceFunction;
import org.jboss.tm.XAResourceWrapper;
import org.springframework.util.Assert;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

/**
 * XAResourceRecoveryHelper implementation which gets XIDs, which needs to be recovered, from the database.
 * See org.springframework.boot.jta.narayana.DataSourceXAResourceRecoveryHelper.
 *
 * Required as we wrap the connection in a pooling-capable factory, and do not use the native Narayana connection factory.
 *
 * Additionally this also implements `XAResourceWrapper`, which (AFAIK) only adds debug info.
 * See XAResourceRecord#getJndiName()
 *
 * @author Gytis Trikleris
 * @author Niels Meijer
 */
public class DataSourceXAResourceRecoveryHelper implements XAResourceRecoveryHelper, XAResource, XAResourceWrapper {

	private static final XAResource[] NO_XA_RESOURCES = {};

	private static final Log logger = LogFactory.getLog(DataSourceXAResourceRecoveryHelper.class);

	private final String name;

	private final ConnectionManager connectionManager;

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource, String name) {
		Assert.notNull(xaDataSource, "XADataSource must not be null");
		this.connectionManager = new ConnectionManager(xaDataSource);
		this.name = name;
	}

	@Override
	public boolean initialise(String properties) {
		return true;
	}

	/**
	 * If Database connection was created successfully, returns an array with one instance of DataSourceXAResourceRecoveryHelper. Otherwise,
	 * returns an empty array.
	 * @return Array with one instance of DataSourceXAResourceRecoveryHelper or an empty array
	 */
	@Override
	public XAResource[] getXAResources() {
		if (connectionManager.connect()) {
			return new XAResourceWrapper[] { this };
		}
		return NO_XA_RESOURCES;
	}

	@Override
	public Xid[] recover(int flag) throws XAException {
		try {
			return connectionManager.connectAndApply(delegate -> delegate.recover(flag));
		}
		finally {
			if (flag == XAResource.TMENDRSCAN) {
				connectionManager.disconnect();
			}
		}
	}

	@Override
	public void start(Xid xid, int flags) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.start(xid, flags));
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.end(xid, flags));
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.prepare(xid));
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.commit(xid, onePhase));
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.rollback(xid));
	}

	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.isSameRM(xaResource));
	}

	@Override
	public void forget(Xid xid) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.forget(xid));
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return connectionManager.connectAndApply(XAResource::getTransactionTimeout);
	}

	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.setTransactionTimeout(seconds));
	}

	@Override
	public XAResource getResource() {
		return this;
	}

	@Override
	public String getProductName() {
		return null;
	}

	@Override
	public String getProductVersion() {
		return null;
	}

	@Override
	public String getJndiName() {
		return name;
	}

	/**
	 * Based on the JMS ConnectionManager class from the Narayana code base.
	 *
	 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
	 * @author Tim van der Leeuw
	 */
	static class ConnectionManager {

		private final XADataSource xaDataSource;

		private XAConnection xaConnection;
		private XAResource delegate;

		ConnectionManager(XADataSource xaDataSource) {
			this.xaDataSource = xaDataSource;
		}

		/**
		 * Invoke {@link XAResourceConsumer} accept method before making sure that JMS connection is available. Current
		 * connection is used if one is available. If connection is not available, new connection is created before the
		 * accept call and closed after it.
		 *
		 * @param consumer {@link XAResourceConsumer} to be executed.
		 * @throws XAException if JMS connection cannot be created.
		 */
		void connectAndAccept(XAResourceConsumer consumer) throws XAException {
			if (isConnected()) {
				consumer.accept(delegate);
				return;
			}

			connect();
			try {
				consumer.accept(delegate);
			} finally {
				disconnect();
			}
		}

		/**
		 * Invoke {@link XAResourceFunction} apply method before making sure that JMS connection is available. Current
		 * connection is used if one is available. If connection is not available, new connection is created before the
		 * apply call and closed after it.
		 *
		 * @param function {@link XAResourceFunction} to be executed.
		 * @param <T> Return type of the {@link XAResourceFunction}.
		 * @return The result of {@link XAResourceFunction}.
		 * @throws XAException if JMS connection cannot be created.
		 */
		<T> T connectAndApply(XAResourceFunction<T> function) throws XAException {
			if (isConnected()) {
				return function.apply(delegate);
			}

			connect();
			try {
				return function.apply(delegate);
			} finally {
				disconnect();
			}
		}

		boolean connect() {
			if (delegate == null) {
				try {
					xaConnection = getXaConnection();
					delegate = xaConnection.getXAResource();
				}
				catch (SQLException ex) {
					logger.warn("Failed to create connection", ex);
					return false;
				}
			}
			return true;
		}

		void disconnect() {
			try {
				xaConnection.close();
			}
			catch (SQLException e) {
				logger.warn("Failed to close connection", e);
			} finally {
				xaConnection = null;
				delegate = null;
			}
		}

		/**
		 * Check if JMS connection is active.
		 *
		 * @return {@code true} if JMS connection is active.
		 */
		boolean isConnected() {
			return xaConnection != null;
		}

		private XAConnection getXaConnection() throws SQLException {
			return xaDataSource.getXAConnection();
		}
	}
}
