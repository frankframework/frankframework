package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import lombok.Setter;

public class StatusRecordingTransactionManagerTest {
	
	public String STATUS_FILE = "status.txt";
	public String TMUID_FILE = "tm-uid.txt";
	
	@Mock
	private TransactionManager delegateTransactionManager;
	private TemporaryFolder folder;
	
	@Before
	public void setup() throws IOException {
		folder = new TemporaryFolder();
		folder.create();
	}

	@After
	public void tearDown() {
		folder.delete();
	}

	private class TestableStatusRecordingTransactionManager extends StatusRecordingTransactionManager {

		private @Setter boolean pendingTransactionsAfterShutdown;

		@Override
		protected TransactionManager createTransactionManager() throws TransactionSystemException {
			return delegateTransactionManager;
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

	public TestableStatusRecordingTransactionManager getStatusRecordingTransactionManager() {
		TestableStatusRecordingTransactionManager result = new TestableStatusRecordingTransactionManager();
		result.setStatusFile(folder.getRoot()+"/"+STATUS_FILE);
		result.setUidFile(folder.getRoot()+"/"+TMUID_FILE);
		return result;
	}
	
	@Test
	public void testCleanSetup() {
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
		String tmUid = tm.getUid();
		assertNotNull(tmUid);
		String status = read(STATUS_FILE);
		assertEquals("ACTIVE", status);
	}
	
	@Test
	public void testPresetTmUid() {
		write(TMUID_FILE,"fakeTmUid");
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
		String tmUid = tm.getUid();
		assertEquals("fakeTmUid", tmUid);
		
		assertStatus("ACTIVE", tmUid);
	}
	
	@Test
	public void testCleanShutdown() {
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
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
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
		assertNotNull(tm.getUid());
		tm.destroy(); // should throw no exception
	}
	
	@Test
	public void testShutdownWithPendingTransactions() {
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		tm.initUserTransactionAndTransactionManager();
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.setPendingTransactionsAfterShutdown(true);
		tm.destroy();

		assertStatus("PENDING", tmUid);
	}
	

	@Test
	public void testCreateFolders() {
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		tm.setUidFile(folder.getRoot()+"/a/b/c/"+TMUID_FILE);
		tm.initUserTransactionAndTransactionManager();
		assertEquals(delegateTransactionManager, tm.getTransactionManager());
		String tmUid = tm.getUid();
		assertNotNull(tmUid);
		String recordedTmUid = read("/a/b/c/"+TMUID_FILE);
		assertEquals(tmUid, recordedTmUid);
	}

	@Test
	public void testTestReadWithWhitespace() {
		TestableStatusRecordingTransactionManager tm = getStatusRecordingTransactionManager();
		String value = "fake tm uid";
		String fullPathTmFile = folder.getRoot()+"/"+TMUID_FILE;
		tm.write(fullPathTmFile, "\n "+value+" \n\n");
		assertEquals(value, tm.read(fullPathTmFile));
	}

	public void assertStatus(String status, String tmUid) {
		assertEquals(status, read(STATUS_FILE));
		if (tmUid!=null) {
			assertEquals(tmUid, read(TMUID_FILE));
		}
	}
	
	public void write(String filename, String text) throws TransactionSystemException {
		Path file = Paths.get(folder.getRoot()+"/"+filename);
		try {
			try (OutputStream fos = Files.newOutputStream(file)) {
				fos.write(text.getBytes(StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot write line ["+text+"] to file ["+file+"]", e);
		}
	}
	
	public String read(String filename) {
		Path file = Paths.get(folder.getRoot()+"/"+filename);
		if (!Files.exists(file)) {
			return null;
		}
		try (InputStream fis = Files.newInputStream(file)) {
			return StreamUtils.copyToString(fis, StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot read from file ["+file+"]", e);
		}
	}

}
