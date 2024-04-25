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
package org.frankframework.ibistesttool.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.jndi.PoolingJndiDataSourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Prevent PostgreSQL from throwing the following exception when Ladybug stores a report:
 * <code>org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.</code>
 * </p>
 * 
 * <p>
 * Configuring a transaction manager also disables auto commit but then an independent transaction manager needs to be
 * configured to prevent interference with other transactions / prevent following error when running TestIAF Larva tests
 * with Narayana:
 * <code>You cannot commit during a managed transaction.</code>
 * </p>
 * 
 *<p>
 * With propagation = Propagation.REQUIRES_NEW this error is also gone but then tests fail with:
 * <code>Commit not allowed by transaction service.</code>
 * </p>
 * 
 * <p>
 * When using qualifier ladybug (<code>@Transactional("ladybug")</code>) on the DatabaseStorage class to have an
 * independent transaction manager still some TestIAF Larva tests (related to JMS) fail (looks like the transaction
 * manager configured for Ladybug is somehow being used elsewhere too when using Narayana).
 * </p>
 * 
 * <p>
 * Hence using this NoAutoCommitDataSource class and no transaction manager is a simpler solution.
 * </p>
 * 
 * @author Jaco de Groot
 */
public class AutoCommitDisabledDataSource implements DataSource {
	protected @Autowired PoolingJndiDataSourceFactory poolingJndiDataSourceFactory;
	protected @Autowired String dataSourceName;
	protected DataSource dataSource;

	@PostConstruct
	public void init() throws NamingException {
		// File storage will be used instead of database storage when dataSourceName is empty (see
		// springIbisTestTool.xml) but Spring will still wire bean dataSource (this class) to bean scheduler (which will
		// ignore it, see SchedulerFactoryBean.setDataSource()). Prevent poolingJndiDataSourceFactory from throwing an
		// exception when dataSourceName is empty
		if (StringUtils.isNotEmpty(dataSourceName)) {
			dataSource = poolingJndiDataSourceFactory.getDataSource(dataSourceName);
		}
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return dataSource.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return dataSource.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return dataSource.isWrapperFor(iface);
	}

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = dataSource.getConnection();
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
		return connection;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		Connection connection = dataSource.getConnection(username, password);
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
		return connection;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		dataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		dataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

}
