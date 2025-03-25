package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.frankframework.testutil.FindAvailableDataSources.TestDatasource;

/**
 * Here to prefix the names with jdbc/ (or else we must change all the tests..)
 * And amend the DataSource to use the 'xa' variant
 */
public class TestNarayanaDataSourceFactory extends NarayanaDataSourceFactory {

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) {
		String enrichedDataSourceName = TestDatasource.valueOf(jndiName).getXaDataSourceName();
		return super.getDataSource(enrichedDataSourceName, jndiEnvironment);
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
		setMaxPoolSize(0); // Always disable, some tests change the default values. This ensure we never pool
		return super.createXADataSource(XaDatasourceCommitStopper.augmentXADataSource(xaDataSource), product);
	}
}
