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

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedDataSource;
import org.apache.commons.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp2.managed.XAConnectionFactory;
import org.apache.commons.pool2.ObjectPool;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory {

	private @Setter NarayanaJtaTransactionManager transactionManager;

	public NarayanaDataSourceFactory() {
		// For backwards compatibility, apply these configuration constants if they're found.
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdle = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTime);
		testQuery = appConstants.getString("transactionmanager.narayana.jdbc.connection.testQuery", testQuery);
	}

	@Override
	protected DataSource createXAPool(XADataSource xaDataSource, String dataSourceName) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper(xaDataSource);
		this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

		DataSource ds;
		if(maxPoolSize > 1) {
			XAConnectionFactory cf = new DataSourceXAConnectionFactory(transactionManager.getTransactionManager(), xaDataSource);
			PoolableConnectionFactory poolableConnectionFactory = new PoolableManagedConnectionFactory(cf, null);

			ObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

			ds = new ManagedDataSource<>(connectionPool, cf.getTransactionRegistry());
			log.info("created XA-enabled PoolingDataSource [{}]", ds);
		} else {
			ds = new NarayanaDataSource(xaDataSource, dataSourceName);
			log.info("registered Narayana DataSource [{}] with Transaction Manager", ds);
		}
		return ds;
	}
}
