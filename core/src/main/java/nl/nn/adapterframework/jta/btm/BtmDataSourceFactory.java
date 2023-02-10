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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.Getter;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.util.AppConstants;

public class BtmDataSourceFactory extends JndiDataSourceFactory implements DisposableBean {

	private @Getter int minPoolSize=0;
	private @Getter int maxPoolSize=20;
	private @Getter int maxIdleTime=60;
	private @Getter int maxLifeTime=0;

	public BtmDataSourceFactory() {
		AppConstants appConstants = AppConstants.getInstance();
		minPoolSize = appConstants.getInt(MIN_POOL_SIZE_PROPERTY, minPoolSize);
		maxPoolSize = appConstants.getInt(MIN_POOL_SIZE_PROPERTY, maxPoolSize);
		maxIdleTime = appConstants.getInt(MIN_POOL_SIZE_PROPERTY, maxIdleTime);
		maxLifeTime = appConstants.getInt(MIN_POOL_SIZE_PROPERTY, maxLifeTime);
	}

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String dataSourceName) {
		if (dataSource instanceof XADataSource) {
			PoolingDataSource result = new PoolingDataSource();
			result.setUniqueName(dataSourceName);
			result.setMinPoolSize(getMinPoolSize());
			result.setMaxPoolSize(getMaxPoolSize());
			result.setMaxIdleTime(getMaxIdleTime());
			result.setMaxLifeTime(getMaxLifeTime());
			result.setAllowLocalTransactions(true);
			result.setXaDataSource((XADataSource)dataSource);
			result.init();
			return result;
		}

		log.warn("DataSource [{}] is not XA enabled", dataSourceName);
		return (DataSource)dataSource;
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
