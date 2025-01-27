package org.frankframework.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.transaction.TransactionManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionSystemException;

import lombok.Setter;

public class StatusRecordingTransactionManagerTest extends StatusRecordingTransactionManagerTestBase<StatusRecordingTransactionManagerTest.TestableStatusRecordingTransactionManager>{

	private TestableStatusRecordingTransactionManager tm;

	@Override
	protected TestableStatusRecordingTransactionManager createTransactionManager() {
		return new TestableStatusRecordingTransactionManager();
	}


	protected class TestableStatusRecordingTransactionManager extends AbstractStatusRecordingTransactionManager {

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

	@Override
	@AfterEach
	public void tearDown() {
		if (tm != null) {
			tm.destroy();
		}
		super.tearDown();
	}

	@Test
	public void testCleanSetup() {
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String status = read(statusFile);
		assertEquals("ACTIVE", status);
	}

	@Test
	public void testPresetTmUid() {
		write(tmUidFile,"fakeTmUid");
		tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertEquals("fakeTmUid", tmUid);

		assertStatus("ACTIVE", tmUid);
	}

	@Test
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

	@Test
	public void testNoStatusFiles() {
		tm = new TestableStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertNotNull(tm.getUid());
		tm.destroy(); // should throw no exception
		tm = null;
	}

	@Test
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

	@Test
	public void testCreateFolders() {
		tm = setupTransactionManager();
		tm.setUidFile(folder+"/a/b/c/"+TMUID_FILE);
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);
		String recordedTmUid = read(folder+"/a/b/c/"+TMUID_FILE);
		assertEquals(tmUid, recordedTmUid);
	}

	@Test
	public void testTestReadWithWhitespace() {
		tm = setupTransactionManager();
		String value = "fake tm uid";
		String fullPathTmFile = folder+"/"+TMUID_FILE;
		tm.write(fullPathTmFile, "\n "+value+" \n\n");
		assertEquals(value, tm.read(fullPathTmFile));
	}
}
