package nl.nn.adapterframework.testutil.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.mockito.Mockito;

import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.jdbc.datasource.OpenPoolingDataSource;
import nl.nn.adapterframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class DataSourceFactoryMock implements IDataSourceFactory {
	private Map<String, DataSource> objects = new ConcurrentHashMap<>();

	public DataSourceFactoryMock() {
		// Create a pooled datasource for the TestSecurityItems test, and wrap it in a delegating datasource to test it's recursive-ness.
		DataSource ds = new OpenPoolingDataSource(new GenericObjectPool(Mockito.mock(PoolableConnectionFactory.class)));
		objects.put(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME, new TransactionalDbmsSupportAwareDataSourceProxy(ds));
	}

	@Override
	public DataSource getDataSource(String dataSourceName) throws NamingException {
		return getDataSource(dataSourceName, null);
	}

	@Override
	public DataSource getDataSource(String dataSourceName, Properties jndiEnvironment) throws NamingException {
		return objects.get(dataSourceName);
	}

	@Override
	public List<String> getDataSourceNames() {
		return new ArrayList<>(objects.keySet());
	}
}
