package nl.nn.adapterframework.jta;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.util.Collection;

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
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.jta.xa.XaDatasourceCommitStopper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.Semaphore;

@RunWith(Parameterized.class)
public class StatusRecordingTransactionManagerImplementationTest<S extends StatusRecordingTransactionManager> extends StatusRecordingTransactionManagerTestBase<S> {

	private static final String SECONDARY_PRODUCT = "H2";

	protected SpringTxManagerProxy txManager;
	protected StatusRecordingTransactionManager txManagerReal;
	private @Getter TestConfiguration configuration;

	private String tableName;

	@Parameterized.Parameter(0)
	public @Getter TransactionManagerType transactionManagerType;
	@Parameterized.Parameter(1)
	public String productKey;


	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		return TransactionManagerTestBase.data();
	}


	@BeforeClass
	public static void init() {
		assumeThat(URLDataSourceFactory.availableDatasources, hasItems(SECONDARY_PRODUCT));
		TransactionManagerType.closeAllConfigurationContexts();
	}

	@Override
	public void setup() throws IOException {
		assumeFalse(transactionManagerType==TransactionManagerType.DATASOURCE);
		assumeThat(productKey, not(equalTo(SECONDARY_PRODUCT)));

		assumeFalse("FIXME JDBC/JTA: These tests currently broken with Narayana", transactionManagerType == TransactionManagerType.NARAYANA);

		// Release any hanging commits that might be from previous tests
		XaDatasourceCommitStopper.stop(false);

		super.setup();
	}

	@Override
	protected S createTransactionManager() {
		configuration = transactionManagerType.getConfigurationContext(productKey);
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
		XaDatasourceCommitStopper.stop(false);
		try {
			transactionManagerType.closeConfigurationContext();
		} catch (Exception e) {
			log.warn("Exception in teardown", e);
		}
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
			fs1.sendMessageOrThrow(new Message("DROP TABLE "+tableName),null);
		} catch (Exception e) {
			log.warn(e);
		}

		fs1.sendMessageOrThrow(new Message("CREATE TABLE "+tableName+"(id char(1))"),null);
		fs1.close();
	}

	private class ConcurrentXATransactionTester extends ConcurrentActionTester {

		@Override
		public void initAction() throws Exception {
			prepareTable(productKey);
			prepareTable(SECONDARY_PRODUCT);
		}

		@Override
		public void action() throws Exception {
			DirectQuerySender fs1 = new DirectQuerySender();
			configuration.autowireByName(fs1);
			fs1.setName("fs1");
			fs1.setDatasourceName(productKey);
			fs1.configure();

			DirectQuerySender fs2 = new DirectQuerySender();
			configuration.autowireByName(fs2);
			fs2.setName("fs1");
			fs2.setDatasourceName(SECONDARY_PRODUCT);
			fs2.configure();

			fs1.open();
			fs2.open();

			TransactionDefinition txDef = txManager.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 10);
			TransactionStatus txStatus = txManager.getTransaction(txDef);
			try {

				fs1.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
				fs2.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
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
