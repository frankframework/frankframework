package org.frankframework.testutil;

import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.jspecify.annotations.NonNull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.jta.xa.XaDataSourceModifier;
import org.frankframework.testutil.FindAvailableDataSources.TestDatasource;

/**
 * Here to prefix the names with jdbc/ (or else we must change all the tests..)
 * And amend the DataSource to use the 'xa' variant
 */
@Log4j2
public class TestNarayanaDataSourceFactory extends NarayanaDataSourceFactory {

	@NonNull
	@Override
	public DataSource getDataSource(@NonNull String jndiName, Properties jndiEnvironment) {
		try {
			String enrichedDataSourceName = TestDatasource.valueOf(jndiName).getXaDataSourceName();
			return super.getDataSource(enrichedDataSourceName, jndiEnvironment);
		} catch (IllegalArgumentException e) {
			log.debug("Cannot find enum value for datasource name [%s]; trying with plain name".formatted(jndiName), e);
			return super.getDataSource(jndiName, jndiEnvironment);
		}
	}

	@NonNull
	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
		// setMaxPoolSize(0); // Always disable, some tests change the default values. This ensure we never pool
		return super.createXADataSource(XaDataSourceModifier.augmentXADataSource(xaDataSource), product);
	}
}
