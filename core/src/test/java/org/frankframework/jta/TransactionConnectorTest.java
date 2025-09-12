package org.frankframework.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.log4j.Log4j2;

import org.frankframework.jdbc.datasource.JdbcPoolUtil;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.DatabaseTestOptions;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;

@Log4j2
@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TransactionConnectorTest.TEST_TABLE)
public class TransactionConnectorTest {
	static final String TEST_TABLE = "tx_temp_table";
	private IThreadConnectableTransactionManager txManager;
	private DatabaseTestEnvironment env;

	private static final int TX_DEF_REQUIRES_NEW = TransactionDefinition.PROPAGATION_REQUIRES_NEW;

	@BeforeAll
	public static void beforeAll() {
		// With NARAYANA Transaction Manager, this test needs Connection Pooling to pass.
		System.setProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize", "2");
	}

	@AfterAll
	public static void afterAll() {
		System.clearProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize");
		AppConstants.removeInstance();
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		this.env = env;
		txManager = (IThreadConnectableTransactionManager) env.getTxManager();
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testSimpleTransaction() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF_REQUIRES_NEW);

		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

		} finally {
			if (txStatus.isRollbackOnly()) {
				txManager.rollback(txStatus);
				assertEquals(1, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
			} else {
				txManager.commit(txStatus);
				assertEquals(2, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
			}
		}
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	@Disabled("Something in this test is not right yet at the moment")
	public void testNewTransactionMustLock() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF_REQUIRES_NEW, 10);

		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			log.info("Starting nested transaction");
			TransactionStatus txStatus2 = env.startTransaction(TX_DEF_REQUIRES_NEW, 5);
			try {
				int count = runQuery("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
				log.warn("updateRowCount = " + count);
				assertEquals(0, count, "If there was no exception, then count of rows updated must be 0");
			} catch (Exception e) {
				log.info("exception from nested transaction", e);
			} finally {
				if (txStatus2.isRollbackOnly()) {
					txManager.rollback(txStatus2);
				} else {
					txManager.commit(txStatus2);
				}
			}
		} catch (Exception e) {
			log.info("exception caught from outer transaction", e);
		} finally {
			if (txStatus.isRollbackOnly()) {
				txManager.rollback(txStatus);
				fail("expected commit");
			} else {
				txManager.commit(txStatus);
			}
		}
		assertEquals(2, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testBasicSameThread() throws Exception {
		if (!"DATASOURCE".equals(env.getName())) {
			assertTrue(JdbcPoolUtil.isXaCapable(env.getDataSource()), "In environment [" + env.getName() + "] the datasource [" + env.getDataSourceName() + "] should be XA-Capable but it was not");
			assertTrue(JdbcPoolUtil.isXaCapable(txManager), "In environment [" + env.getName() + "] the transaction manager should be XA-Capable but it was not");
		}

		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");

		log.info("<*> Is transaction Active: " + TransactionSynchronizationManager.isActualTransactionActive());
		displayTransaction();
		TransactionStatus txStatus = env.startTransaction(TX_DEF_REQUIRES_NEW);
		displayTransaction();

		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");
			displayTransaction();

			runQuery("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
		} finally {
			env.getTxManager().commit(txStatus);
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testBasic() throws Exception {
		if (!"DATASOURCE".equals(env.getName())) {
			assertTrue(JdbcPoolUtil.isXaCapable(env.getDataSource()), "In environment [" + env.getName() + "] the datasource [" + env.getDataSourceName() + "] should be XA-Capable but it was not");
			assertTrue(JdbcPoolUtil.isXaCapable(txManager), "In environment [" + env.getName() + "] the transaction manager should be XA-Capable but it was not");
		}
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF_REQUIRES_NEW);

		// do some action in main thread
		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			try {
				boolean wasTxActiveInChildThread = runInConnectedChildThread("UPDATE " + TEST_TABLE + " SET TINT=3 WHERE TKEY=999 AND TINT=2");
				assertTrue(wasTxActiveInChildThread, "A transaction should have been active in the connected child-thread");
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testNoOuterTransaction() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		// do some action in main thread
		runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

		try {
			boolean wasTxActiveInChildThread = runInConnectedChildThread("UPDATE " + TEST_TABLE + " SET TINT=3 WHERE TKEY=999 AND TINT=2");
			assertFalse(wasTxActiveInChildThread, "No transaction should have been active in the connected child-thread");
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			fail();
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	// TODO: How does this test trigger a rollback in the child thread??? Should we try to make that so?
	@DatabaseTestOptions(cleanupBeforeUse = true, cleanupAfterUse = true)
	@TxManagerTest
	public void testBasicRollbackInChildThread() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");

		TransactionStatus txStatus = env.startTransaction(TX_DEF_REQUIRES_NEW);
		// do some action in main thread
		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			try {
				boolean wasTxActiveInChildThread = runInConnectedChildThread("UPDATE " + TEST_TABLE + " SET TINT=3 WHERE TKEY=999 AND TINT=2");
				assertTrue(wasTxActiveInChildThread, "A transaction should have been active in the connected child-thread");
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	private int runQuery(String query) throws SQLException {
		try (Connection con = env.getConnection(); PreparedStatement stmt = con.prepareStatement(query)) {
			TimeoutGuard guard = new TimeoutGuard(3, "run query") {

				@Override
				protected void abort() {
					try {
						log.warn("--> TIMEOUT executing [{}]", query);
						stmt.cancel();
					} catch (SQLException e) {
						log.warn(e.getMessage(), e);
					}
				}

			};
			try {
				log.debug("runQuery thread ["+Thread.currentThread().getId()+"] query ["+query+"] ");
				stmt.execute();
				return stmt.getUpdateCount();
			} finally {
				if (guard.cancel()) {
					throw new SQLException("Interrupted ["+query+"]");
				}
			}
		}
	}

	private int runSelectQuery(String query) throws SQLException {
		try (Connection con = env.getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(query)) {
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					return rs.getInt(1);
				}
			}
		}
	}

	public boolean runInConnectedChildThread(String query) throws InterruptedException {
		AtomicBoolean isTxActive = new AtomicBoolean(false);
		try (TransactionConnector transactionConnector = TransactionConnector.getInstance(txManager, null, null)) {
			if (transactionConnector == null) {
				log.warn("transaction connector is null");
			}
			Thread thread = new Thread(() -> {
				if (transactionConnector!=null) transactionConnector.beginChildThread();
				isTxActive.set(TransactionSynchronizationManager.isActualTransactionActive());
				try {
					runQuery(query);
				} catch (Throwable e) {
					log.warn(ClassUtils.nameOf(e)+": "+e.getMessage());
				} finally {
					if (transactionConnector!=null) transactionConnector.endChildThread();
				}
			});
			thread.start();
			thread.join();
		}
		return isTxActive.get();
	}

	public void displayTransaction() throws SystemException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		if (txManager != null) {
			IThreadConnectableTransactionManager tctm = txManager;
			Object transaction = tctm.getCurrentTransaction();
			if (transaction instanceof JtaTransactionObject object) {
				UserTransaction ut =object.getUserTransaction();
				System.out.println("-> UserTransaction status: "+ut.getStatus());
			} else {
				return;
			}
			Object resources = tctm.suspendTransaction(transaction);
			tctm.resumeTransaction(transaction, resources);
			System.out.println("-> Transaction: "+ToStringBuilder.reflectionToString(transaction, ToStringStyle.MULTI_LINE_STYLE));
			System.out.println("-> Resources: "+ToStringBuilder.reflectionToString(resources, ToStringStyle.MULTI_LINE_STYLE));

			Object wasActive = ClassUtils.getDeclaredFieldValue(resources, "wasActive");
			System.out.println("-> wasActive: "+wasActive);

			Object suspendedResources = ClassUtils.getDeclaredFieldValue(resources, "suspendedResources");
			if (suspendedResources!=null) {
				System.out.println("-> suspendedResources: "+ToStringBuilder.reflectionToString(suspendedResources, ToStringStyle.MULTI_LINE_STYLE));
			}
		}
	}
}
