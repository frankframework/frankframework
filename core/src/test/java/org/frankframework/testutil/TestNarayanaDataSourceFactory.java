package org.frankframework.testutil;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;

public class TestNarayanaDataSourceFactory extends NarayanaDataSourceFactory {

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) throws NamingException {
		return super.getDataSource("jdbc/" + jndiName, jndiEnvironment);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
		return super.createXADataSource(XaDatasourceCommitStopper.augmentXADataSource(xaDataSource), product);
	}
}
