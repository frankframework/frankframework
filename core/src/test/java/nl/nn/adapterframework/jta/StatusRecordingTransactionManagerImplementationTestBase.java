package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import nl.nn.adapterframework.util.LogUtil;

public abstract class StatusRecordingTransactionManagerImplementationTestBase<T extends StatusRecordingTransactionManager, TM extends TransactionManager, UT extends UserTransaction> {
	protected Logger log = LogUtil.getLogger(this);

	public String STATUS_FILE = "status.txt";
	public String TMUID_FILE = "tm-uid.txt";

	private T delegateTransactionManager;
	public @Rule TemporaryFolder folder = new TemporaryFolder();

	protected abstract T createTransactionManager();
	protected abstract String getTMUID(TM tm);

	@Before
	public void setup() {
		log.debug("setup");
		delete(TMUID_FILE);
	}

	@After
	public void tearDown() {
		if (delegateTransactionManager != null) {
			delegateTransactionManager.shutdownTransactionManager();
			delegateTransactionManager = null;
		}
	}


	protected T setupTransactionManager() {
		log.debug("setupTransactionManager folder ["+folder.getRoot().toString()+"]");
		T result = createTransactionManager();
		result.setStatusFile(folder.getRoot()+"/"+STATUS_FILE);
		result.setUidFile(folder.getRoot()+"/"+TMUID_FILE);
		delegateTransactionManager = result;
		return result;
	}

	@Test
	public void testCleanSetup() {
		T tm = setupTransactionManager();
		tm.afterPropertiesSet();
		TM delegateTm = (TM)tm.getTransactionManager();
		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager

		String serverId = getTMUID(delegateTm);
		String tmUid = tm.getUid();
		assertEquals(serverId, tmUid);

		assertStatus("ACTIVE", tmUid);
	}

	@Test
	public void testPresetTmUid() {
		String presetTmUid = "fakeTmUid";
		write(TMUID_FILE, presetTmUid);
		T tm = setupTransactionManager();
		tm.afterPropertiesSet();
		TM delegateTm = (TM)tm.getTransactionManager();
		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager

		String serverId = getTMUID(delegateTm);

		assertEquals(presetTmUid, serverId);
		assertEquals(presetTmUid, tm.getUid());

		assertStatus("ACTIVE", presetTmUid);
	}

	@Test
	public void testCleanShutdown() {
		T tm = setupTransactionManager();
		tm.afterPropertiesSet();
		TM delegateTm = (TM)tm.getTransactionManager();
		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager

		String tmUid = getTMUID(delegateTm);
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();

		assertStatus("COMPLETED", tmUid);
	}

	@Test
	public void testShutdownWithPendingTransactions() throws NotSupportedException, SystemException {
		//TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(1);
		T tm = setupTransactionManager();
		tm.afterPropertiesSet();
		TM delegateTm = (TM)tm.getTransactionManager();
		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager

		delegateTm.begin();

		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();

		assertStatus("PENDING", tmUid);
	}

	public void assertStatus(String status, String tmUid) {
		assertEquals(status, read(STATUS_FILE));
		if (tmUid!=null) {
			assertEquals(tmUid, read(TMUID_FILE));
		}
	}

	public void delete(String filename) throws TransactionSystemException {
		Path file = Paths.get(folder.getRoot().toString()+"/"+filename);
		try {
			if (Files.exists(file)) {
				Files.delete(file);
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot delete file ["+file+"]", e);
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
