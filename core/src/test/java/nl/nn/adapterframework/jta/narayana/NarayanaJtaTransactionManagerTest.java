package nl.nn.adapterframework.jta.narayana;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;

import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.LogUtil;

public class NarayanaJtaTransactionManagerTest {
	protected Logger log = LogUtil.getLogger(this);

	public String STATUS_FILE = "status.txt";
	public String TMUID_FILE = "tm-uid.txt";

	private NarayanaJtaTransactionManager delegateTransactionManager;
	public @Rule TemporaryFolder folder = new TemporaryFolder();

	@BeforeClass
	public static void ensureNarayanaisNotActive() {
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerType.NARAYANA.closeConfigurationContext();
//		}
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
//			fail("unable to shutdown NARAYANA TransactionManager");
//		}
	}

	@AfterClass
	public static void validateNoTXIsActive() {
//		if(TransactionManagerServices.isTransactionManagerRunning()) {
//			fail("TransactionManager still running");
//		}
	}

	@Before
	public void setup() {
		log.debug("setup");
		delete(TMUID_FILE);
	}

	@After
	public void tearDown() {
		log.debug("start teardown");
		if (delegateTransactionManager != null) {
			delegateTransactionManager.shutdownTransactionManager();
			delegateTransactionManager = null;
		}
		log.debug("end teardown");
	}

	private NarayanaJtaTransactionManager getNarayanaJtaTransactionManager() throws Exception {

		log.debug("getNarayanaJtaTransactionManager folder ["+folder.getRoot().toString()+"]");

		NarayanaJtaTransactionManager result = new NarayanaJtaTransactionManager();
		Properties props = new Properties();
		props.setProperty("JDBCEnvironmentBean.isolationLevel", "2" );
		props.setProperty("ObjectStoreEnvironmentBean.objectStoreDir", folder.getRoot().toString());
		props.setProperty("ObjectStoreEnvironmentBean.stateStore.objectStoreDir", folder.getRoot().toString());
		props.setProperty("ObjectStoreEnvironmentBean.communicationStore.objectStoreDir", folder.getRoot().toString());

		NarayanaConfigurationBean config = new NarayanaConfigurationBean();
		config.setProperties(props);
		config.afterPropertiesSet();

		result.setStatusFile(folder.getRoot().toString()+"/"+STATUS_FILE);
		result.setUidFile(folder.getRoot().toString()+"/"+TMUID_FILE);
		delegateTransactionManager = result;
		return result;
	}

	@Test
	public void testCleanSetup() throws Exception {
		NarayanaJtaTransactionManager tm = getNarayanaJtaTransactionManager();
		tm.afterPropertiesSet();
		TransactionManagerImple ntm = (TransactionManagerImple)tm.getTransactionManager();
		assertNotNull(ntm); // assert that transaction manager is a Narayana TransactionManager

		String narayanaServerId = arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
		String tmUid = tm.getUid();


		assertEquals(tmUid, narayanaServerId);
		assertStatus("ACTIVE", tmUid);
	}

	@Test
	public void testPresetTmUid() throws Exception {
		String presetTmUid = "fakeTmUid";
		write(TMUID_FILE, presetTmUid);
		NarayanaJtaTransactionManager tm = getNarayanaJtaTransactionManager();
		tm.afterPropertiesSet();
		TransactionManager ntm = tm.getTransactionManager();
		assertNotNull(ntm); // assert that transaction manager is a Narayana-transaction manager

		String narayanaServerId = arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
		assertEquals(presetTmUid, narayanaServerId);
		assertEquals(presetTmUid, tm.getUid());

		assertStatus("ACTIVE", presetTmUid);
	}

	@Test
	public void testCleanShutdown() throws Exception {
		NarayanaJtaTransactionManager tm = getNarayanaJtaTransactionManager();
		tm.afterPropertiesSet();
		TransactionManager ntm = tm.getTransactionManager();
		assertNotNull(ntm); // assert that transaction manager is a BitronixTransactionManager
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();
		delegateTransactionManager=null;

		assertStatus("COMPLETED", tmUid);
	}

	@Ignore("For a proper test, we need to set up a transaction that cannot be completed or rolled back")
	@Test
	public void testShutdownWithPendingTransactions() throws Exception {
		NarayanaJtaTransactionManager tm = getNarayanaJtaTransactionManager();
		tm.afterPropertiesSet();
		TransactionManager ntm = tm.getTransactionManager();
		ntm.setTransactionTimeout(1);
		ntm.begin();
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);

		tm.destroy();
		delegateTransactionManager=null;

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
		Path file = Paths.get(folder.getRoot().toString()+"/"+filename);
		try {
			try (OutputStream fos = Files.newOutputStream(file)) {
				fos.write(text.getBytes(StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot write line ["+text+"] to file ["+file+"]", e);
		}
	}

	public String read(String filename) {
		Path file = Paths.get(folder.getRoot().toString()+"/"+filename);
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
