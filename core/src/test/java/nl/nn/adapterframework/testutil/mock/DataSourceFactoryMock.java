package nl.nn.adapterframework.testutil.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import nl.nn.adapterframework.jdbc.IDataSourceFactory;

public class DataSourceFactoryMock implements IDataSourceFactory {
	private Map<String, DataSource> objects = new ConcurrentHashMap<>();

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
