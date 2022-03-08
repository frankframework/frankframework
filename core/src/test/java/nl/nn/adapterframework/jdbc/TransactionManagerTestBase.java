package nl.nn.adapterframework.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import lombok.Getter;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.jta.ThreadConnectableDataSourceTransactionManager;
import nl.nn.adapterframework.jta.ThreadConnectableJtaTransactionManager;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;
	private @Getter TestConfiguration configuration;

	private static TransactionManagerType singleTransactionManagerType = null; // set to a specific transaction manager type, to speed up testing

	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		TransactionManagerType[] transactionManagerTypes = { singleTransactionManagerType };
		if (singleTransactionManagerType==null) {
			transactionManagerTypes = TransactionManagerType.values();
		}
		List<Object[]> matrix = new ArrayList<>();

		for(TransactionManagerType type: transactionManagerTypes) {
			List<DataSource> datasources;
			if (StringUtils.isNotEmpty(singleDatasource)) {
				datasources = new ArrayList<>();
				datasources.add(type.getDataSource(singleDatasource));
			} else {
				datasources = type.getAvailableDataSources();
			}
			for(DataSource ds : datasources) {
				matrix.add(new Object[] {type, ds});
			}
		}

		return matrix;
	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		configuration = transactionManagerType.getConfigurationContext(productKey);
		txManager = configuration.getBean(SpringTxManagerProxy.class, "txManager");
//		setupDataSource();

		System.err.println(dataSource);
		prepareDatabase();
	}
/*
	protected void setupDataSource() throws Exception {
		switch (transactionManagerType) {
			case DATASOURCE:
				setupSpringTransactionManager();
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

	private void setupSpringTransactionManager() {
		// setup a TransactionManager like in springTOMCAT.xml
		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
	}

	private void setupBTM() {
		bitronix.tm.BitronixTransactionManager btm = bitronix.tm.TransactionManagerServices.getTransactionManager();
		txManager = new ThreadConnectableJtaTransactionManager(btm, btm);
	}

	private void setupNarayana() throws Exception {
		TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
		UserTransaction utx = com.arjuna.ats.jta.UserTransaction.userTransaction();
		txManager = new ThreadConnectableJtaTransactionManager(utx, tm);
	}
*/
	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
