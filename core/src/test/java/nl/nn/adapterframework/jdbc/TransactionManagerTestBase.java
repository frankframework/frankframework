package nl.nn.adapterframework.jdbc;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.jta.ThreadConnectableDataSourceTransactionManager;
import nl.nn.adapterframework.jta.ThreadConnectableJtaTransactionManager;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;

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
		computeProductKey();

		setupDataSource();

		prepareDatabase();
	}

	protected void computeProductKey() {
		switch (transactionManagerType) {
			case DATASOURCE:
				Properties dataSourceProperties = ((DriverManagerDataSource)dataSource).getConnectionProperties();
				productKey = dataSourceProperties.getProperty(URLDataSourceFactory.PRODUCT_KEY);
				testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceProperties.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));
				break;
			default:
				productKey = dataSource.toString();
				break;
		}
	}

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
		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager;
		dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
	}

	private void setupBTM() {
		BitronixTransactionManager btm = TransactionManagerServices.getTransactionManager();
		txManager = new ThreadConnectableJtaTransactionManager(btm, btm);
	}

	private void setupNarayana() throws Exception {
		ThreadConnectableDataSourceTransactionManager dataSourceTransactionManager = new ThreadConnectableDataSourceTransactionManager(dataSource);
		dataSourceTransactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		txManager = dataSourceTransactionManager;
	}

	public TransactionDefinition getTxDef(int transactionAttribute, int timeout) {
		return SpringTxManagerProxy.getTransactionDefinition(transactionAttribute, timeout);
	}

	public TransactionDefinition getTxDef(int transactionAttribute) {
		return getTxDef(transactionAttribute, 20);
	}
}
