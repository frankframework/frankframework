package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.SpringUtils;

public class TransactionManagerTest extends TransactionManagerTestBase {
	private static Map<TransactionManagerType, TestConfiguration> configurations = new WeakHashMap<>();

	protected void checkNumberOfLines(int expected) throws JdbcException, SQLException {
		checkNumberOfLines(expected, "select count(*) from TEMP where TKEY = 1");
	}
	private void checkNumberOfLines(int expected, String query) throws JdbcException, SQLException {
		String preparedQuery = dbmsSupport.prepareQueryTextForNonLockingRead(query);
		try (JdbcSession session = dbmsSupport.prepareSessionForNonLockingRead(connection)) {
			int count = JdbcUtil.executeIntQuery(connection, preparedQuery);
			assertEquals("number of lines in table", expected, count);
		}
	}

	@Override
	public void setup() throws Exception {
//		TestConfiguration config = configurations.computeIfAbsent(getTransactionManagerType(), txType -> new TestConfiguration(getTransactionManagerType().getSpringConfiguration()));
//		SpringUtils.autowireByName(config, txManager);
//		config.getAutowireCapableBeanFactory().initializeBean(txManager, "txManager");
//		System.err.println("toito: " + getTransactionManagerType() + " + " +config.getName());
		super.setup();
	}

	@Test
	public void testCommit() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM TEMP where TKEY=1");

		TransactionStatus txStatus = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRED));

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (1)");
//			checkNumberOfLines(0);			
		}
//		checkNumberOfLines(0);

		txManager.commit(txStatus);

		checkNumberOfLines(1);
	}

	@Test
	public void testRollback() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM TEMP where TKEY=1");

		TransactionStatus txStatus = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRED));

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (1)");
//			checkNumberOfLines(0);
		}
//		checkNumberOfLines(0);

		txManager.rollback(txStatus);

		checkNumberOfLines(0);
	}

	@Test
	public void testRequiresNew() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM TEMP where TKEY=1");
		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (1)");
		}

		TransactionStatus txStatus1 = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRED));

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(1);
			JdbcUtil.executeStatement(txManagedConnection, "UPDATE TEMP SET TVARCHAR='tralala' WHERE tkey=1");
		}

		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "SELECT TVARCHAR FROM TEMP WHERE tkey=1");
		}
		checkNumberOfLines(1);

		TransactionStatus txStatus2 = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (2)");
		}

		txManager.commit(txStatus2);
		txManager.commit(txStatus1);

		checkNumberOfLines(1);
		checkNumberOfLines(1, "select count(*) from TEMP where TKEY = 2");
	}
}
