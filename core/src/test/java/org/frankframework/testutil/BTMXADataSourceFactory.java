package org.frankframework.testutil;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class BTMXADataSourceFactory extends URLXADataSourceFactory {

	@Override
	protected DataSource augmentXADataSource(XADataSource xaDataSource, String product) {
		PoolingDataSource result = new PoolingDataSource();
		result.setUniqueName(product);
		result.setMaxPoolSize(100);
		result.setAllowLocalTransactions(true);
		result.setXaDataSource(xaDataSource);
		result.setIgnoreRecoveryFailures(true);
		result.setEnableJdbc4ConnectionTest(true);
		result.init();
		return result;
	}

	@Override
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
