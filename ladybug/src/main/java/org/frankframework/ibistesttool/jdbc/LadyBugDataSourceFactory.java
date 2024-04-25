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

import java.sql.Connection;
import java.time.Duration;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.frankframework.jdbc.datasource.ObjectFactoryBase;
import org.frankframework.jdbc.datasource.OpenPoolingDataSource;
import org.frankframework.util.AppConstants;

import lombok.Getter;
import lombok.Setter;

/**
 * The Ladybug uses it's own factory so it can be in control over it's connections.
 * Is only capable of non-xa DataSources.
 */
public class LadyBugDataSourceFactory extends ObjectFactoryBase<DataSource> {

	@Getter @Setter protected int minIdle = 0;
	@Getter @Setter protected int maxPoolSize = 10;
	@Getter @Setter protected int maxIdle = 2;
	@Getter @Setter protected int maxLifeTime = 0;
	@Getter @Setter protected int connectionCheckInterval = 300;
	@Getter @Setter protected String testQuery = null;

	protected LadyBugDataSourceFactory(Class<DataSource> lookupClass) {
		super(lookupClass);

		AppConstants appConstants = AppConstants.getInstance();
		minIdle = appConstants.getInt("ibistesttool.jdbc.minIdle", minIdle);
		maxPoolSize = appConstants.getInt("ibistesttool.jdbc.maxPoolSize", maxPoolSize);
		maxIdle = appConstants.getInt("ibistesttool.jdbc.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("ibistesttool.jdbc.maxLifeTime", maxLifeTime);
		connectionCheckInterval = appConstants.getInt("ibistesttool.jdbc.checkInterval", connectionCheckInterval);
		testQuery = appConstants.getString("ibistesttool.jdbc.testQuery", testQuery);
	}

	@Override
	protected DataSource augment(DataSource dataSource, String dataSourceName) {
		if(maxPoolSize > 1) {
			log.info("Creating connection pool for datasource [{}]", dataSourceName);
			return createPool(dataSource, dataSourceName);
		}
		log.info("Pooling not configured, using datasource [{}] without augmentation", dataSourceName);
		return dataSource;
	}

	private static boolean isPooledDataSource(DataSource dataSource) {
		return dataSource instanceof ConnectionPoolDataSource
				|| dataSource.getClass().getName().startsWith("org.apache.tomcat")
				;
	}

	private DataSource createPool(DataSource dataSource, String dataSourceName) {
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

	private GenericObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
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

	public DataSource getDataSource(String dataSourceName) {
		return get(dataSourceName, null);
	}
}
