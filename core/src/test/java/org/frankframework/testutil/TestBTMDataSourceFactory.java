package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.frankframework.jta.btm.BtmDataSourceFactory;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class TestBTMDataSourceFactory extends BtmDataSourceFactory {

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) {
		return super.getDataSource("jdbc/" + jndiName, jndiEnvironment);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
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

}
