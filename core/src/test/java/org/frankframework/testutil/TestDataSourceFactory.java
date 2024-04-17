package org.frankframework.testutil;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.frankframework.jndi.DataSourceFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

public class TestDataSourceFactory extends DataSourceFactory {

	public static final String PRODUCT_KEY = "product";

	@Override
	protected DataSource augmentDatasource(CommonDataSource dataSource, String product) {
		// Just cast the datasource and do not wrap it in a pool for the benefit of the tests.
		return namedDataSource((DataSource) dataSource, product);
	}

	private DataSource namedDataSource(DataSource ds, String name) {
		return new DelegatingDataSource(ds) {
			@Override
			public String toString() {
				return String.format("%s [%s] ", PRODUCT_KEY, name.replaceAll("jdbc/", ""));
			}
		};
	}

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) throws NamingException {
		return super.getDataSource("jdbc/"+jndiName, jndiEnvironment);
	}
}
