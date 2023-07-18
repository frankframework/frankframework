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
package nl.nn.adapterframework.jta.btm;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

public class BtmDataSourceFactory extends JndiDataSourceFactory implements DisposableBean {

	private @Getter @Setter int minPoolSize = 0;
	private @Getter @Setter int maxPoolSize = 20;
	private @Getter @Setter int maxIdleTime = 60;
	private @Getter @Setter int maxLifeTime = 0;
	private @Getter @Setter String testQuery = null;

	public BtmDataSourceFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt("transactionmanager.btm.jdbc.connection.minPoolSize", minPoolSize);
		maxPoolSize = appConstants.getInt("transactionmanager.btm.jdbc.connection.maxPoolSize", maxPoolSize);
		maxIdleTime = appConstants.getInt("transactionmanager.btm.jdbc.connection.maxIdleTime", maxIdleTime);
		maxLifeTime = appConstants.getInt("transactionmanager.btm.jdbc.connection.maxLifeTime", maxLifeTime);
		testQuery = appConstants.getString("transactionmanager.btm.jdbc.connection.testQuery", testQuery);
	}

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if (dataSource instanceof XADataSource) {
			return createXAPool((XADataSource) dataSource, dataSourceName);
		}

		log.info("DataSource [{}] is not XA enabled, unable to register with an Transaction Manager", dataSourceName);
		if(maxPoolSize > 1) {
			return createPool((DataSource)dataSource, dataSourceName);
		}
		return (DataSource) dataSource;
	}

	private DataSource createPool(DataSource dataSource, String dataSourceName) {
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

	private DataSource createXAPool(XADataSource dataSource, String dataSourceName) {
		PoolingDataSource result = new PoolingDataSource();
		result.setUniqueName(dataSourceName);
		result.setMinPoolSize(minPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		result.setMaxIdleTime(maxIdleTime);
		result.setMaxLifeTime(maxLifeTime);

		if(StringUtils.isNotBlank(testQuery)) {
			result.setTestQuery(testQuery);
		}
		result.setEnableJdbc4ConnectionTest(true); //Assume everything uses JDBC4. BTM will test if isValid exists, to avoid unnecessary 'future' calls.

		result.setAllowLocalTransactions(true);
		result.setXaDataSource(dataSource);
		result.init();

		log.info("registered BTM DataSource [{}] with Transaction Manager", result);
		return result;
	}

	@Override
	// implementation is necessary, because PoolingDataSource does not implement AutoCloseable
	public synchronized void destroy() throws Exception {
		for (DataSource dataSource : objects.values()) {
			DataSource originalDataSource = getOriginalDataSource(dataSource);
			if(originalDataSource instanceof PoolingDataSource) {
				((PoolingDataSource) originalDataSource).close();
			}
		}
		super.destroy();
	}

	private DataSource getOriginalDataSource(DataSource dataSource) {
		if(dataSource instanceof DelegatingDataSource) {
			return getOriginalDataSource(((DelegatingDataSource) dataSource).getTargetDataSource());
		}
		return dataSource;
	}
}
