package nl.nn.adapterframework.testutil;

import javax.sql.DataSource;
import javax.sql.XADataSource;

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
		result.init();
		return result;
	}

	@Override
	public void destroy() throws Exception {
		objects.values().stream().filter(ds -> ds instanceof PoolingDataSource).forEach(ds -> ((PoolingDataSource)ds).close());
	}

}
