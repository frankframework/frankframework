package nl.nn.adapterframework.jdbc;

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
	protected void setupTransactionManagerAndDataSource() {
		switch (transactionManagerType) {
			case DATASOURCE:
				setupDatasourceTransactionManagerAndDataSource();
				break;
			case BTM:
				setupJtaTransactionManagerAndDataSource();
				break;
			default:
				throw new IllegalArgumentException("Don't know how to setupTransactionManagerAndDataSource() for transactionManagerType ["+transactionManagerType+"]");
		}
	}

	private void setupDatasourceTransactionManagerAndDataSource() {
		// setup a TransactionManager like in springTOMCAT.xml
		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager;
		dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
		txManagedDataSource = new TransactionAwareDataSourceProxy(dataSource);
		txManagedDataSource = (dataSource);
	}
	
	private void setupJtaTransactionManagerAndDataSource() {
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
		txManagedDataSource = (dataSource);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}
	
	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
