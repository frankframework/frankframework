package org.frankframework.jta;

import static org.frankframework.dbms.Dbms.H2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ConcurrentActionTester;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;

public class StatusRecordingTransactionManagerImplementationTest extends StatusRecordingTransactionManagerTestBase<AbstractStatusRecordingTransactionManager> {

	private static final String SECONDARY_PRODUCT = "H2";

	protected SpringTxManagerProxy txManager;
	protected AbstractStatusRecordingTransactionManager txManagerReal;
	private @Getter TestConfiguration configuration;
	protected DatabaseTestEnvironment env;

	private String tableName;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		assumeFalse("DATASOURCE".equals(env.getName()), "Testing XA transaction-manager, not the DatasourceTransactionManager");
		assumeFalse(H2 == env.getDbmsSupport().getDbms(), "Cannot run this test with H2");

		// Release any hanging commits that might be from previous tests
		XaDatasourceCommitStopper.stop(false);

		super.setup();
		this.env = env;
	}

	@Override
	protected AbstractStatusRecordingTransactionManager createTransactionManager() {
		configuration = env.getConfiguration();
		txManager = (SpringTxManagerProxy) env.getTxManager();
		txManagerReal = configuration.getBean(AbstractStatusRecordingTransactionManager.class, "txReal");
		statusFile = txManagerReal.getStatusFile();
		tmUidFile = txManagerReal.getUidFile();
		log.debug("statusFile [{}], tmUidFile [{}]", statusFile, tmUidFile);
		tableName = "tmp_"+env.getName();
		return txManagerReal;
	}

	@AfterEach
	public void teardown() {
		log.debug("teardown");
		XaDatasourceCommitStopper.stop(false);
	}

	protected String getTMUID() {
		return switch (env.getName()) {
			case "NARAYANA" -> arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
			default -> throw new NotImplementedException("Unknown transaction manager type [" + env.getName() + "]");
		};
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testSetup() {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(), getTMUID());
	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testShutdown() {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(), getTMUID());
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester();
		xaTester.start(); // same thread
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());

	}

	@DatabaseTest(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	@Disabled("This test fails for some databases and hangs for others. Needs to be investigated. (See issue #6935)")
	public void testShutdownPending() {
		setupTransactionManager();
		String uid = txManagerReal.getUid();
		assertStatus("ACTIVE", uid);
		XaDatasourceCommitStopper.stop(true);
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester();
		// Register each ConcurrentXATransactionTester instance right away
		XaDatasourceCommitStopper.commitGuard.register();

		xaTester.start();
		// Wait for all others to have arrived here too.
		log.info("<*> Nr of participants: {}", XaDatasourceCommitStopper.commitGuard.getRegisteredParties());
		log.info("Waiting for all other participants to arrive in 'commit'");
		int tst1 = XaDatasourceCommitStopper.commitGuard.arriveAndAwaitAdvance();
		log.info("<*> Phase at Tst1: {}", tst1);

		txManagerReal.destroy();
		assertStatus("PENDING", uid);

		log.info("<*> Allow blocked transactions to proceed");
		XaDatasourceCommitStopper.stop(false);

		log.info("<*> Recreating transaction manager");
		setupTransactionManager();

		assertEquals(uid, txManagerReal.getUid(), "tmuid must be the same after restart");
		txManagerReal.destroy();
		assertStatus("COMPLETED", uid);
	}

	public void prepareTable(String datasourceName) throws ConfigurationException, SenderException, TimeoutException {
		DirectQuerySender fs1 = new DirectQuerySender();
		configuration.autowireByName(fs1);
		fs1.setName("fs1");
		fs1.setDatasourceName(datasourceName);
		fs1.configure();
		fs1.start();
		try {
			fs1.sendMessageOrThrow(new Message("DROP TABLE "+tableName),new PipeLineSession());
		} catch (Exception e) {
			log.warn(e);
		}

		fs1.sendMessageOrThrow(new Message("CREATE TABLE "+tableName+"(id char(1))"),new PipeLineSession());
		fs1.stop();
	}

	private class ConcurrentXATransactionTester extends ConcurrentActionTester {

		@Override
		public void initAction() throws ConfigurationException, SenderException, TimeoutException {
			prepareTable(env.getDataSourceName());
			prepareTable(SECONDARY_PRODUCT);
		}

		@Override
		public void action() throws ConfigurationException, SenderException, TimeoutException {
			DirectQuerySender fs1 = new DirectQuerySender();
			configuration.autowireByName(fs1);
			fs1.setName("fs1");
			fs1.setDatasourceName(env.getDataSourceName());
			fs1.configure();

			DirectQuerySender fs2 = new DirectQuerySender();
			configuration.autowireByName(fs2);
			fs2.setName("fs1");
			fs2.setDatasourceName(SECONDARY_PRODUCT);
			fs2.configure();

			fs1.start();
			fs2.start();

			TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 10);
			TransactionStatus txStatus = txManager.getTransaction(txDef);
			try (PipeLineSession pls = new PipeLineSession()) {

				fs1.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),pls);
				fs2.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),pls);
			} finally {
				txManager.commit(txStatus);
			}
		}
	}
}
