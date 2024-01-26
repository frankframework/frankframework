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

import java.sql.Connection;
import java.time.Duration;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp2.managed.XAConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.frankframework.jdbc.datasource.OpenManagedDataSource;
import org.frankframework.jndi.JndiDataSourceFactory;
import org.frankframework.util.AppConstants;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NarayanaDataSourceFactory extends JndiDataSourceFactory {

	private @Getter @Setter int minPoolSize = 0;
	private @Getter @Setter int maxPoolSize = 20;
	private @Getter @Setter int maxLifeTimeSeconds = 0;

	public NarayanaDataSourceFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		maxLifeTimeSeconds = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTimeSeconds);
	}

	private @Setter NarayanaJtaTransactionManager transactionManager;

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if(dataSource instanceof XADataSource) {
			XADataSource xaDataSource = (XADataSource) dataSource;
			XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper(xaDataSource);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

			if(maxPoolSize > 1) {
				return XAPool(xaDataSource);
			}

			NarayanaDataSource ds = new NarayanaDataSource(xaDataSource, dataSourceName);
			log.info("registered Narayana DataSource [{}] with Transaction Manager", ds);
			return ds;
		}

		log.info("DataSource [{}] is not XA enabled, unable to register with an Transaction Manager", dataSourceName);
		return pool((DataSource) dataSource);
	}

	private DataSource pool(DataSource dataSource) {
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

		PoolingDataSource<PoolableConnection> ds = new PoolingDataSource<>(connectionPool);
		log.info("created PoolingDataSource [{}]", ds);
		return ds;
	}

	private DataSource XAPool(XADataSource dataSource) {
		XAConnectionFactory cf = new DataSourceXAConnectionFactory(transactionManager.getTransactionManager(), dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableManagedConnectionFactory(cf, null);

		ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

		OpenManagedDataSource<PoolableConnection> ds = new OpenManagedDataSource<>(connectionPool, cf.getTransactionRegistry());
		log.info("created XA-enabled PoolingDataSource [{}]", ds);
		return ds;
	}

	private ObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		if (maxLifeTimeSeconds > 0) {
			poolableConnectionFactory.setMaxConn(Duration.ofSeconds(maxLifeTimeSeconds));
		}
		poolableConnectionFactory.setRollbackOnReturn(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(minPoolSize);
		connectionPool.setMaxTotal(maxPoolSize);
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);
		log.info("created connectionPool [{}]", connectionPool);
		return connectionPool;
	}
}
