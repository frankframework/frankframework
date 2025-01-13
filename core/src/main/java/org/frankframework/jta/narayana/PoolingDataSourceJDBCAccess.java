/*
   Copyright 2023 WeAreFrank!

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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp2.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.DataSourceConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.PoolingDataSource;
import org.apache.tomcat.dbcp.pool2.ObjectPool;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.springframework.jndi.JndiTemplate;

import com.arjuna.ats.arjuna.exceptions.FatalError;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.accessors.DataSourceJDBCAccess;

import lombok.extern.log4j.Log4j2;

/**
 * Alternative to {@link DataSourceJDBCAccess} that adds connection pooling, instead of doing a JNDI lookup each time a connection is called.
 * {@link #getConnection()} will be called for every transaction, pooling will drastically improve performance.
 *
 * @author Niels Meijer
 */
@Log4j2
public class PoolingDataSourceJDBCAccess implements JDBCAccess {
	private DataSource datasource;

	private DataSource augmentDataSource(DataSource dataSource) {
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);
		return new PoolingDataSource<>(connectionPool);
	}

	private ObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		poolableConnectionFactory.setRollbackOnReturn(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(1);
		connectionPool.setMaxTotal(10);
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);
		log.info("created connection pool [{}]", connectionPool);
		return connectionPool;
	}

	/** Must be a 'new' connection with autocommit set to false. Implementations should close the connection. */
	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = datasource.getConnection();
		connection.setAutoCommit(false);
		return connection;
	}

	/**
	 * Since we've already verified the connection in {@link NarayanaConfigurationBean}
	 * we can almost be certain this won't fail. In case such a thing does happen,
	 * throw a fatal exception because we may absolutely not continue initializing the application.
	 */
	@Override
	public void initialise(StringTokenizer stringTokenizer) {
		String jndiName = stringTokenizer.nextToken();
		try {
			JndiTemplate locator = new JndiTemplate();
			DataSource dataSource = locator.lookup(jndiName, DataSource.class);
			this.datasource = augmentDataSource(dataSource);
		} catch (NamingException e) {
			throw new FatalError("unable to lookup datasource ["+jndiName+"]", e);
		}
	}
}
