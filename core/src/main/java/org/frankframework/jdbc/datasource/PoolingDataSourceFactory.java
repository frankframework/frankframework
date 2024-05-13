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
import javax.sql.XADataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.dbcp.dbcp2.ConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.DataSourceConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.util.AppConstants;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Factory through which Pooling DataSources can be retrieved. This Factory should basically only be used on
 * application servers like Tomcat that do not provide a JTA environment (resources with an XA transaction manager and
 * corresponding connection pool). This class should not be used directly when an XA transaction manager is required but
 * can be extended for this purpose (see {@link NarayanaDataSourceFactory}).
 * </p>
 * 
 * <p>
 * For Tomcat a connection pool will be created only when the DataSource retrieved from JNDI is not already wrapped by
 * a connection pool by Tomcat. Whether this is the case depends on how the Resource is configured in the context.xml.
 * In Tomcat the Resource can also be configured to return an {@link XADataSource} instead of a {@link DataaSource}. An
 * {@link XADataSource} should normally be configured with a transaction manager and a corresponding connection pool by
 * the application server. For Tomcat this is not the case but {@link NarayanaDataSourceFactory}, that extends this
 * class, can be used in that case. Apart from that this class can be used with an {@link XADataSource} and optionally
 * create a connection pool anyway allowing it to be used in a non-XA way. This will make it possible to have only one
 * Resource configured which can be used by both XA transaction managers (e.g. Narayana, see
 * {@link NarayanaDataSourceFactory}) and non-XA transaction managers (e.g. {@link DataSourceTransactionManager} that
 * can be configured with this class as pooling DataSource factory). But do note that this should only be done in a
 * non-JTA environment.
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

	/**
	 * Set to true to pool even when the data source is an {@link XADataSource}. Use with care as an
	 * {@link XADataSource} is supposed to be part of a JTA environment that does the pooling. Basically, only use it
	 * in a non-JTA environment where the database driver is configured to provide an {@link XADataSource} instead of
	 * {@link DataSource}. A {@link DataSource} is preferred when there is no reason to configure an
	 * {@link XADataSource}) but otherwise this property can be set to true to pool the {@link XADataSource} and use it
	 * with for example {@link DataSourceTransactionManager}.
	 */
	@Getter @Setter protected boolean poolXA = false;
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
			return createPool(dataSource, dataSourceName);
		}
		log.info("Pooling not configured, using datasource [{}] without augmentation", dataSourceName);
		return (DataSource) dataSource;
	}

	private boolean isPooledDataSource(CommonDataSource dataSource) {
		// ConnectionPoolDataSource doesn't necessary mean that the connections are pooled. In a JTA environment this
		// should be the case but a database driver configured in Tomcat can provide a DataSource that implements
		// ConnectionPoolDataSource while Tomcat isn't configured to provide one
		return (dataSource instanceof ConnectionPoolDataSource && !isPoolXA())
				|| dataSource.getClass().getName().startsWith("org.apache.tomcat");
	}

	protected DataSource createPool(CommonDataSource dataSource, String dataSourceName) {
		if (isPooledDataSource(dataSource)) {
			log.debug("DataSource [{}] already implements pooling. Will not be wrapped with DBCP2 pool. Frank!Framework connection pooling configuration is ignored, configure pooling properties in the JNDI Resource to avoid issues.", dataSourceName);
			if (dataSource instanceof XADataSource xadatasource) {
				// Should only be the case in a JTA environment, hence pooling should be done by the application
				// server. This DataSource should be used in combination with JtaTransactionManager and not with
				// DataSourceTransactionManager which doesn't make use of the JTA environment. PoolingDataSourceFactory
				// would not even need to play a role in this setup, as the pooled DataSource can be retrieved from JNDI
				// directly, but as it is wired the same in all environments it can be part of it, hence only proxy the
				// DataSource
				return new XADataSourceWrapper(xadatasource);
			} else {
				// This is typically the case when the Resource in Tomcat is configured in a way that Tomcat provides
				// a connection pool
				return (DataSource)dataSource;
			}
		}
		ConnectionFactory cf = null;
		if (dataSource instanceof XADataSource xadatasource) {
			// Should only be the case in a non-JTA environment where poolXA is explicitly set to true making it
			// possible to use a Resource in Tomcat that will return an XADataSource with for example
			// DataSourceTransactionManager (which should only be used in a non-JTA environment). See also the Javadoc
			// on setPoolXA() and XADataSourceWrapper
			cf = new DataSourceConnectionFactory(new XADataSourceWrapper(xadatasource));
		} else {
			// This is typically the case when the Resource in Tomcat is not configured in a way that Tomcat provides
			// a connection pool
			cf = new DataSourceConnectionFactory((DataSource)dataSource);
		}
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
