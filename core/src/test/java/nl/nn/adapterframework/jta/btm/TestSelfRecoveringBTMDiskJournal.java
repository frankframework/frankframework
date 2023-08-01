package nl.nn.adapterframework.jta.btm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.journal.DiskJournal;
import bitronix.tm.journal.Journal;
import bitronix.tm.journal.TransactionLogAppender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.JdbcUtil;

public class TestSelfRecoveringBTMDiskJournal extends TransactionManagerTestBase {

	private static final String SELECT_QUERY = "SELECT count(*) FROM "+TEST_TABLE+" where tvarchar='TestSelfRecoveringBTMDiskJournal'";
	private static final String INSERT_QUERY = "INSERT INTO "+TEST_TABLE+" (tkey, tvarchar) VALUES (?, 'TestSelfRecoveringBTMDiskJournal')";

	@Before
	@Override
	public void setup() throws Exception {
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
		try (Connection connection = createNonTransactionalConnection(); JdbcSession session = dbmsSupport.prepareSessionForNonLockingRead(connection)) {
			return JdbcUtil.executeIntQuery(connection, preparedQuery);
		}
	}

	@Test
	public void testSelfRecoveringDiskJournalDuringTransaction() throws Exception {
		assumeTrue(getTransactionManagerType().equals(TransactionManagerType.BTM));

		assertEquals(0, getNumberOfLines()); // Ensure that the table is empty
		// Arrange
		TransactionDefinition required = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 20);
		TransactionStatus txStatus = getTransaction(required);

		try (Connection txManagedConnection = getConnection()) {
			JdbcUtil.executeStatement(txManagedConnection, INSERT_QUERY, 1);

			// Act
			closeDiskJournal();

			txManager.commit(txStatus);

			// Assert if old transaction has been completed.
			// These values don't change regardless of the journal state.
			// The next connection will be put in a rollback state
			assertFalse(txStatus.isRollbackOnly());
			assertTrue(txStatus.isCompleted());
			assertFalse(txManagedConnection.isClosed());

			JdbcException ex = assertThrows(JdbcException.class, ()-> {JdbcUtil.executeIntQuery(txManagedConnection, SELECT_QUERY);});
			assertThat(ex.getMessage(), Matchers.endsWith("connection handle already closed")); // But the connection handle apparently is !?
		}

		assertEquals(1, getNumberOfLines()); // Database has been updated !?

		// Assert if new transaction can be created, this is not the case when the
		// previous commit corrupted the TX log due to a ClosedChannelException
		try (Connection txManagedConnection = getConnection()) {
			TransactionStatus txStatus2 = getTransaction(required);
			assertFalse(txStatus2.isRollbackOnly());
			assertFalse(txStatus2.isCompleted());
			JdbcUtil.executeStatement(txManagedConnection, INSERT_QUERY, 2);
			txManager.commit(txStatus2);
			assertTrue(txStatus2.isCompleted());
		}

		assertEquals(2, getNumberOfLines());
	}

	private TransactionStatus getTransaction(final TransactionDefinition required) {
		final TransactionStatus tx = txManager.getTransaction(required);
		registerForCleanup(tx);
		return tx;
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
