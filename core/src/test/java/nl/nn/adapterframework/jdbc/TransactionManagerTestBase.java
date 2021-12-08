package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.Getter;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase.TransactionManagerType;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.jta.ThreadConnectableDataSourceTransactionManager;
import nl.nn.adapterframework.jta.ThreadConnectableJtaTransactionManager;
import nl.nn.adapterframework.jta.narayana.NarayanaConfigurationBean;
import nl.nn.adapterframework.testutil.BTMXADataSourceFactory;
import nl.nn.adapterframework.testutil.NarayanaXADataSourceFactory;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;
	private DataSource txManagedDataSource;

	@Parameters(name= "{0}: {1}")
	public static Collection data() {
		TransactionManagerType[] tmt = TransactionManagerType.values();
		Object[][][] matrix = new Object[tmt.length][][];
//		List list = new ArrayList();

		int i = 0;
		for(TransactionManagerType type : TransactionManagerType.values()) {
//			List transactionManagers = new ArrayList();
			List<DataSource> datasources = type.getAvailableDataSources();
			int j = 0;
			matrix[i] = new Object[datasources.size()][2];
			for(DataSource ds : datasources) {
				matrix[i][j] = new Object[] {type, ds};
//				transactionManagers.add(new Object[] {type, ds});
				j++;
			}
			i++;
//			list.add(transactionManagers);
		}

//		return list;
		return Arrays.asList(matrix);
	}

	@Override
	@Before
	public void setup() throws Exception {
		switch (transactionManagerType) {
			case DATASOURCE:
				Properties dataSourceProperties = ((DriverManagerDataSource)dataSource).getConnectionProperties();
				productKey = dataSourceProperties.getProperty(URLDataSourceFactory.PRODUCT_KEY);
				testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceProperties.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));
				break;
			case BTM:
				productKey = ((PoolingDataSource)dataSource).getUniqueName();
				break;
			case NARAYANA:
				productKey = dataSource.toString();
				break;
			default:
				throw new IllegalArgumentException("Don't know how to setup() for transactionManagerType ["+transactionManagerType+"]");
		}

		setupTransactionManagerAndDataSource();
		super.setup();
	}

	public enum TransactionManagerType {
		DATASOURCE(URLDataSourceFactory.class), 
		BTM(BTMXADataSourceFactory.class), 
		NARAYANA(NarayanaXADataSourceFactory.class);

		private @Getter URLDataSourceFactory dataSourceFactory;

		private TransactionManagerType(Class<? extends URLDataSourceFactory> clazz) {
			try {
				dataSourceFactory = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}

		public List<DataSource> getAvailableDataSources() {
			return getDataSourceFactory().getAvailableDataSources();
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return txManagedDataSource.getConnection();
	}

	@Override
	@After
	public void teardown() throws Exception {
		super.teardown();
		if (transactionManagerType == TransactionManagerType.BTM) {
			if (txManager!=null) {
				TransactionManager txManager2 = txManager;
				if (txManager2 instanceof SpringTxManagerProxy) {
					txManager2 = ((SpringTxManagerProxy)txManager2).getRealTxManager();
				}
				BitronixTransactionManager btm = (BitronixTransactionManager)((JtaTransactionManager)txManager2).getTransactionManager();
				btm.shutdown();
			}
		}
	}
	protected void setupTransactionManagerAndDataSource() throws Exception {
		switch (transactionManagerType) {
			case DATASOURCE:
				setupDatasource();
				break;
			case BTM:
				setupBTM();
				break;
			case NARAYANA:
				setupNarayana();
				break;
			default:
				throw new IllegalArgumentException("Don't know how to setupTransactionManagerAndDataSource() for transactionManagerType ["+transactionManagerType+"]");
		}
	}

	private void setupDatasource() {
		// setup a TransactionManager like in springTOMCAT.xml
		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager;
		dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
//		txManagedDataSource = (dataSource);
	}

	private void setupBTM() {
		// setup a TransactionManager like in springTOMCAT.xml with BTM
		Configuration configuration = TransactionManagerServices.getConfiguration();
		configuration.setLogPart1Filename("target/btm1.log");
		configuration.setLogPart2Filename("target/btm2.log");
		configuration.setSkipCorruptedLogs(true);
		configuration.setGracefulShutdownInterval(3);
		configuration.setDefaultTransactionTimeout(5);
		BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
		txManager = new ThreadConnectableJtaTransactionManager(btm, btm);
		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
//		txManagedDataSource = (dataSource);
	}

	private void setupNarayana() throws Exception {
		// setup a TransactionManager like in springTOMCAT.xml with NARAYANA
		NarayanaConfigurationBean configuration = new NarayanaConfigurationBean();
		Properties properties = new Properties();
		properties.put("JDBCEnvironmentBean.isolationLevel", "2");
		properties.put("ObjectStoreEnvironmentBean.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.stateStore.objectStoreDir", "target/narayana");
		properties.put("ObjectStoreEnvironmentBean.communicationStore.objectStoreDir", "target/narayana");
		configuration.setProperties(properties);
		configuration.afterPropertiesSet();

		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
//		txManagedDataSource = (dataSource);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
