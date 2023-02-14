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
package nl.nn.adapterframework.jta.narayana;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.Properties;
import java.util.Set;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.logging.log4j.Logger;

import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.jdbc.TransactionalDriver;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * {@link DataSource} implementation wrapping {@link XADataSource} and using
 * {@link ConnectionManager} to acquire connections.
 *
 */
public class NarayanaDataSource implements DataSource {
	protected Logger log = LogUtil.getLogger(this);

	private @Setter boolean connectionPooling = true;
	private @Setter int maxConnections = 20;

	private @Getter CommonDataSource targetDataSource;
	private String dbUrl;

	public NarayanaDataSource(CommonDataSource dataSource, String dbUrl) {
		this.targetDataSource = dataSource;
		this.dbUrl = dbUrl;
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
		return ConnectionManager.create(dbUrl, properties);
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
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return ((Wrapper)targetDataSource).unwrap(iface);
	}

	public String toString() {
		Set<?> connections=null;
		try {
			connections = (Set)ClassUtils.getDeclaredFieldValue(null, ConnectionManager.class,"_connections");
		} catch (IllegalArgumentException | SecurityException | IllegalAccessException | NoSuchFieldException e) {
			log.warn("could not obtain connectionPool size", e);
		}
		int connectionPoolSize = connections!=null ? connections.size() : -1;
		return "NarayanaDataSource with connection pool size "+connectionPoolSize;
	}
}
