package org.frankframework.testutil.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.mockito.Mockito;

import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.jdbc.datasource.OpenPoolingDataSource;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;

public class DataSourceFactoryMock implements IDataSourceFactory {
	private final Map<String, DataSource> objects = new HashMap<>();

	public DataSourceFactoryMock() {
		// Create a pooled datasource for the TestSecurityItems test, and wrap it in a delegating datasource to test it's recursive-ness.
		DataSource ds = new OpenPoolingDataSource<PoolableConnection>(new GenericObjectPool<>(Mockito.mock(PoolableConnectionFactory.class)));
		objects.put(IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME, new TransactionalDbmsSupportAwareDataSourceProxy(ds));
	}

	@Override
	public DataSource getDataSource(String dataSourceName) {
		return getDataSource(dataSourceName, null);
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) {
		return objects.get(dataSourceName);
	}

	@Override
	public List<String> getDataSourceNames() {
		return new ArrayList<>(objects.keySet());
	}
}
