package nl.nn.adapterframework.testutil.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.mockito.Mockito;

import nl.nn.adapterframework.jdbc.IDataSourceFactory;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class DataSourceFactoryMock implements IDataSourceFactory {
	private final Map<String, DataSource> objects = new ConcurrentHashMap<>();

	public DataSourceFactoryMock() {
		DataSource ds = Mockito.mock(DataSource.class);
		objects.put(JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME, ds);
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
