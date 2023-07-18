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

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactory extends JndiDataSourceFactory {

	private @Getter @Setter int minPoolSize = 0;
	private @Getter @Setter int maxPoolSize = 20;
	private @Getter @Setter int maxIdleTime = 60;
	private @Getter @Setter int maxLifeTime = 0;

	public NarayanaDataSourceFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdleTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxIdleTime", maxIdleTime);
		maxLifeTime = appConstants.getInt("transactionmanager.narayana.jdbc.connection.maxLifeTime", maxLifeTime);
	}

	private @Setter NarayanaJtaTransactionManager transactionManager;

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		DataSource ds = registerWithTM(dataSource, dataSourceName);

		if(maxPoolSize > 1) {
			return pool(ds, dataSourceName);
		}
		return ds;
	}

	private DataSource registerWithTM(CommonDataSource dataSource, String dataSourceName) {
		if(dataSource instanceof XADataSource) {
			XAResourceRecoveryHelper recoveryHelper = new DataSourceXAResourceRecoveryHelper((XADataSource) dataSource);
			this.transactionManager.registerXAResourceRecoveryHelper(recoveryHelper);
			NarayanaDataSource ds = new NarayanaDataSource(dataSource, dataSourceName);
			log.info("registered BTM DataSource [{}] with Transaction Manager", ds);
			return ds;
		}

		log.info("DataSource [{}] is not XA enabled, unable to register with an Transaction Manager", dataSourceName);
		return (DataSource) dataSource;
	}

	private DataSource pool(DataSource dataSource, String dataSourceName) {
		HikariConfig config = new HikariConfig();
		config.setRegisterMbeans(false);
		config.setMaxLifetime(maxLifeTime);
		config.setIdleTimeout(maxIdleTime);
		config.setMaximumPoolSize(maxPoolSize);
		config.setMinimumIdle(minPoolSize);
		config.setDataSource(dataSource);
		config.setPoolName(dataSourceName);

		HikariDataSource poolingDataSource = new HikariDataSource(config);
		log.info("created Hikari pool [{}]", poolingDataSource);
		return poolingDataSource;
	}
}
