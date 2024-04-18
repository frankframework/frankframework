/*
   Copyright 2021-2024 WeAreFrank!

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

import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.frankframework.jndi.AbstractXADataSourceFactory;
import org.frankframework.util.AppConstants;

@Log4j2
public class NarayanaDataSourceFactory extends AbstractXADataSourceFactory {

	private @Setter NarayanaJtaTransactionManager transactionManager;

	public NarayanaDataSourceFactory() {
		// For backwards compatibility, apply these configuration constants if they're found.
		AppConstants appConstants = AppConstants.getInstance();
		minIdle = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minIdle", minIdle);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdle = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTime);
		testQuery = appConstants.getString("transactionmanager.narayana.jdbc.connection.testQuery", testQuery);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String dataSourceName) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper(xaDataSource);
		this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

		DataSource ds;
		if(maxPoolSize > 1) {
			// Library 'commons-dbcp2' is used to create a connection pool, but not switched to jakarta version yet.
			log.warn("Could not create XA-enabled PoolingDataSource, because 'commons-dbcp2' is not yet switched to 'jakarta'.");
//			XAConnectionFactory cf = new DataSourceXAConnectionFactory(transactionManager.getTransactionManager(), xaDataSource);
//			PoolableConnectionFactory poolableConnectionFactory = new PoolableManagedConnectionFactory(cf, null);

//			GenericObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

//			ds = new OpenManagedDataSource<>(connectionPool, cf.getTransactionRegistry());
//			log.info("created XA-enabled PoolingDataSource [{}]", ds);
		}
//		else {
			ds = new NarayanaDataSource(xaDataSource, dataSourceName);
//		}
//		log.info("registered Narayana DataSource [{}] with Transaction Manager", ds);
		return ds;
	}
}
