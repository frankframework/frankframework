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

import java.sql.Connection;
import java.time.Duration;

import javax.sql.CommonDataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.dbcp.dbcp2.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.DataSourceConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.frankframework.util.AppConstants;

/**
 * Factory through which (TX-enabled) Pooling DataSources can be retrieved.
 *
 * Already created DataSources are stored in a ConcurrentHashMap.
 * Every DataSource can be augmented before it is added.
 */
public class PoolingDataSourceFactory extends DataSourceFactory {

	@Getter @Setter protected int minIdle = 0;
	@Getter @Setter protected int maxPoolSize = 20;
	@Getter @Setter protected int maxIdle = 2;
	@Getter @Setter protected int maxLifeTime = 0;
	@Getter @Setter protected int connectionCheckInterval = 300;
	@Getter @Setter protected String testQuery = null;

	public PoolingDataSourceFactory() {
		super();
		AppConstants appConstants = AppConstants.getInstance();
		minIdle = appConstants.getInt("transactionmanager.jdbc.connection.minIdle", minIdle);
		maxPoolSize = appConstants.getInt("transactionmanager.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdle = appConstants.getInt("transactionmanager.jdbc.connection.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("transactionmanager.jdbc.connection.maxLifeTime", maxLifeTime);
		connectionCheckInterval = appConstants.getInt("transactionmanager.jdbc.connection.checkInterval", connectionCheckInterval);
		testQuery = appConstants.getString("transactionmanager.jdbc.connection.testQuery", testQuery);
	}

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if(maxPoolSize > 1) {
			log.info("Creating connection pool for datasource [{}]", dataSourceName);
			return createPool((DataSource)dataSource, dataSourceName);
		}
		log.info("Pooling not configured, using datasource [{}] without augmentation", dataSourceName);
		return (DataSource) dataSource;
	}

	private static boolean isPooledDataSource(CommonDataSource dataSource) {
		return dataSource instanceof ConnectionPoolDataSource
				|| dataSource.getClass().getName().startsWith("org.apache.tomcat")
				;
	}

	protected DataSource createPool(DataSource dataSource, String dataSourceName) {
		if (isPooledDataSource(dataSource)) {
			log.warn("DataSource [{}] already implements pooling. Will not be wrapped with DBCP2 pool. Frank!Framework connection pooling configuration is ignored, configure pooling properties in the JNDI Resource to avoid issues.", dataSourceName);
			return dataSource;
		}
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		GenericObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);
		OpenPoolingDataSource<PoolableConnection> ds = new OpenPoolingDataSource<>(connectionPool);
		log.info("registered PoolingDataSource [{}]", ds);
		return ds;
	}

	protected GenericObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
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
		connectionPool.setMinIdle(minIdle);
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
