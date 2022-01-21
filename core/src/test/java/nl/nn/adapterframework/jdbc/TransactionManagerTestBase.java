package nl.nn.adapterframework.jdbc;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.jta.ThreadConnectableDataSourceTransactionManager;
import nl.nn.adapterframework.jta.ThreadConnectableJtaTransactionManager;
import nl.nn.adapterframework.testutil.TransactionManagerType;

public abstract class TransactionManagerTestBase extends JdbcTestBase {

	protected IThreadConnectableTransactionManager txManager;

/*
	@Parameters(name= "{0}")
	public static Collection data() {
		TransactionManagerType[] tmt = TransactionManagerType.values();
//		Object[][] matrix = new Object[tmt.length][];
		List<List<ParameterHolder>> list = new ArrayList<>();

		int i = 0;
		for(TransactionManagerType type : TransactionManagerType.values()) {
			List<ParameterHolder> transactionManagers = new ArrayList<>();
			List<DataSource> datasources = type.getAvailableDataSources();
			int j = 0;
//			matrix[i] = new Object[datasources.size()][2];
			for(DataSource ds : datasources) {
//				matrix[i][j] = new ParameterHolder(type, ds);
				transactionManagers.add(new ParameterHolder(type, ds));
				j++;
			}
			i++;
			System.err.println(transactionManagers);
			list.add(transactionManagers);
		}

		return list;
//		return Arrays.asList(matrix);
	}
*/

	@Parameters(name= "{0}: {1}")
	public static Collection data() {
		TransactionManagerType type = TransactionManagerType.BTM;
		List<DataSource> datasources = type.getAvailableDataSources();
		Object[][] matrix = new Object[datasources.size()][];

		int i = 0;
		for(DataSource ds : datasources) {
			matrix[i] = new Object[] {type, ds};
			i++;
		}

		return Arrays.asList(matrix);
	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		setupDataSource();

		prepareDatabase();
	}

	protected void setupDataSource() throws Exception {
		switch (transactionManagerType) {
			case DATASOURCE:
			case NARAYANA:
				setupSpringTransactionManager();
				break;
			case BTM:
				setupBTM();
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
		BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
		txManager = new ThreadConnectableJtaTransactionManager(btm, btm);
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
