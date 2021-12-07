package nl.nn.adapterframework.jdbc;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.jta.ThreadConnectableDataSourceTransactionManager;
import nl.nn.adapterframework.jta.ThreadConnectableJtaTransactionManager;
import nl.nn.adapterframework.jta.narayana.NarayanaConfigurationBean;


public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;
	protected DataSource txManagedDataSource;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		setupTransactionManagerAndDataSource();
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
//		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
		txManagedDataSource = (dataSource);
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
//		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
		txManagedDataSource = (dataSource);
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
//		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
		txManagedDataSource = (dataSource);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}
	
	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
