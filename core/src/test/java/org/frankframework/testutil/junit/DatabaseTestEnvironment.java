package org.frankframework.testutil.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.URLDataSourceFactory;
import org.frankframework.util.SpringUtils;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * The DatabaseTestEnvironment will automatically be cleaned up after each test where it may throw exceptions if any connections have not been closed properly.
 * 
 * @author Niels Meijer
 */
@Log4j2
public class DatabaseTestEnvironment implements Store.CloseableResource {

	private String name;
	private final @Getter String dataSourceName;
	private final DataSource dataSource;
	private @Getter IDbmsSupport dbmsSupport;
	private final TransactionManagerType type;
	private final @Getter TestConfiguration configuration;
	private final AtomicInteger connectionCount = new AtomicInteger(0);

	private @Getter final PlatformTransactionManager txManager;
	private final List<TransactionStatus> transactionsToClose = new ArrayList<>();

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

	public String getName() {
		return type.name();
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

		txManager = getConfiguration().getBean(SpringTxManagerProxy.class, "txManager");
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
			String value = kvPair.length == 1 ? "" : kvPair[1];
			if(!props.containsKey(key)) {
				props.put(key, value);
			}
		}
		return props;
	}

	/** Populates all database related fields that are normally wired through Spring */
	public void autowire(@Nonnull Object bean) {
		configuration.autowireByName(bean);
		if(bean instanceof JdbcFacade) {
			((JdbcFacade) bean).setDatasourceName(getDataSourceName());
		}
	}

	public <T> T createBean(Class<T> beanClass) {
		T bean = SpringUtils.createBean(configuration, beanClass);
		autowire(bean);
		return bean;
	}

	@Override
	public String toString() {
		return "DB Environment for [" + type.name() + "] name [" + dataSourceName + "]";
	}

	@Override
	public void close() throws Throwable {
		Collections.reverse(transactionsToClose);
		transactionsToClose.forEach(this::completeTxSafely);

		if(connectionCount.get() > 0) {
			throw new JUnitException("Not all connections have been closed! Don't forget to close all database connections in your test.");
		}
	}

	private TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	private TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}

	public TransactionStatus startTransaction(final int transactionAttribute) {
		return startTransaction(getTxDef(transactionAttribute));
	}

	protected TransactionStatus startTransaction(final TransactionDefinition txDef) {
		TransactionStatus tx = getTxManager().getTransaction(txDef);
		registerForCleanup(tx);
		return tx;
	}

	private void registerForCleanup(final TransactionStatus tx) {
		transactionsToClose.add(tx);
	}
	private void completeTxSafely(final TransactionStatus tx) {
		if (!tx.isCompleted()) {
			try {
				getTxManager().rollback(tx);
			} catch (Exception e) {
				log.warn("Exception rolling back non-completed transaction", e);
			}
		}
	}
}
