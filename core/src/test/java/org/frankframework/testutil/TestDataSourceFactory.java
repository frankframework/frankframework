package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.frankframework.jdbc.datasource.DataSourceFactory;

/**
 * Solely here to prefix the names with jdbc/ (or else we must change all the tests..)
 */
public class TestDataSourceFactory extends DataSourceFactory {

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String product) {
		// Just cast the datasource and do not wrap it in a pool for the benefit of the tests.
		return (DataSource) dataSource;
	}

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) {
		return super.getDataSource("jdbc/"+jndiName, jndiEnvironment);
	}
}
