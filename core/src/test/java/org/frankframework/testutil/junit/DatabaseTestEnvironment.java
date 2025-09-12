package org.frankframework.testutil.junit;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;

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

import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.SpringUtils;

/**
 * The DatabaseTestEnvironment will automatically be cleaned up after each test where it may throw exceptions if any connections have not been closed properly.
 *
 * @author Niels Meijer
 */
@Log4j2
public class DatabaseTestEnvironment implements Store.CloseableResource {

	private final @Getter String dataSourceName;
	private final @Getter DataSource dataSource;
	private final @Getter IDbmsSupport dbmsSupport;
	private final TransactionManagerType type;
	private final @Getter TestConfiguration configuration;
	private final AtomicInteger connectionCount = new AtomicInteger();

	private final @Getter PlatformTransactionManager txManager;
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

		return (Connection) factory.create(new Class[0], new Object[0], (MethodHandler) (self, method, proceed, args) -> {
			if("close".equals(method.getName())) {
				connectionCount.decrementAndGet();
			}
			return method.invoke(connection, args);
		}
		);
	}

	public String getName() {
		return type.name();
	}

	public DatabaseTestEnvironment(TransactionManagerType type, String productKey) {
		this.type = type;
		this.dataSourceName = productKey;

		TestConfiguration config = type.getConfigurationContext(productKey);
		dataSource = type.getDataSource(productKey);

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
		if(dataSource instanceof DelegatingDataSource source) {
			return getTargetDataSource(source.getTargetDataSource());
		}
		return dataSource;
	}

	/** Populates all database related fields that are normally wired through Spring */
	public void autowire(@Nonnull Object bean) {
		configuration.autowireByName(bean);
		if(bean instanceof JdbcFacade facade) {
			facade.setDatasourceName(getDataSourceName());
		}
	}

	public <T> T createBean(Class<T> beanClass) {
		T bean = SpringUtils.createBean(configuration, beanClass);
		if(bean instanceof JdbcFacade facade) {
			facade.setDatasourceName(getDataSourceName());
		}
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

	public TransactionStatus startTransaction(final int transactionAttribute, int timeout) {
		return startTransaction(getTxDef(transactionAttribute,  timeout));
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
