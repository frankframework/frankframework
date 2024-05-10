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
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.util.AppConstants;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Factory through which Pooling DataSources can be retrieved. This Fractory should basically only be used on
 * application servers like Tomcat that do not provide a JTA environment (resources with an XA transaction manager and
 * corresponding connection pool). This class should not be used directly when an XA transaction manager is required but
 * can be extended for this purpuse (see {@link NarayanaDataSourceFactory}).
 * </p>
 * 
 * <p>
 * For Tomcat a connection pool will be created only when the DataSource retrieved from JNDI is not already wrapped by
 * a connection pool by Tomcat. Whether this is the case depends on how the Resource is configured in the context.xml.
 * In Tomcat the Resource can also be configured to return an {@link XADataSource} instead of a {@link DataaSource}. An
 * {@link XADataSource} should normally be configured with a transaction manager and a corresponding connection pool by
 * the application server. For Tomcat this is not the case but {@link NarayanaDataSourceFactory}, that extends this
 * class, can be used in that case. When this class is used directly with an {@link XADataSource} it will create
 * a connection pool anyway allowing it to be used in a non-XA way. This will make it possible to have only one Resource
 * configured which can be used by both XA transaction managers (e.g. Narayana, see {@link NarayanaDataSourceFactory})
 * and non-XA transaction managers (e.g. {@link DataSourceTransactionManager} that can be configures with this class as
 * pooling DataSource factory on an {@link XADataSource}).
 * </p>
 * 
 * <p>
 * Already created DataSources are stored in a ConcurrentHashMap.
 * Every DataSource can be augmented before it is added.
 * </p>
 * 
 * @see XADataSourceWrapper
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
		return dataSource.getClass().getName().startsWith("org.apache.tomcat");
	}

	protected DataSource createPool(DataSource dataSource, String dataSourceName) {
		if (isPooledDataSource(dataSource)) {
			log.warn("DataSource [{}] already implements pooling. Will not be wrapped with DBCP2 pool. Frank!Framework connection pooling configuration is ignored, configure pooling properties in the JNDI Resource to avoid issues.", dataSourceName);
			return dataSource;
		}
		if (dataSource instanceof XADataSource) {
			// See Javadoc of XADataSourceWrapper
			dataSource = new XADataSourceWrapper((XADataSource)dataSource);
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
