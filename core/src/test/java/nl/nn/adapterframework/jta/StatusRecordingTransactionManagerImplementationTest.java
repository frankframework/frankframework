package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jta.xa.XaDatasourceCommitStopper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

@RunWith(Parameterized.class)
public class StatusRecordingTransactionManagerImplementationTest {
	protected Logger log = LogUtil.getLogger(this);

	private static TransactionManagerType singleTransactionManagerType = TransactionManagerType.NARAYANA; // set to a specific transaction manager type, to speed up testing

	protected SpringTxManagerProxy txManager;
	protected StatusRecordingTransactionManager txManagerReal;
	private @Getter TestConfiguration configuration;
	
	private String STATUS_FILE;
	private String TMUID_FILE;
	
	private String tableName;

	@Parameters(name= "{0}")
	public static Collection data() throws NamingException {
		TransactionManagerType[] transactionManagerTypes = { singleTransactionManagerType };
		if (singleTransactionManagerType==null) {
			transactionManagerTypes = TransactionManagerType.values();
		}
		List<Object> list = new ArrayList<>();

		for(TransactionManagerType type: transactionManagerTypes) {
			list.add(type);
		}

		return list;
	}

	@Parameterized.Parameter(0)
	public @Getter TransactionManagerType transactionManagerType;
	

	@Before
	public void setup() {
		configuration = transactionManagerType.getConfigurationContext("H2");
		assumeFalse(transactionManagerType==TransactionManagerType.DATASOURCE);
		txManager = configuration.getBean(SpringTxManagerProxy.class, "txManager");
		txManagerReal = configuration.getBean(StatusRecordingTransactionManager.class, "txReal");
		STATUS_FILE = txManagerReal.getStatusFile();
		TMUID_FILE = txManagerReal.getUidFile();
		tableName = "tmp_"+transactionManagerType;
		log.debug("STATUS_FILE ["+STATUS_FILE+"]");
	}
	
	@After
	public void teardown() {
		try {
			transactionManagerType.closeConfigurationContext();
		} catch (Exception e) {
			log.warn("Exception in teardown", e);
		}
		XaDatasourceCommitStopper.stop(false);
	}
	@Test
	public void testSetup() {
		assertStatus("ACTIVE", txManagerReal.getUid());
	}

	@Test
	public void testShutdown() throws Exception {
		assertStatus("ACTIVE", txManagerReal.getUid());
		prepareTable("H2");
		prepareTable("Oracle");
		runXATransactionInThread();
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());
		
	}
	
	@Test
	public void testShutdownPending() throws Exception {
		assertStatus("ACTIVE", txManagerReal.getUid());
		prepareTable("H2");
		prepareTable("Oracle");
		XaDatasourceCommitStopper.stop(true);
		runXATransactionInThread();
		XaDatasourceCommitStopper.commitCalled.acquire();
		txManagerReal.destroy();
		assertStatus("PENDING", txManagerReal.getUid());
		
		teardown();
		XaDatasourceCommitStopper.stop(false);
		setup();
		RecoveryManager manager = RecoveryManager.manager();
		log.info("Start scan 1");
		manager.scan();
		log.info("End scan 1");
		log.info("sleeping 10 seconds, to let second recovery manager pass kick in");
		Thread.sleep(10000);
		log.info("Start scan 2");
		manager.scan();
		log.info("End scan 2");
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());

	}
		
	public Semaphore semaphore;
	
	public void prepareTable(String datasourceName) throws ConfigurationException, SenderException, TimeoutException {
		DirectQuerySender fs1 = new DirectQuerySender();
		configuration.autowireByName(fs1);
		fs1.setName("fs1");
		fs1.setDatasourceName(datasourceName);
		fs1.configure();
		fs1.open();
		try {
			fs1.sendMessage(new Message("DROP TABLE "+tableName),null);
		} catch (Exception e) {
			log.warn(e);
		}

		fs1.sendMessage(new Message("CREATE TABLE "+tableName+"(id char(1))"),null);
		fs1.close();
	}
	
	private void runXATransactionInThread() throws InterruptedException {
		semaphore = new Semaphore();
		Thread thread = new Thread(()->runXATransaction());
		thread.start();
		semaphore.acquire();
		semaphore = null;
	}
	
	private void runXATransaction() {
		try {
			DirectQuerySender fs1 = new DirectQuerySender();
			configuration.autowireByName(fs1);
			fs1.setName("fs1");
			fs1.setDatasourceName("H2");
			fs1.configure();
			
			DirectQuerySender fs2 = new DirectQuerySender();
			configuration.autowireByName(fs2);
			fs2.setName("fs1");
			fs2.setDatasourceName("Oracle");
			fs2.configure();
			
			fs1.open();
			fs2.open();
			
			TransactionDefinition txDef = txManager.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 10);
			TransactionStatus txStatus = txManager.getTransaction(txDef);
			try {
				fs1.sendMessage(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
				fs2.sendMessage(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
				
				if (semaphore!=null) {
					semaphore.release();
				}
				
			} finally {
				txManager.commit(txStatus);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	private S delegateTransactionManager;
//	public @Rule TemporaryFolder folder = new TemporaryFolder();

//	protected abstract S createTransactionManager();
//	protected abstract String getTMUID(T tm);

//	@Before
//	public void setup() {
//		log.debug("setup");
//		delete(TMUID_FILE);
//	}
//
//	@After
//	public void tearDown() {
//		if (delegateTransactionManager != null) {
//			delegateTransactionManager.shutdownTransactionManager();
//			delegateTransactionManager = null;
//		}
//	}
//
//
//	protected S setupTransactionManager() {
//		log.debug("setupTransactionManager folder ["+folder.getRoot().toString()+"]");
//		S result = createTransactionManager();
//		result.setStatusFile(folder.getRoot()+"/"+STATUS_FILE);
//		result.setUidFile(folder.getRoot()+"/"+TMUID_FILE);
//		delegateTransactionManager = result;
//		return result;
//	}
//
//	@Test
//	public void testCleanSetup() {
//		log.debug("--> testCleanSetup)");
//		S tm = setupTransactionManager();
//		tm.afterPropertiesSet();
//		T delegateTm = (T)tm.getTransactionManager();
//		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager
//
//		String serverId = getTMUID(delegateTm);
//		String tmUid = tm.getUid();
//		assertEquals(serverId, tmUid);
//
//		assertStatus("ACTIVE", tmUid);
//	}
//
//	@Test
//	public void testPresetTmUid() {
//		log.debug("--> testPresetTmUid)");
//		String presetTmUid = "fakeTmUid";
//		write(TMUID_FILE, presetTmUid);
//		S tm = setupTransactionManager();
//		tm.afterPropertiesSet();
//		T delegateTm = (T)tm.getTransactionManager();
//		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager
//
//		String serverId = getTMUID(delegateTm);
//
//		assertEquals(presetTmUid, serverId);
//		assertEquals(presetTmUid, tm.getUid());
//
//		assertStatus("ACTIVE", presetTmUid);
//	}
//
//	@Test
//	public void testCleanShutdown() {
//		log.debug("--> testCleanShutdown)");
//		S tm = setupTransactionManager();
//		tm.afterPropertiesSet();
//		T delegateTm = (T)tm.getTransactionManager();
//		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager
//
//		String tmUid = getTMUID(delegateTm);
//		assertNotNull(tmUid);
//
//		assertStatus("ACTIVE", tmUid);
//
//		tm.destroy();
//
//		assertStatus("COMPLETED", tmUid);
//	}
//
//	@Test
//	@Ignore
//	public void testShutdownWithPendingTransactions() throws NotSupportedException, SystemException {
//		log.debug("--> testShutdownWithPendingTransactions)");
//		//TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(1);
//		S tm = setupTransactionManager();
//		tm.afterPropertiesSet();
//		T delegateTm = (T)tm.getTransactionManager();
//		assertNotNull(delegateTm); // assert that transaction manager is a javax.transaction.TransactionManager
//
//		delegateTm.begin();
//
//		String tmUid = tm.getUid();
//		assertNotNull(tmUid);
//
//		assertStatus("ACTIVE", tmUid);
//
//		tm.destroy();
//
//		assertStatus("PENDING", tmUid);
//	}

//	private void createPendingTransaction() {
//		delegateTransactionManager.getTransactionManager().begin();
//	}
	
	public void assertStatus(String status, String tmUid) {
		log.debug("testing file ["+STATUS_FILE+"] for status ["+status+"]");
		assertEquals(status, read(STATUS_FILE));
		if (tmUid!=null) {
			assertEquals(tmUid, read(TMUID_FILE));
		}
	}

	public void delete(String filename) throws TransactionSystemException {
		Path file = Paths.get(filename);
		try {
			if (Files.exists(file)) {
				Files.delete(file);
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot delete file ["+file+"]", e);
		}
	}

	public void write(String filename, String text) throws TransactionSystemException {
		Path file = Paths.get(filename);
		try {
			try (OutputStream fos = Files.newOutputStream(file)) {
				fos.write(text.getBytes(StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			throw new TransactionSystemException("Cannot write line ["+text+"] to file ["+file+"]", e);
		}
	}

	public String read(String filename) {
		Path file = Paths.get(filename);
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
