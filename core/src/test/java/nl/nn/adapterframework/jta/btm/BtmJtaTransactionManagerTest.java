package nl.nn.adapterframework.jta.btm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import nl.nn.adapterframework.testutil.BTMXADataSourceFactory;

@Ignore
public class BtmJtaTransactionManagerTest {

	public String STATUS_FILE = "status.txt";
	public String TMUID_FILE = "tm-uid.txt";

	private BtmJtaTransactionManager delegateTransactionManager;
	private TemporaryFolder folder;

	@BeforeClass
	public static void ensureBTMisNotActive() {
		if(TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerServices.getTransactionManager().shutdown();
		}
	}

	@AfterClass
	public static void reinstatePreviousTM() {
		BTMXADataSourceFactory.createBtmTransactionManager();
	}

	@Before
	public void setup() throws IOException {
		folder = new TemporaryFolder();
		folder.create();
	}

	@After
	public void tearDown() {
		if (delegateTransactionManager!=null) {
			delegateTransactionManager.shutdownTransactionManager();
		}
		folder.delete();
	}

	public BtmJtaTransactionManager getBtmJtaTransactionManager() {
		BtmJtaTransactionManager result = new BtmJtaTransactionManager();
		result.setStatusFile(folder.getRoot()+"/"+STATUS_FILE);
		result.setUidFile(folder.getRoot()+"/"+TMUID_FILE);
		TransactionManagerServices.getConfiguration().setLogPart1Filename(folder.getRoot()+"/btm1.tlog");
		TransactionManagerServices.getConfiguration().setLogPart2Filename(folder.getRoot()+"/btm2.tlog");
		delegateTransactionManager = result;
		return result;
	}
	
	@Test
	public void testCleanSetup() {
		BtmJtaTransactionManager tm = getBtmJtaTransactionManager();
		tm.afterPropertiesSet();
		BitronixTransactionManager btm = (BitronixTransactionManager)tm.getTransactionManager();
		assertNotNull(btm); // assert that transaction manager is a BitronixTransactionManager

		String btmServerId = TransactionManagerServices.getConfiguration().getServerId();
		String tmUid = tm.getUid();
		assertEquals(btmServerId, tmUid);
		
		assertStatus("ACTIVE", tmUid);
	}
	
	@Test
	public void testPresetTmUid() {
		String presetTmUid = "fakeTmUid";
		write(TMUID_FILE, presetTmUid);
		BtmJtaTransactionManager tm = getBtmJtaTransactionManager();
		tm.afterPropertiesSet();
		BitronixTransactionManager btm = (BitronixTransactionManager)tm.getTransactionManager();
		assertNotNull(btm); // assert that transaction manager is a BitronixTransactionManager

		assertEquals(presetTmUid, TransactionManagerServices.getConfiguration().getServerId());
		assertEquals(presetTmUid, tm.getUid());
		
		assertStatus("ACTIVE", presetTmUid);
	}
	
	@Test
	public void testCleanShutdown() {
		BtmJtaTransactionManager tm = getBtmJtaTransactionManager();
		tm.afterPropertiesSet();
		BitronixTransactionManager btm = (BitronixTransactionManager)tm.getTransactionManager();
		assertNotNull(btm); // assert that transaction manager is a BitronixTransactionManager
		String tmUid = tm.getUid();
		assertNotNull(tmUid);

		assertStatus("ACTIVE", tmUid);
		
		tm.destroy();

		assertStatus("COMPLETED", tmUid);
	}
	
	@Ignore("This test takes 1 minute to executed")
	@Test
	public void testShutdownWithPendingTransactions() throws NotSupportedException, SystemException {
		TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(1);
		BtmJtaTransactionManager tm = getBtmJtaTransactionManager();
		tm.afterPropertiesSet();
		BitronixTransactionManager btm = (BitronixTransactionManager)tm.getTransactionManager();
		btm.begin();
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
