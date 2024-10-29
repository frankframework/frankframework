package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;

public class TransactionManagerTest {
	private static final String TEST_TABLE = "tralala";

	protected void checkNumberOfLines(DatabaseTestEnvironment env, int expected) throws Exception {
		checkNumberOfLines(env, expected, "select count(*) from " + TEST_TABLE + " where TKEY = 1");
	}

	private void checkNumberOfLines(DatabaseTestEnvironment env, int expected, String query) throws Exception {
		String preparedQuery = env.getDbmsSupport().prepareQueryTextForNonLockingRead(query);
		try(Connection connection = env.getConnection()) {
			int count = JdbcTestUtil.executeIntQuery(connection, preparedQuery);
			assertEquals(expected, count, "number of lines in table");
		}
	}

	@TxManagerTest
	@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TEST_TABLE)
	public void testCommit(DatabaseTestEnvironment env) throws Exception {
		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "DELETE FROM " + TEST_TABLE + " where TKEY=1");
		}

		TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = env.getConnection()) {
			checkNumberOfLines(env, 0);
			JdbcTestUtil.executeStatement(txManagedConnection, "INSERT INTO " + TEST_TABLE + " (tkey) VALUES (1)");
		}

		env.getTxManager().commit(txStatus);

		checkNumberOfLines(env, 1);
	}

	@TxManagerTest
	@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TEST_TABLE)
	public void testRollback(DatabaseTestEnvironment env) throws Exception {
		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "DELETE FROM " + TEST_TABLE + " where TKEY=1");
		}

		TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = env.getConnection()) {
			checkNumberOfLines(env, 0);
			JdbcTestUtil.executeStatement(txManagedConnection, "INSERT INTO " + TEST_TABLE + " (tkey) VALUES (1)");
//			checkNumberOfLines(env, 0);
		}
//		checkNumberOfLines(env, 0);

		env.getTxManager().rollback(txStatus);

		checkNumberOfLines(env, 0);
	}

	@TxManagerTest
	@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TEST_TABLE)
	public void testRequiresNew(DatabaseTestEnvironment env) throws Exception {
		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, "DELETE FROM " + TEST_TABLE + " where TKEY=1");
		}

		try (Connection txManagedConnection = env.getConnection()) {
			checkNumberOfLines(env, 0);
			JdbcTestUtil.executeStatement(txManagedConnection, "INSERT INTO " + TEST_TABLE + " (tkey) VALUES (1)");
		}

		TransactionStatus txStatus1 = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);

		try (Connection txManagedConnection = env.getConnection()) {
			checkNumberOfLines(env, 1);
			JdbcTestUtil.executeStatement(txManagedConnection, "UPDATE " + TEST_TABLE + " SET TVARCHAR='tralala' WHERE tkey=1");
		}

		try (Connection txManagedConnection = env.getConnection()) {
			JdbcTestUtil.executeStatement(txManagedConnection, "SELECT TVARCHAR FROM " + TEST_TABLE + " WHERE tkey=1");
		}
		checkNumberOfLines(env, 1);

		TransactionStatus txStatus2 = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try (Connection txManagedConnection = env.getConnection()) {
			JdbcTestUtil.executeStatement(txManagedConnection, "INSERT INTO " + TEST_TABLE + " (tkey) VALUES (2)");
		}

		env.getTxManager().commit(txStatus2);
		env.getTxManager().commit(txStatus1);

		checkNumberOfLines(env, 1);
		checkNumberOfLines(env, 1, "select count(*) from " + TEST_TABLE + " where TKEY = 2");
	}

	@TxManagerTest
	@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TEST_TABLE)
	public void testRequiresNewAfterSelect(DatabaseTestEnvironment env) throws Exception {

		// This tests fails for Narayana, if no Modifiers are present for the database driver.
		// @see NarayanaDataSourceFactory.checkModifiers()

		TransactionStatus txStatusOuter = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);
		try (Connection txManagedConnection = env.getConnection()) {
			JdbcTestUtil.executeStatement(txManagedConnection, "SELECT TVARCHAR FROM " + TEST_TABLE + " WHERE tkey=1");
		}

		TransactionStatus txStatusInner = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try (Connection txManagedConnection = env.getConnection()) {
			JdbcTestUtil.executeStatement(txManagedConnection, "INSERT INTO " + TEST_TABLE + " (tkey) VALUES (2)");
		}

		env.getTxManager().commit(txStatusInner);
		env.getTxManager().commit(txStatusOuter);
	}

}
