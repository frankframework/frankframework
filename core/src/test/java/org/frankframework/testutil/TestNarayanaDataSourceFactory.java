package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;

public class TestNarayanaDataSourceFactory extends NarayanaDataSourceFactory {

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) {
		return super.getDataSource("jdbc/" + jndiName, jndiEnvironment);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
		setMaxPoolSize(0); //Always disable, some tests change the default values. This ensure we never pool
		return super.createXADataSource(XaDatasourceCommitStopper.augmentXADataSource(xaDataSource), product);
	}
}
