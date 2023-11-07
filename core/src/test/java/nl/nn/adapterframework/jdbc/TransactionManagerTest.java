package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.util.JdbcUtil;

public class TransactionManagerTest extends TransactionManagerTestBase {

	protected void checkNumberOfLines(int expected) throws JdbcException, SQLException {
		checkNumberOfLines(expected, "select count(*) from "+TEST_TABLE+" where TKEY = 1");
	}
	private void checkNumberOfLines(int expected, String query) throws JdbcException, SQLException {
		String preparedQuery = dbmsSupport.prepareQueryTextForNonLockingRead(query);
		int count = JdbcUtil.executeIntQuery(connection, preparedQuery);
		assertEquals("number of lines in table", expected, count);
	}

	@Test
	public void testCommit() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM "+TEST_TABLE+" where TKEY=1");

		TransactionStatus txStatus = startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO "+TEST_TABLE+" (tkey) VALUES (1)");
		}

		txManager.commit(txStatus);

		checkNumberOfLines(1);
	}

	@Test
	public void testRollback() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM "+TEST_TABLE+" where TKEY=1");

		TransactionStatus txStatus = startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO "+TEST_TABLE+" (tkey) VALUES (1)");
//			checkNumberOfLines(0);
		}
//		checkNumberOfLines(0);

		txManager.rollback(txStatus);

		checkNumberOfLines(0);
	}

	@Test
	public void testRequiresNew() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM "+TEST_TABLE+" where TKEY=1");
		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO "+TEST_TABLE+" (tkey) VALUES (1)");
		}

		TransactionStatus txStatus1 = startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = getConnection()) {
			checkNumberOfLines(1);
			JdbcUtil.executeStatement(txManagedConnection, "UPDATE "+TEST_TABLE+" SET TVARCHAR='tralala' WHERE tkey=1");
		}

		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "SELECT TVARCHAR FROM "+TEST_TABLE+" WHERE tkey=1");
		}
		checkNumberOfLines(1);

		TransactionStatus txStatus2 = startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO "+TEST_TABLE+" (tkey) VALUES (2)");
		}

		txManager.commit(txStatus2);
		txManager.commit(txStatus1);

		checkNumberOfLines(1);
		checkNumberOfLines(1, "select count(*) from "+TEST_TABLE+" where TKEY = 2");
	}

	@Test
	public void testRequiresNewAfterSelect() throws Exception {

		// This tests fails for Narayana, if no Modifiers are present for the database driver.
		// @see NarayanaDataSourceFactory.checkModifiers()

		TransactionStatus txStatusOuter = startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);
		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "SELECT TVARCHAR FROM "+TEST_TABLE+" WHERE tkey=1");
		}

		TransactionStatus txStatusInner = startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO "+TEST_TABLE+" (tkey) VALUES (2)");
		}

		txManager.commit(txStatusInner);
		txManager.commit(txStatusOuter);
	}

}
