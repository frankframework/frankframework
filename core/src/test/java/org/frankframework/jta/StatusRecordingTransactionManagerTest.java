package org.frankframework.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.transaction.TransactionManager;

import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.TxManagerTest;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.TransactionSystemException;

import lombok.Setter;

public class StatusRecordingTransactionManagerTest extends StatusRecordingTransactionManagerTestBase<StatusRecordingTransactionManagerTest.TestableStatusRecordingTransactionManager>{

	private TestableStatusRecordingTransactionManager tm;

	@Override
	protected TestableStatusRecordingTransactionManager createTransactionManager() {
		return new TestableStatusRecordingTransactionManager();
	}


	protected class TestableStatusRecordingTransactionManager extends StatusRecordingTransactionManager {

		private @Setter boolean pendingTransactionsAfterShutdown = true;

		@Override
		protected TransactionManager createTransactionManager() throws TransactionSystemException {
			return null;
		}

		@Override
		protected boolean shutdownTransactionManager() throws TransactionSystemException {
			return pendingTransactionsAfterShutdown;
		}

		@Override
		public String determineTmUid() {
			return super.determineTmUid();
		}
		@Override
		public void initUserTransactionAndTransactionManager() throws TransactionSystemException {
			super.initUserTransactionAndTransactionManager();
		}

	}

	@AfterEach
	public void tearDown() {
		if (tm != null) {
			tm.destroy();
		}
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testCleanSetup() {
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String status = read(statusFile);
		assertEquals("ACTIVE", status);
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testPresetTmUid() {
		write(tmUidFile,"fakeTmUid");
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertEquals("fakeTmUid", tmUid);

		assertStatus("ACTIVE", tmUid);
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testCleanShutdown() {
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();
		tm = null;

		assertStatus("COMPLETED", tmUid);
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testNoStatusFiles() {
		tm = new TestableStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertNotNull(tm.getUid());
		tm.destroy(); // should throw no exception
		tm = null;
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testShutdownWithPendingTransactions() {
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.setPendingTransactionsAfterShutdown(false);
		tm.destroy();
		tm = null;

		assertStatus("PENDING", tmUid);
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testCreateFolders() {
		tm = setupTransactionManager();
		tm.setUidFile(folder+"/a/b/c/"+TMUID_FILE);
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);
		String recordedTmUid = read(folder+"/a/b/c/"+TMUID_FILE);
		assertEquals(tmUid, recordedTmUid);
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testTestReadWithWhitespace() {
		tm = setupTransactionManager();
		String value = "fake tm uid";
		String fullPathTmFile = folder+"/"+TMUID_FILE;
		tm.write(fullPathTmFile, "\n "+value+" \n\n");
		assertEquals(value, tm.read(fullPathTmFile));
	}
}
