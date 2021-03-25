/*
 * Copyright 2012-2016 the original author or authors, 2021 WeAreFrank!
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.nn.adapterframework.narayana;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.jdbc.TransactionalDriver;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link DataSource} implementation wrapping {@link XADataSource} and using
 * {@link ConnectionManager} to acquire connections.
 * 
 * TransactionalDriver.poolConnections defaults to true
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaDataSource implements DataSource {

	private final XADataSource xaDataSource;
	private final boolean connectionPooling = true;
	private final int maxConnections = 100;

	/**
	 * Create a new {@link NarayanaDataSource} instance.
	 * @param xaDataSource the XA DataSource
	 */
	public NarayanaDataSource(XADataSource xaDataSource) {
		Assert.notNull(xaDataSource, "XADataSource must not be null");
		this.xaDataSource = xaDataSource;
	}

	@Override
	public Connection getConnection() throws SQLException {
		Properties properties = new Properties();
		properties.put(TransactionalDriver.XADataSource, this.xaDataSource);
		properties.setProperty(TransactionalDriver.poolConnections, ""+connectionPooling);
		properties.setProperty(TransactionalDriver.maxConnections, ""+maxConnections);
		return ConnectionManager.create(null, properties);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Properties properties = new Properties();
		properties.put(TransactionalDriver.XADataSource, this.xaDataSource);
		properties.put(TransactionalDriver.userName, username);
		properties.put(TransactionalDriver.password, password);
		properties.setProperty(TransactionalDriver.poolConnections, ""+connectionPooling);
		properties.setProperty(TransactionalDriver.maxConnections, ""+maxConnections);
		return ConnectionManager.create(null, properties);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return this.xaDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		this.xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return this.xaDataSource.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (isWrapperFor(iface)) {
			return (T) this;
		}
		if (ClassUtils.isAssignableValue(iface, this.xaDataSource)) {
			return (T) this.xaDataSource;
		}
		throw new SQLException(getClass() + " is not a wrapper for " + iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass());
	}

}
