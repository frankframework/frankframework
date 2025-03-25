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

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.tm.XAResourceWrapper;
import org.springframework.util.Assert;

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
	private final XADataSource xaDataSource;

	private XAConnection xaConnection;
	private XAResource delegate;

	/**
	 * Create a new {@link DataSourceXAResourceRecoveryHelper} instance.
	 * @param xaDataSource the XA data source
	 */
	public DataSourceXAResourceRecoveryHelper(XADataSource xaDataSource, String name) {
		Assert.notNull(xaDataSource, "XADataSource must not be null");
		this.xaDataSource = xaDataSource;
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
		if (connect()) {
			return new XAResourceWrapper[] { this };
		}
		return NO_XA_RESOURCES;
	}

	private boolean connect() {
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

	private XAConnection getXaConnection() throws SQLException {
		return xaDataSource.getXAConnection();
	}

	@Override
	public Xid[] recover(int flag) throws XAException {
		try {
			return getDelegate(true).recover(flag);
		}
		finally {
			if (flag == XAResource.TMENDRSCAN) {
				disconnect();
			}
		}
	}

	private void disconnect() {
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

	@Override
	public void start(Xid xid, int flags) throws XAException {
		getDelegate(true).start(xid, flags);
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		getDelegate(true).end(xid, flags);
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		return getDelegate(true).prepare(xid);
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		getDelegate(true).commit(xid, onePhase);
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		getDelegate(true).rollback(xid);
	}

	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return getDelegate(true).isSameRM(xaResource);
	}

	@Override
	public void forget(Xid xid) throws XAException {
		getDelegate(true).forget(xid);
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return getDelegate(true).getTransactionTimeout();
	}

	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		return getDelegate(true).setTransactionTimeout(seconds);
	}

	private XAResource getDelegate(boolean required) {
		Assert.state(delegate != null || !required, "Connection has not been opened");
		return delegate;
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

}
