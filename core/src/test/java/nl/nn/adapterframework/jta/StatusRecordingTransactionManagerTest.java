package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.transaction.TransactionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.TransactionSystemException;

import lombok.Setter;

public class StatusRecordingTransactionManagerTest extends StatusRecordingTransactionManagerTestBase<nl.nn.adapterframework.jta.StatusRecordingTransactionManagerTest.TestableStatusRecordingTransactionManager>{

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
	
	@Override
	@Before
	public void setup() throws IOException {
		TemporaryFolder tmpFolder = new TemporaryFolder();
		tmpFolder.create();
		folder = tmpFolder.getRoot().toString();
		super.setup();
		delete(tmUidFile);
	}

	@Test
	public void testCleanSetup() {
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String status = read(statusFile);
		assertEquals("ACTIVE", status);
	}

	@Test
	public void testPresetTmUid() {
		write(tmUidFile,"fakeTmUid");
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertEquals("fakeTmUid", tmUid);

		assertStatus("ACTIVE", tmUid);
	}

	@Test
	public void testCleanShutdown() {
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();

		assertStatus("COMPLETED", tmUid);
	}

	@Test
	public void testNoStatusFiles() {
		TestableStatusRecordingTransactionManager tm = new TestableStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertNotNull(tm.getUid());
		tm.destroy(); // should throw no exception
	}

	@Test
	public void testShutdownWithPendingTransactions() {
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.setPendingTransactionsAfterShutdown(false);
		tm.destroy();

		assertStatus("PENDING", tmUid);
	}

	@Test
	public void testCreateFolders() {
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		tm.setUidFile(folder+"/a/b/c/"+TMUID_FILE);
		tm.initUserTransactionAndTransactionManager();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);
		String recordedTmUid = read(folder+"/a/b/c/"+TMUID_FILE);
		assertEquals(tmUid, recordedTmUid);
	}

	@Test
	public void testTestReadWithWhitespace() {
		TestableStatusRecordingTransactionManager tm = setupTransactionManager();
		String value = "fake tm uid";
		String fullPathTmFile = folder+"/"+TMUID_FILE;
		tm.write(fullPathTmFile, "\n "+value+" \n\n");
		assertEquals(value, tm.read(fullPathTmFile));
	}

}
