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
package nl.nn.adapterframework.jndi;

import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Factory through which (TX-enabled) DataSources can be retrieved.
 *
 * Already created DataSources are stored in a ConcurrentHashMap.
 * Every DataSource can be augmented before it is added.
 */
public class JndiDataSourceFactory extends JndiObjectFactory<DataSource,CommonDataSource> implements IDataSourceFactory {

	public static final String DEFAULT_DATASOURCE_NAME_PROPERTY = "jdbc.datasource.default";
	public static final String GLOBAL_DEFAULT_DATASOURCE_NAME = AppConstants.getInstance().getProperty(DEFAULT_DATASOURCE_NAME_PROPERTY);
	@Getter @Setter protected int minPoolSize = 0;
	@Getter @Setter protected int maxPoolSize = 20;
	@Getter @Setter protected int maxIdle = 2;
	@Getter @Setter protected int maxLifeTime = 0;
	@Getter @Setter protected int connectionCheckInterval = 300;
	@Getter @Setter protected String testQuery = null;

	public JndiDataSourceFactory() {
		super(CommonDataSource.class);
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdle = appConstants.getInt("transactionmanager.jdbc.connection.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("transactionmanager.jdbc.connection.maxLifeTime", maxLifeTime);
		connectionCheckInterval = appConstants.getInt("transactionmanager.jdbc.connection.checkInterval", connectionCheckInterval);
		testQuery = appConstants.getString("transactionmanager.jdbc.connection.testQuery", testQuery);
	}

	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if (dataSource instanceof XADataSource) {
			return createXAPool((XADataSource) dataSource, dataSourceName);
		}

		log.info("DataSource [{}] is not XA enabled, unable to register with an Transaction Manager", dataSourceName);
		if(maxPoolSize > 1) {
			return createPool((DataSource)dataSource);
		}
		return (DataSource) dataSource;
	}

	protected DataSource createXAPool(XADataSource xaDataSource, String dataSourceName) {
		throw new UnsupportedOperationException("Creating XA DataSources not created by [" + this.getClass().getName() + "]");
	}

	protected DataSource createPool(DataSource dataSource) {
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);
		org.apache.commons.dbcp2.PoolingDataSource<PoolableConnection> ds = new org.apache.commons.dbcp2.PoolingDataSource<>(connectionPool);
		log.info("registered PoolingDataSource [{}]", ds);
		return ds;
	}

	/**
	 * Ensure that the outermost DataSource proxy/wrapper in the chain is the {@link TransactionAwareDataSourceProxy}.
	 * Otherwise the {@link TransactionAwareDataSourceProxy} will NOT work!
	 */
	@Override
	protected final DataSource augment(CommonDataSource dataSource, String dataSourceName) {
		return new TransactionalDbmsSupportAwareDataSourceProxy(augmentDatasource(dataSource, dataSourceName));
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return get(dataSourceName);
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return get(dataSourceName, jndiEnvironment);
	}


	@Override
	public List<String> getDataSourceNames() {
		return new ArrayList<>(objects.keySet());
	}

	protected ObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		if (maxLifeTime > 0) {
			poolableConnectionFactory.setMaxConn(Duration.ofSeconds(maxLifeTime));
		}
		poolableConnectionFactory.setRollbackOnReturn(true);
		if (StringUtils.isNotBlank(testQuery)) {
			poolableConnectionFactory.setValidationQuery(testQuery);
			poolableConnectionFactory.setValidationQueryTimeout(Duration.ofSeconds(5));
		}
		poolableConnectionFactory.setFastFailValidation(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(minPoolSize);
		connectionPool.setMaxTotal(maxPoolSize);
		connectionPool.setMaxIdle(maxIdle);
		connectionPool.setTestOnBorrow(true);
		connectionPool.setTestWhileIdle(true);
		if (connectionCheckInterval > 0) {
			connectionPool.setDurationBetweenEvictionRuns(Duration.ofSeconds(connectionCheckInterval));
		}
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);
		return connectionPool;
	}
}