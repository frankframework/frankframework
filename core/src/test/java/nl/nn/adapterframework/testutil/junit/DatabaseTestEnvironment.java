package nl.nn.adapterframework.testutil.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.util.ExceptionUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import lombok.Getter;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.jndi.TransactionalDbmsSupportAwareDataSourceProxy;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;

public class DatabaseTestEnvironment implements Store.CloseableResource {

	private @Getter String name;
	private final @Getter String dataSourceName;
	private final @Getter DataSource dataSource;
	private @Getter IDbmsSupport dbmsSupport;
	private final TransactionManagerType type;
	private final TestConfiguration configuration;

	/**
	 * <b>Make sure to close this!</b>
	 * @return a new Connection each time this method is called
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public DatabaseTestEnvironment(TransactionManagerType type, String productKey) {
		this.type = type;
		this.dataSourceName = productKey;

		TestConfiguration config = type.getConfigurationContext(productKey);
		dataSource = type.getDataSource(productKey);

		String dsInfo; //We can assume a connection has already been made by the URLDataSourceFactory to validate the DataSource/connectivity
		if(dataSource instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
			dsInfo = ((TransactionalDbmsSupportAwareDataSourceProxy) dataSource).getTargetDataSource().toString();
		} else {
			dsInfo = dataSource.toString();
		}
		Properties dataSourceInfo = parseDataSourceInfo(dsInfo);

		//The datasourceName must be equal to the ProductKey to ensure we're testing the correct datasource
		assertEquals(productKey, dataSourceInfo.getProperty(URLDataSourceFactory.PRODUCT_KEY), "DataSourceName does not match ProductKey");

		configuration = type.getConfigurationContext(productKey);
		IDbmsSupportFactory dbmsSupportFactory = config.getBean(IDbmsSupportFactory.class, "dbmsSupportFactory");
		dbmsSupport = dbmsSupportFactory.getDbmsSupport(dataSource);
	}

	final Connection createNonTransactionalConnection() {
		try {
			Connection connection = getTargetDataSource(dataSource).getConnection();
			connection.setAutoCommit(true); //Ensure this connection is NOT transactional!
			return connection;
		} catch (Exception e) {
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}

	private DataSource getTargetDataSource(DataSource dataSource) {
		if(dataSource instanceof DelegatingDataSource) {
			return getTargetDataSource(((DelegatingDataSource) dataSource).getTargetDataSource());
		}
		return dataSource;
	}

	private Properties parseDataSourceInfo(String dsInfo) {
		Properties props = new Properties();
		String[] parts = dsInfo.split("\\] ");
		for (String part : parts) {
			String[] kvPair = part.split(" \\[");
			String key = kvPair[0];
			String value = (kvPair.length == 1) ? "" : kvPair[1];
			if(!props.containsKey(key)) {
				props.put(key, value);
			}
		}
		return props;
	}

	@Override
	public String toString() {
		return "DB Environment for [" + type.name() + "] name [" + dataSourceName + "]";
	}

	@Override
	public void close() throws Throwable {
		System.err.println("--------------------");
	}
}
