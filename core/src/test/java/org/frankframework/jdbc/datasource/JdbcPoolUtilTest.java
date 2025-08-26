package org.frankframework.jdbc.datasource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.transaction.TransactionManager;

import org.apache.tomcat.dbcp.dbcp2.PoolableConnection;
import org.apache.tomcat.dbcp.dbcp2.PoolableConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.DataSourceXAConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.tomcat.dbcp.dbcp2.managed.XAConnectionFactory;
import org.apache.tomcat.dbcp.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;

public class JdbcPoolUtilTest {

	@Test
	public void testDelegatedDs() {
		DataSource dataSource = mock(DataSource.class);
		assertFalse(JdbcPoolUtil.isXaCapable(dataSource));
	}

	@Test
	public void testDelegatedDsProxy() {
		DataSource dataSource = mock(DataSource.class);
		TransactionalDbmsSupportAwareDataSourceProxy proxy = new TransactionalDbmsSupportAwareDataSourceProxy(dataSource);
		assertFalse(JdbcPoolUtil.isXaCapable(proxy));
	}

	@Test
	public void testPooledDs() {
		DataSource dataSource = mock(DataSource.class);
		PoolingDataSourceFactory factory = new PoolingDataSourceFactory();
		DataSource pooled = factory.augment(dataSource, null);
		assertFalse(JdbcPoolUtil.isXaCapable(pooled));
	}

	@Test
	public void testPooledXaDs() {
		XADataSource xaDataSource = mock(XADataSource.class);

		XAConnectionFactory cf = new DataSourceXAConnectionFactory(mock(TransactionManager.class), xaDataSource);
		PoolableConnectionFactory poolableConnectionFactory = new PoolableManagedConnectionFactory(cf, null);

		PoolingDataSourceFactory factory = new PoolingDataSourceFactory();
		GenericObjectPool<PoolableConnection> connectionPool = factory.createConnectionPool(poolableConnectionFactory);

		DataSource pooled = new OpenManagedDataSource<>(connectionPool, cf.getTransactionRegistry());

		assertTrue(JdbcPoolUtil.isXaCapable(pooled));
	}
}
