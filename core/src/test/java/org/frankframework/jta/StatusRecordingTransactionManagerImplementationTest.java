package org.frankframework.jta;

import static org.awaitility.Awaitility.await;
import static org.frankframework.dbms.Dbms.H2;
import static org.frankframework.dbms.Dbms.MYSQL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.arjuna.ats.arjuna.common.arjPropertyManager;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.jta.xa.XaDataSourceModifier;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.frankframework.jta.xa.XaDatasourceOperationCounter;
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
	public void setup(DatabaseTestEnvironment env) {
		assumeFalse(H2 == env.getDbmsSupport().getDbms(), "Cannot run this test with " + env.getDbmsSupport().getDbmsName());

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
	}

	protected String getTMUID() {
		if (env.getName().equals("NARAYANA")) {
			return arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier();
		}
		throw new IllegalArgumentException("Unknown transaction manager type [" + env.getName() + "]");
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
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester(null);
		xaTester.start(); // same thread
		txManagerReal.destroy();
		assertStatus("COMPLETED", txManagerReal.getUid());

	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@JtaTxManagerTest(resourceObserverFactory = XaDatasourceCommitStopper.class)
	@Timeout(value = 180, unit = TimeUnit.SECONDS)
//	@Disabled("This test is unreliable, often failing in a full build but passing in individual testrun. Might need more time? (Perhaps still see issue #6935)")
	public void testShutdownPending() throws Exception {
		assumeFalse(MYSQL == env.getDbmsSupport().getDbms(), "Cannot run this test with " + env.getDbmsSupport().getDbmsName());

		XaDatasourceCommitStopper commitStopper = XaDataSourceModifier.getXaResourceObserverFactory();
		setupTransactionManager();
		String uid = txManagerReal.getUid();
		assertStatus("ACTIVE", uid);
		commitStopper.blockCommits();
		ConcurrentXATransactionTester xaTester = new ConcurrentXATransactionTester(commitStopper);
		Semaphore actionDone = new Semaphore(0);
		xaTester.setActionDone(actionDone);

		xaTester.start();

		// Wait for all others to have arrived here too.
		log.info("<*> Nr of participants: {}", commitStopper.getNumberOfParticipants());
		log.info("Waiting for all other participants to arrive in 'commit'");
		int tst1 = commitStopper.proceed();
		log.info("<*> Phaser at Tst1: {}", tst1);

		commitStopper.allowNewCommits();
		txManagerReal.destroy();
		assertStatus("PENDING", uid);

		log.info("<*> Allow blocked transactions to proceed");
		commitStopper.unblockPendingCommits();

		log.info("<*> Recreating transaction manager");
		setupTransactionManager();

		assertEquals(uid, txManagerReal.getUid(), "tmuid must be the same after restart");
		Thread.yield();
		boolean actionCompleted = actionDone.tryAcquire(1, 30, TimeUnit.SECONDS);

		txManagerReal.destroy();

		assertTrue(actionCompleted, "action did not complete within 30 seconds");
		assertStatus("COMPLETED", uid);
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@JtaTxManagerTest(resourceObserverFactory = XaDatasourceOperationCounter.class)
	@Timeout(value = 180, unit = TimeUnit.SECONDS)
	public void testRecoverIncompleteTransactions() throws Exception {
		// Arrange
		XaDatasourceOperationCounter opCounter = XaDataSourceModifier.getXaResourceObserverFactory();

		setupTransactionManager();
		prepareTable(env.getDataSourceName());
		String uid = txManagerReal.getUid();
		assertStatus("ACTIVE", uid);

		final DirectQuerySender dqs1 = buildQuerySender(env.getDataSourceName(), "dqs1");
		final DirectQuerySender dqs2 = buildQuerySender(SECONDARY_PRODUCT, "dqs2");

		opCounter.blockNewCommitsAndRollbacks();

		// Act
		ConcurrentActionTester action1 = new ConcurrentActionTester() {
			@Override
			public void action() {
				TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 5);
				doQuery(dqs1, "INSERT INTO "+tableName+" (id) VALUES ('y')");
				doQuery(dqs2, "INSERT INTO "+tableName+" (id) VALUES ('y')");
				env.getTxManager().commit(txStatus);
			}
		};
		ConcurrentActionTester action2 = new ConcurrentActionTester() {
			@Override
			public void action() {
				TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 5);
				doQuery(dqs1, "INSERT INTO "+tableName+" (id) VALUES ('z')");
				doQuery(dqs2, "INSERT INTO "+tableName+" (id) VALUES ('z')");
				env.getTxManager().rollback(txStatus);
			}
		};
		ConcurrentActionTester action3 = new ConcurrentActionTester() {
			@Override
			public void action() {
				TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 1);
				doQuery(dqs1, "INSERT INTO "+tableName+" (id) VALUES ('v')");
				doQuery(dqs2, "INSERT INTO "+tableName+" (id) VALUES ('v')");
				// Do not commit or rollback transaction, let it time out
				// Since this action does not commit or rollback, it is also not a participant in the phaser
				await().atLeast(5, TimeUnit.SECONDS);
			}
		};

		opCounter.registerParticipant(action1);
		opCounter.registerParticipant(action2);
		// Action3 does not commit or rollback so it's not registered as "participant".
		action1.start();
		action2.start();
		action3.start();

		opCounter.awaitActions();

		txManagerReal.destroy();
		assertStatus("PENDING", uid);

		log.info("<*> Recreating transaction manager");
		setupTransactionManager();

		assertEquals(uid, txManagerReal.getUid(), "tmuid must be the same after restart");

		opCounter.allowNewOperations();
		opCounter.awaitOperationsDone();

		action1.join();
		action2.join();
		action3.join();
		Thread.yield();

		txManagerReal.destroy();

		// Assert
		assertStatus("COMPLETED", uid);

		assertAll(
				() -> assertEquals(6, opCounter.getStartCount(), "Expected 6 TX Starts"),
				() -> assertEquals(6, opCounter.getEndCount(), "Expected 6 TX Ends"),
				() -> assertEquals(2, opCounter.getCommitCount(), "Expected a TX commit"),
				() -> assertEquals(4, opCounter.getRollbackCount(), "Expected a TX rollback"),
				() -> assertEquals(0, opCounter.getForgetCount(),  "Expected no TX forget"),
				() -> assertEquals(2, opCounter.getPrepareCount(),  "Expected 2 TX prepares"),
				() -> assertThat("Expected TX Recover", opCounter.getRecoverCount(), greaterThanOrEqualTo(3)),
				() -> assertThat("Expected TX isSameRM", opCounter.getSameRMCount(), greaterThanOrEqualTo(1))
		);
	}

	public void prepareTable(String datasourceName) throws ConfigurationException, SenderException, TimeoutException {
		DirectQuerySender fs1 = buildQuerySender(datasourceName, "fs1");
		try {
			fs1.sendMessageOrThrow(new Message("DROP TABLE "+tableName),new PipeLineSession());
		} catch (Exception e) {
			log.warn(e);
		}

		fs1.sendMessageOrThrow(new Message("CREATE TABLE "+tableName+"(id char(1))"),new PipeLineSession());
		fs1.stop();
	}

	@Nonnull
	private DirectQuerySender buildQuerySender(@Nonnull String datasourceName, @Nonnull String name) throws ConfigurationException {
		DirectQuerySender qs = new DirectQuerySender();
		configuration.autowireByName(qs);
		qs.setName(name);
		qs.setDatasourceName(datasourceName);
		qs.configure(true);
		qs.start();
		return qs;
	}

	private String doQuery(DirectQuerySender qs, String query) {
		try (PipeLineSession session = new PipeLineSession();
			 Message queryMessage = new Message(query)) {
			Message result = qs.sendMessageOrThrow(queryMessage, session);
			return result.asString();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return e.getMessage();
		}
	}

	private class ConcurrentXATransactionTester extends ConcurrentActionTester {

		ConcurrentXATransactionTester(@Nullable XaDatasourceCommitStopper commitStopper) {
			if (commitStopper != null) {
				commitStopper.register();
			}
		}

		@Override
		public void initAction() throws ConfigurationException, SenderException, TimeoutException {
			log.info("Initializing ConcurrentXATransactionTester");
			prepareTable(env.getDataSourceName());
			prepareTable(SECONDARY_PRODUCT);
		}

		@Override
		public void action() throws ConfigurationException, SenderException, TimeoutException {
			log.info("Running ConcurrentXATransactionTester action");

			// Sender 1 inserts into primary database (MariaDB, Oracle, MySQL, etc, not H2)
			DirectQuerySender fs1 = buildQuerySender(env.getDataSourceName(), "fs1");

			// Sender 2 inserts into H2
			DirectQuerySender fs2 = buildQuerySender(SECONDARY_PRODUCT, "fs2");

			TransactionStatus txStatus = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW, 5);
			try (PipeLineSession pls = new PipeLineSession()) {

				fs1.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),pls);
				fs2.sendMessageOrThrow(new Message("INSERT INTO "+tableName+" (id) VALUES ('x')"),pls);
			} finally {
				txManager.commit(txStatus);
			}
		}
	}
}
