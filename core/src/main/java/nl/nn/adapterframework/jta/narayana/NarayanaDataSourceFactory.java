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

import java.sql.Connection;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp2.managed.XAConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory {

	private @Getter @Setter int minPoolSize = 0;
	private @Getter @Setter int maxPoolSize = 20;
	private @Getter @Setter int maxLifeTime = 0;

	public NarayanaDataSourceFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		maxLifeTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTime);
	}

	private @Setter NarayanaJtaTransactionManager transactionManager;

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if(dataSource instanceof XADataSource) {
			XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper((XADataSource) dataSource);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

			if(maxPoolSize > 1) {
				return XAPool((XADataSource) dataSource);
			}

			NarayanaDataSource ds = new NarayanaDataSource(dataSource, dataSourceName);
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

		PoolingDataSource<PoolableConnection> ds = new ManagedDataSource<>(connectionPool, cf.getTransactionRegistry());
		log.info("created XA-enabled PoolingDataSource [{}]", ds);
		return ds;
	}

	private ObjectPool<PoolableConnection> createConnectionPool(PoolableConnectionFactory poolableConnectionFactory) {
		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		poolableConnectionFactory.setMaxConnLifetimeMillis((maxLifeTime > 0) ? maxLifeTime * 1000 : -1);
		poolableConnectionFactory.setRollbackOnReturn(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(minPoolSize);
		connectionPool.setMaxTotal(maxPoolSize);
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);
		return connectionPool;
	}
}
