package org.frankframework.jta.btm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.frankframework.dbms.JdbcException;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.BTMArgumentSource;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DbmsUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogAppender;

@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TestSelfRecoveringBTMDiskJournal.TEST_TABLE)
public class TestSelfRecoveringBTMDiskJournal {
	static final String TEST_TABLE = "BTM_Temp_Table";

	private static final String SELECT_QUERY = "SELECT count(*) FROM "+TEST_TABLE+" where tvarchar='TestSelfRecoveringBTMDiskJournal'";
	private static final String INSERT_QUERY = "INSERT INTO "+TEST_TABLE+" (tkey, tvarchar) VALUES (?, 'TestSelfRecoveringBTMDiskJournal')";
	private static final AtomicInteger COUNT = new AtomicInteger(0);

	private DatabaseTestEnvironment env;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		assumeTrue("H2".equals(env.getDataSourceName()));
		assumeTrue("BTM".equals(env.getName()));

		this.env = env;
	}

	private int getNumberOfLines() throws JdbcException, SQLException {
		String preparedQuery = env.getDbmsSupport().prepareQueryTextForNonLockingRead(SELECT_QUERY);
		try (Connection connection = env.getConnection()) {
			return DbmsUtil.executeIntQuery(connection, preparedQuery);
		}
	}

	@BTMArgumentSource
	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	public void testSelfRecoveringDiskJournalDuringTransaction() throws Exception {
		assertEquals(0, getNumberOfLines()); // Ensure that the table is empty

		// Arrange
		getConnectionCloseJournalAndTestException();

		assertEquals(1, getNumberOfLines()); // Database has been updated !?

		// Assert if new transaction can be created, this is not the case when the
		// previous commit corrupted the TX log due to a ClosedChannelException
		runInsertQuery();

		assertEquals(2, getNumberOfLines());
	}

	@BTMArgumentSource
	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	public void testSelf5RecoveringDiskJournalDuringTransaction() throws Exception {
		assertEquals(0, getNumberOfLines()); // Ensure that the table is empty
		int amount = 5;

		// Arrange
		for (int i = 0; i < amount; i++) {
			getConnectionCloseJournalAndTestException();
		}

		assertEquals(amount, getNumberOfLines()); // Database has been updated !?

		// Assert if new transaction can be created, this is not the case when the
		// previous commit corrupted the TX log due to a ClosedChannelException
		runInsertQuery();

		assertEquals(amount+1, getNumberOfLines());
	}

	@BTMArgumentSource
	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	public void testTooManyRecoveringsDuringTransaction() throws Exception {
		assertEquals(0, getNumberOfLines()); // Ensure that the table is empty

		BtmDiskJournal.setMaxErrorCount(5);
		try {
			// Arrange
			for (int i = 0; i < 5; i++) {
				getConnectionCloseJournalAndTestException(); // 5 times OK
			}
			assertThrows(TransactionSystemException.class, this::getConnectionCloseJournalAndTestException);

			assertEquals(6, getNumberOfLines()); // Database has been updated !?

			// Assert that no new transaction can be created
			assertThrows(CannotCreateTransactionException.class, this::runInsertQuery);

			assertEquals(6, getNumberOfLines()); // Assert no new changes
		} finally {
			int maxRetries = AppConstants.getInstance().getInt("transactionmanager.btm.journal.maxRetries", 500);
			BtmDiskJournal.setMaxErrorCount(maxRetries);
		}
	}

	private void runInsertQuery() throws Exception {
		try (Connection txManagedConnection = env.getConnection()) {
			TransactionStatus txStatus2 = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			assertFalse(txStatus2.isRollbackOnly());
			assertFalse(txStatus2.isCompleted());
			JdbcTestUtil.executeStatement(txManagedConnection, INSERT_QUERY, COUNT.getAndIncrement());
			env.getTxManager().commit(txStatus2);
			assertTrue(txStatus2.isCompleted());
		}
	}

	private void getConnectionCloseJournalAndTestException() throws Exception {
		TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		try (Connection txManagedConnection = env.getConnection()) {
			JdbcTestUtil.executeStatement(txManagedConnection, INSERT_QUERY, COUNT.getAndIncrement());

			// Act
			closeDiskJournal();

			env.getTxManager().commit(txStatus);

			// Assert if old transaction has been completed.
			// These values don't change regardless of the journal state.
			// The next connection will be put in a rollback state
			assertFalse(txStatus.isRollbackOnly());
			assertTrue(txStatus.isCompleted());
			assertFalse(txManagedConnection.isClosed());

			JdbcException ex = assertThrows(JdbcException.class, () -> DbmsUtil.executeIntQuery(txManagedConnection, SELECT_QUERY));
			assertThat(ex.getMessage(), Matchers.endsWith("connection handle already closed")); // But the connection handle apparently is !?
		}
	}

	private void closeDiskJournal() throws Exception {
		Journal journal = TransactionManagerServices.getJournal();

		Field transactionLogAppenderField = DiskJournal.class.getDeclaredField("activeTla");
		transactionLogAppenderField.setAccessible(true);
		AtomicReference<TransactionLogAppender> tlaRef = (AtomicReference<TransactionLogAppender>) transactionLogAppenderField.get(journal);
		TransactionLogAppender tla = tlaRef.get();
		Field fileChannelField = TransactionLogAppender.class.getDeclaredField("fc");
		fileChannelField.setAccessible(true);
		FileChannel fc = (FileChannel) fileChannelField.get(tla);
		fc.close();
	}
}
