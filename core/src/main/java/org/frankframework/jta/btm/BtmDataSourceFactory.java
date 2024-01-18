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
package org.frankframework.jta.btm;

import java.sql.Connection;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.frankframework.jdbc.dbms.OpenPoolingDataSource;
import org.frankframework.jndi.JndiDataSourceFactory;
import org.frankframework.util.AppConstants;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.Getter;
import lombok.Setter;

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
			return createPool((DataSource)dataSource);
		}
		return (DataSource) dataSource;
	}

	private DataSource createPool(DataSource dataSource) {
		ConnectionFactory cf = new DataSourceConnectionFactory(dataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(cf, null);

		poolableConnectionFactory.setAutoCommitOnReturn(false);
		poolableConnectionFactory.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		poolableConnectionFactory.setMaxConnLifetimeMillis((maxLifeTime > 0) ? maxLifeTime * 1000L : -1);
		poolableConnectionFactory.setRollbackOnReturn(true);
		GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
		connectionPool.setMinIdle(minPoolSize);
		connectionPool.setMaxTotal(maxPoolSize);
		connectionPool.setBlockWhenExhausted(true);
		poolableConnectionFactory.setPool(connectionPool);

		OpenPoolingDataSource<PoolableConnection> ds = new OpenPoolingDataSource<>(connectionPool);
		log.info("registered PoolingDataSource [{}]", ds);
		return ds;
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

		log.info("registered BTM DataSource [{}]", result);
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
