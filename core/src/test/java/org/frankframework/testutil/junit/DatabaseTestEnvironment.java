package org.frankframework.testutil.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.dbms.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.URLDataSourceFactory;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.Getter;

public class DatabaseTestEnvironment implements Store.CloseableResource {

	private @Getter String name;
	private final @Getter String dataSourceName;
	private final @Getter DataSource dataSource;
	private @Getter IDbmsSupport dbmsSupport;
	private final TransactionManagerType type;
	private final @Getter TestConfiguration configuration;
	private final AtomicInteger connectionCount = new AtomicInteger(0);

	/**
	 * <b>Make sure to close this!</b>
	 * @return a new Connection each time this method is called
	 */
	public Connection getConnection() throws SQLException {
		connectionCount.incrementAndGet();
		try {
			return wrapCountingConnectionDelegate(dataSource.getConnection());
		} catch (Exception e) {
			connectionCount.decrementAndGet();
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}

	private Connection wrapCountingConnectionDelegate(Connection connection) throws Exception {
		ProxyFactory factory = new ProxyFactory();
		factory.setInterfaces(new Class[] {Connection.class});

		return (Connection) factory.create(new Class[0], new Object[0], new MethodHandler() {
			@Override
			public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
				if(method.getName().equals("close")) {
					connectionCount.decrementAndGet();
				}
				return method.invoke(connection, args);
			}
		});
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
		DbmsSupportFactory dbmsSupportFactory = config.getBean(DbmsSupportFactory.class, "dbmsSupportFactory");
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
		if(connectionCount.get() > 0) {
			throw new JUnitException("Not all connections have been closed! Don't forget to close all database connections in your test.");
		}
	}
}
