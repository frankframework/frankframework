/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

/**
 * <p>
 * Wrap {@link XADataSource} and expose it as a {@link DataSource} allowing it to get a {@link Connection} from the
 * {@link XAConnection}. This should only be done after careful consideration as an {@link XAConnection} is supposed to
 * be managed by an XA transaction manager with corresponding connection pool. See also
 * {@link PoolingDataSourceFactory}.
 * </p>
 * 
 * <p>
 * When databases are configured in Tomcat to return an {@link XADataSource} not all of them (like PostgreSQL and DB2)
 * return a class that also implements {@link DataSource}. Oracle does return a class that is both an
 * {@link XADataSource} and a {@link DataSource} and it's ojdbc8-19.3.0.0.jar allows to both call
 * {@link DataSource#getConnection()} and {@link XADataSource#getXAConnection()}, but with version 23.4.0.24.05 (and
 * possible earlier versions) a call to
 * oracle.jdbc.datasource.impl.OracleConnectionPoolDataSource.getConnection(OracleConnectionPoolDataSource.java:243)
 * will throw a java.sql.SQLException: ORA-17023: Unsupported feature https://docs.oracle.com/error-help/db/ora-17023/.
 * This class will allow {@link PoolingDataSourceFactory} to get the connection from
 * {@link XADataSource#getXAConnection()} (which will prevent this exception).
 * </p>
 * 
 * @author Jaco de Groot
 */
public class XADataSourceWrapper implements DataSource {
	XADataSource xaDataSource;

	XADataSourceWrapper(XADataSource xaDataSource) {
		this.xaDataSource = xaDataSource;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return xaDataSource.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return xaDataSource.getXAConnection().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return xaDataSource.getXAConnection(username, password).getConnection();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return xaDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		xaDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		xaDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return xaDataSource.getLoginTimeout();
	}

}
