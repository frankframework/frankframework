package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;

import org.frankframework.jdbc.datasource.DataSourceFactory;
import org.frankframework.testutil.FindAvailableDataSources.TestDatasource;

/**
 * Solely here to prefix the names with jdbc/ (or else we must change all the tests..)
 */
public class TestDataSourceFactory extends DataSourceFactory {

	@NonNull
	@Override
	public DataSource getDataSource(@NonNull String jndiName, Properties jndiEnvironment) {
		String enrichedDataSourceName = TestDatasource.valueOf(jndiName).getDataSourceName();
		return super.getDataSource(enrichedDataSourceName, jndiEnvironment);
	}

	/**
	 * In getDataSource we add the prefix {@value FindAvailableDataSources#JDBC_RESOURCE_PREFIX}, here we chomp it off again.
	 */
	@NonNull
	@Override
	protected ObjectInfo toObjectInfo(String name) {
		if (name.startsWith(FindAvailableDataSources.JDBC_RESOURCE_PREFIX)) {
			String chompedName = name.substring(FindAvailableDataSources.JDBC_RESOURCE_PREFIX.length());
			return super.toObjectInfo(chompedName);
		}

		return super.toObjectInfo(name);
	}
}
