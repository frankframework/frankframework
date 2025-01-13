/*
   Copyright 2021-2023 WeAreFrank!

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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;

import com.arjuna.ats.internal.jdbc.ConnectionImple;
import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.internal.jdbc.drivers.modifiers.IsSameRMModifier;
import com.arjuna.ats.internal.jdbc.drivers.modifiers.ModifierFactory;
import com.arjuna.ats.jdbc.TransactionalDriver;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.LogUtil;

/**
 * {@link DataSource} implementation wrapping {@link XADataSource} because Narayana doesn't provide their own DataSource.
 *
 * Bypasses the {@link TransactionalDriver} in order to create connections and
 * uses the {@link ConnectionManager} directly in order to acquire {@link XADataSource} connections.
 *
 * {@link ConnectionImple} requires an {@link XADataSource}
 *
 */
public class NarayanaDataSource implements DataSource {
	private final Logger log = LogUtil.getLogger(NarayanaDataSource.class);

	private @Setter boolean connectionPooling = true;
	private @Setter int maxConnections = 20;

	private final @Getter XADataSource targetDataSource;
	private final String name;

	public NarayanaDataSource(@Nonnull XADataSource dataSource, String name) {
		this.targetDataSource = dataSource;
		this.name = name;

		checkModifiers(dataSource);
	}

	/** In order to allow transactions to run over multiple databases, see {@link ModifierFactory}. */
	private void checkModifiers(XADataSource dataSource) {
		try (Connection connection = dataSource.getXAConnection().getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			String driverName = metadata.getDriverName();
			int major = metadata.getDriverMajorVersion();
			int minor = metadata.getDriverMinorVersion();

			if (ModifierFactory.getModifier(driverName, major, minor)==null) {
				log.info("No Modifier found for driver [{}] version [{}.{}], creating IsSameRM modifier", driverName, major, minor);
				ModifierFactory.putModifier(driverName, major, minor, IsSameRMModifier.class.getName());
			}
		} catch (SQLException e) {
			log.warn("Could not check for existence of Modifier", e);
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(null, null);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Properties properties = new Properties();
		properties.put(TransactionalDriver.XADataSource, getTargetDataSource());
		if (username!=null) {
			properties.put(TransactionalDriver.userName, username);
		}
		if (password!=null) {
			properties.put(TransactionalDriver.password, password);
		}
		properties.setProperty(TransactionalDriver.poolConnections, ""+connectionPooling);
		properties.setProperty(TransactionalDriver.maxConnections, ""+maxConnections);
		return ConnectionManager.create(name, properties);
	}


	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return targetDataSource.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return targetDataSource.getLoginTimeout();
	}

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return targetDataSource.getParentLogger();
	}

	@Override
	public void setLogWriter(PrintWriter writer) throws SQLException {
		targetDataSource.setLogWriter(writer);
	}

	@Override
	public void setLoginTimeout(int timeout) throws SQLException {
		targetDataSource.setLoginTimeout(timeout);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this) || ((Wrapper)targetDataSource).isWrapperFor(iface);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return ((Wrapper)targetDataSource).unwrap(iface);
	}
}
