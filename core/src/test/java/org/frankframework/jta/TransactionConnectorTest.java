package org.frankframework.jta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionObject;

import lombok.extern.log4j.Log4j2;

import org.frankframework.task.TimeoutGuard;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.ClassUtils;

@Log4j2
@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = TransactionConnectorTest.TEST_TABLE)
@Disabled("When this test is enabled, eventually a later test will fail when running Maven (usually the LockerTest) (See issue #6935)")
public class TransactionConnectorTest {
	static final String TEST_TABLE = "temp_table";
	private IThreadConnectableTransactionManager txManager;
	private DatabaseTestEnvironment env;

	private static final int TX_DEF = TransactionDefinition.PROPAGATION_REQUIRES_NEW;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		this.env = env;
		txManager = (IThreadConnectableTransactionManager) env.getTxManager();
	}

	@TxManagerTest
	public void testSimpleTransaction() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF);

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

	@TxManagerTest
	public void testNewTransactionMustLock() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF);

		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			TransactionStatus txStatus2 = env.startTransaction(TX_DEF);
			try {
				runQuery("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Exception e) {
				log.info("expected exception", e);
			} finally {
				if (txStatus2.isRollbackOnly()) {
					txManager.rollback(txStatus2);
				} else {
					txManager.commit(txStatus2);
				}
			}
		} catch (Exception e) {
			log.info("exception caught", e);
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

	@TxManagerTest
	public void testBasicSameThread() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");

		displayTransaction();
		TransactionStatus txStatus = env.startTransaction(TX_DEF);
		displayTransaction();

		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");
			displayTransaction();

			runQuery("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@TxManagerTest
	public void testBasic() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		TransactionStatus txStatus = env.startTransaction(TX_DEF);

		// do some action in main thread
		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			try {
				runInConnectedChildThread("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Throwable t) {
				t.printStackTrace();
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@TxManagerTest
	public void testNoOuterTransaction() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");
		// do some action in main thread
		runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

		try {
			runInConnectedChildThread("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
		assertEquals(3, runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	@TxManagerTest
	public void testBasicRollbackInChildThread() throws Exception {
		runQuery("INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (999, 1)");

		TransactionStatus txStatus = env.startTransaction(TX_DEF);
		// do some action in main thread
		try {
			runQuery("UPDATE "+TEST_TABLE+" SET TINT=2 WHERE TKEY=999");

			try {
				runInConnectedChildThread("UPDATE "+TEST_TABLE+" SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Throwable t) {
				t.printStackTrace();
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=999"));
	}

	private void runQuery(String query) throws SQLException {
		try (Connection con = env.getConnection(); PreparedStatement stmt = con.prepareStatement(query)) {
			TimeoutGuard guard = new TimeoutGuard(3, "run child thread") {

				@Override
				protected void abort() {
					try {
						log.warn("--> TIMEOUT executing [{}]", query);
						stmt.cancel();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

			};
			try {
				log.debug("runQuery thread ["+Thread.currentThread().getId()+"] query ["+query+"] ");
				stmt.execute();
			} finally {
				if (guard.cancel()) {
					throw new SQLException("Interrupted ["+query+"");
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
	public void runInConnectedChildThread(String query) throws InterruptedException {
		try (TransactionConnector transactionConnector = TransactionConnector.getInstance(txManager, null, null)) {
			Thread thread = new Thread() {

				@Override
				public void run() {
					if (transactionConnector!=null) transactionConnector.beginChildThread();
					try {
						runQuery(query);
					} catch (Throwable e) {
						log.warn(ClassUtils.nameOf(e)+": "+e.getMessage());
					} finally {
						if (transactionConnector!=null) transactionConnector.endChildThread();
					}
				}
			};
			thread.start();
			thread.join();
		}
	}

	public void displayTransaction() throws SystemException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		if (txManager instanceof IThreadConnectableTransactionManager) {
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
