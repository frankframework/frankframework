/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.narayana;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory implements DisposableBean {
	private NarayanaRecoveryManager recoveryManager;

	@Override
	protected DataSource augment(CommonDataSource dataSource, String dataSourceName) {
		XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper((XADataSource) dataSource);
		this.recoveryManager.registerXAResourceRecoveryHelper(recoveryHelper);

		return new NarayanaDataSource((XADataSource) dataSource);
//		ConnectionFactory connectionFactory = new DataSourceConnectionFactory(new NarayanaDataSource((XADataSource) dataSource));
//		PoolableConnectionFactory poolFactory = new PoolableConnectionFactory(connectionFactory, null);
//		GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
//		config.setMaxTotal(100);
//		GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(poolFactory, config);
//		poolFactory.setPool(pool);
//		return new PoolingDataSource<>(pool);
	}

	@Override
	public void destroy() throws Exception {
		for(DataSource dataSource : objects.values()) {
//			try {
//				((BasicManagedDataSource) dataSource).close();
//				ConnectionManager.remove((ConnectionImple) dataSource.getConnection());
//			} catch (SQLException e) {
				// ignore the connection if it cannot create/have a connection
//			}
		}
	}

	@Autowired
	public void setRecoveryManager(NarayanaRecoveryManager recoveryManager) {
		this.recoveryManager = recoveryManager;
	}
}
