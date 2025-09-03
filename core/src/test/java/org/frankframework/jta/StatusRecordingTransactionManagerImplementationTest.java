package org.frankframework.jta;

import static org.frankframework.dbms.Dbms.H2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ConcurrentActionTester;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.DatabaseTestOptions;
import org.frankframework.testutil.junit.JtaTxManagerTest;

public class StatusRecordingTransactionManagerImplementationTest extends StatusRecordingTransactionManagerTestBase<AbstractStatusRecordingTransactionManager> {

	private static final String SECONDARY_PRODUCT = "H2";

	protected SpringTxManagerProxy txManager;
	protected AbstractStatusRecordingTransactionManager txManagerReal;
	private @Getter TestConfiguration configuration;
	protected DatabaseTestEnvironment env;

	private String tableName;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws IOException {
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
		switch (env.getName()) {
		case "DATASOURCE":
			return null;
		case "NARAYANA":
			return arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
		default:
			throw new NotImplementedException("Unknown transaction manager type ["+env.getName()+"]");
		}
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@JtaTxManagerTest
	public void testSetup() {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(), getTMUID());
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@JtaTxManagerTest
	public void testShutdown() {
		setupTransactionManager();
		assertStatus("ACTIVE", txManagerReal.getUid());
		assertEquals(txManagerReal.getUid(), getTMUID());
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester();
		xaTester.start(); // same thread
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());

	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@JtaTxManagerTest
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

		teardown();
		XaDatasourceCommitStopper.stop(false);

		log.debug("recreating transaction manager");
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
			fs1.sendMessageOrThrow(new Message("DROP TABLE "+tableName),null);
		} catch (Exception e) {
			log.warn(e);
		}

		fs1.sendMessageOrThrow(new Message("CREATE TABLE "+tableName+"(id char(1))"),null);
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
			try {

				fs1.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
				fs2.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),null);
			} finally {
				txManager.commit(txStatus);
			}
		}
	}
}
