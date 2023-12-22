package org.frankframework.jta.btm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.TransactionManagerTestBase;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DbmsUtil;
import org.frankframework.util.JdbcUtil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogAppender;

public class TestSelfRecoveringBTMDiskJournal extends TransactionManagerTestBase {

	private static final String SELECT_QUERY = "SELECT count(*) FROM "+TEST_TABLE+" where tvarchar='TestSelfRecoveringBTMDiskJournal'";
	private static final String INSERT_QUERY = "INSERT INTO "+TEST_TABLE+" (tkey, tvarchar) VALUES (?, 'TestSelfRecoveringBTMDiskJournal')";
	private static final TransactionDefinition TX_DEF = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 20);
	private static final AtomicInteger COUNT = new AtomicInteger(0);

	@Before
	@Override
	public void setup() throws Exception {
		assumeTrue("H2".equals(productKey));

		if(getTransactionManagerType().equals(TransactionManagerType.BTM) && TransactionManagerServices.isTransactionManagerRunning()) {
			log.info("Shutting down TransactionManager before tests");

			getTransactionManagerType().closeConfigurationContext();
		}
		super.setup();
	}

	@After
	@Override
	public void teardown() throws Exception {
		super.teardown();

		if(getTransactionManagerType().equals(TransactionManagerType.BTM) && TransactionManagerServices.isTransactionManagerRunning()) {
			log.info("Shutting down TransactionManager after tests");
			getTransactionManagerType().closeConfigurationContext();

			if(TransactionManagerServices.isTransactionManagerRunning()) {
				fail("unable to shutdown BTM TransactionManager");
			}
		}
	}

	private int getNumberOfLines() throws JdbcException, SQLException {
		String preparedQuery = dbmsSupport.prepareQueryTextForNonLockingRead(SELECT_QUERY);
		try (Connection connection = createNonTransactionalConnection()) {
			return DbmsUtil.executeIntQuery(connection, preparedQuery);
		}
	}

	@Test
	public void testSelfRecoveringDiskJournalDuringTransaction() throws Exception {
		assumeTrue(getTransactionManagerType().equals(TransactionManagerType.BTM));

		assertEquals(0, getNumberOfLines()); // Ensure that the table is empty

		// Arrange
		getConnectionCloseJournalAndTestException();

		assertEquals(1, getNumberOfLines()); // Database has been updated !?

		// Assert if new transaction can be created, this is not the case when the
		// previous commit corrupted the TX log due to a ClosedChannelException
		runInsertQuery();

		assertEquals(2, getNumberOfLines());
	}

	@Test
	public void testSelf5RecoveringDiskJournalDuringTransaction() throws Exception {
		assumeTrue(getTransactionManagerType().equals(TransactionManagerType.BTM));
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

	@Test
	public void testTooManyRecoveringsDuringTransaction() throws Exception {
		assumeTrue(getTransactionManagerType().equals(TransactionManagerType.BTM));
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
		try (Connection txManagedConnection = getConnection()) {
			TransactionStatus txStatus2 = startTransaction(TX_DEF);
			assertFalse(txStatus2.isRollbackOnly());
			assertFalse(txStatus2.isCompleted());
			JdbcUtil.executeStatement(txManagedConnection, INSERT_QUERY, COUNT.getAndIncrement());
			txManager.commit(txStatus2);
			assertTrue(txStatus2.isCompleted());
		}
	}

	private void getConnectionCloseJournalAndTestException() throws Exception {
		TransactionStatus txStatus = startTransaction(TX_DEF);
		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, INSERT_QUERY, COUNT.getAndIncrement());

			// Act
			closeDiskJournal();

			txManager.commit(txStatus);

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
