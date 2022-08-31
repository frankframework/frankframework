package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import bitronix.tm.TransactionManagerServices;
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jta.xa.XaDatasourceCommitStopper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.Semaphore;

@RunWith(Parameterized.class)
public class StatusRecordingTransactionManagerImplementationTest<S extends StatusRecordingTransactionManager> extends StatusRecordingTransactionManagerTestBase<S> {

	private static TransactionManagerType singleTransactionManagerType = null; // set to a specific transaction manager type, to speed up testing

	protected SpringTxManagerProxy txManager;
	protected StatusRecordingTransactionManager txManagerReal;
	private @Getter TestConfiguration configuration;

	private String tableName;

	@Parameterized.Parameter(0)
	public @Getter TransactionManagerType transactionManagerType;


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


	@BeforeClass
	public static void init() {
		for (TransactionManagerType tmt:TransactionManagerType.values()) {
			tmt.closeConfigurationContext();
		}
	}

	@Override
	public void setup() throws IOException {
		assumeFalse(transactionManagerType==TransactionManagerType.DATASOURCE);
		super.setup();
	}

	@Override
	protected S createTransactionManager() {
		configuration = transactionManagerType.getConfigurationContext("H2");
		txManager = configuration.getBean(SpringTxManagerProxy.class, "txManager");
		txManagerReal = configuration.getBean(StatusRecordingTransactionManager.class, "txReal");
		statusFile = txManagerReal.getStatusFile();
		tmUidFile = txManagerReal.getUidFile();
		log.debug("statusFile [{}], tmUidFile [{}]", statusFile, tmUidFile);
		tableName = "tmp_"+transactionManagerType;
		return (S)txManagerReal;
	}

	@After
	public void teardown() {
		log.debug("teardown");
		try {
			transactionManagerType.closeConfigurationContext();
		} catch (Exception e) {
			log.warn("Exception in teardown", e);
		}
		XaDatasourceCommitStopper.stop(false);
	}

	protected String getTMUID() {
		switch (transactionManagerType) {
		case DATASOURCE:
			return null;
		case BTM:
			return TransactionManagerServices.getConfiguration().getServerId();
		case NARAYANA:
			return arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
		default:
			throw new NotImplementedException("Unkonwn transaction manager type ["+transactionManagerType+"]");
		}
	}



	@Test
	public void testSetup() {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(),getTMUID());
	}

	@Test
	public void testShutdown() throws Exception {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(),getTMUID());
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester();
		xaTester.run(); // same thread
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());

	}

	@Test
	public void testShutdownPending() throws Exception {
		setupTransactionManager();
		String uid = txManagerReal.getUid();
		assertStatus("ACTIVE", uid);
		XaDatasourceCommitStopper.stop(true);
		Semaphore actionDone = new Semaphore();
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester();
		xaTester.setActionDone(actionDone);

		xaTester.start();
		XaDatasourceCommitStopper.commitCalled.acquire();
		txManagerReal.destroy();
		assertStatus("PENDING", uid);

		teardown();
		XaDatasourceCommitStopper.performCommit.release();
		XaDatasourceCommitStopper.stop(false);

//		log.debug("waiting for commit to finish");
//		actionDone.acquire();
		log.debug("recreating transaction manager");
		setupTransactionManager();

		assertEquals("tmuid must be the same after restart", uid, txManagerReal.getUid());
		txManagerReal.destroy();
		assertStatus("COMPLETED", uid);
	}


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

	private class ConcurrentXATransactionTester extends ConcurrentActionTester {

		@Override
		public void initAction() throws Exception {
			prepareTable("H2");
			prepareTable("Oracle");
		}

		@Override
		public void action() throws Exception {
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
			} finally {
				txManager.commit(txStatus);
			}
		}


	}


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

}
