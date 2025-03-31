/*
   Copyright 2021-2025 WeAreFrank!

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

import static java.util.Objects.requireNonNull;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.XAConnectionFactory;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.jdbc.datasource.AbstractXADataSourceFactory;
import org.frankframework.jdbc.datasource.OpenManagedDataSource;
import org.frankframework.util.AppConstants;

@Log4j2
public class NarayanaDataSourceFactory extends AbstractXADataSourceFactory {

	private @Setter NarayanaJtaTransactionManager transactionManager;
	private boolean useNativePoolingMechanism = false;

	public NarayanaDataSourceFactory() {
		// For backwards compatibility, apply these configuration constants if they're found.
		AppConstants appConstants = AppConstants.getInstance();
		minIdle = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minIdle", minIdle);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		useNativePoolingMechanism = appConstants.getBoolean("transactionmanager.narayana.jdbc.nativePoolingMechanism", false);
		maxIdle = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxIdle", maxIdle);
		maxLifeTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTime);
		testQuery = appConstants.getString("transactionmanager.narayana.jdbc.connection.testQuery", testQuery);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String dataSourceName) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper(xaDataSource, dataSourceName);
		this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);

		DataSource ds;
		if (!useNativePoolingMechanism && maxPoolSize > 1) {
			XAConnectionFactory cf = new DataSourceXAConnectionFactory(requireNonNull(transactionManager.getTransactionManager()), xaDataSource);
			PoolableConnectionFactory poolableConnectionFactory = new PoolableManagedConnectionFactory(cf, null);

			GenericObjectPool<PoolableConnection> connectionPool = createConnectionPool(poolableConnectionFactory);

			ds = new OpenManagedDataSource<>(connectionPool, cf.getTransactionRegistry());
			log.info("created XA-enabled PoolingDataSource [{}]", ds);
		} else {
			NarayanaDataSource nds = new NarayanaDataSource(xaDataSource, dataSourceName);
			nds.setConnectionPooling(useNativePoolingMechanism);
			nds.setMaxConnections(maxPoolSize);
			ds = nds;
			log.info("created XA-enabled NarayanaDataSource [{}]", ds);
		}
		log.info("registered Narayana DataSource [{}] with Transaction Manager", ds);
		return ds;
	}
}
