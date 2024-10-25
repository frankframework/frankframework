package org.frankframework.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IbisTransaction;
import org.frankframework.core.TransactionAttribute;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.dbms.ConcurrentManagedTransactionTester;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;

@Log4j2
@WithLiquibase(file = "Migrator/CreateLockTable.xml", tableName = LockerTest.TABLENAME)
public class LockerTest {

	static final String TABLENAME = "IBISLOCK";
	private Locker locker;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		locker = env.createBean(Locker.class);
		locker.setFirstDelay(0);


		try(Connection conn = env.getConnection()) {
			if (env.getDbmsSupport().isTablePresent(conn, TABLENAME)) {
				JdbcTestUtil.executeStatement(conn, "DELETE FROM "+TABLENAME);
			}
		}
	}

	@TxManagerTest
	public void testBasicLockNoTransactionManager(DatabaseTestEnvironment env) throws Exception {
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount(env));
	}

	@TxManagerTest
	public void testBasicLockNoTransactionManagerSecondFails(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();
		String objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount(env));

		MessageKeeper messageKeeper = new MessageKeeper();

		objectId = locker.acquire(messageKeeper);
		assertNull(objectId, "Should not be possible to obtain the lock a second time");

		String message = messageKeeper.get(0).getMessageText();
		assertThat(message, containsString("objectId [myLocker]"));
		assertThat(message, containsString("Process locked by host"));
		assertThat(message, containsString("with expiry date"));
	}

	@TxManagerTest
	public void testBasicLockWithTransactionManager(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();

		assertNotNull(locker.acquire());
		assertEquals(1, getRowCount(env));
	}

	@TxManagerTest
	public void testBasicLockWithTransactionManagerSecondFails(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();
		String objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount(env));

		objectId = locker.acquire();
		assertNull(objectId, "Should not be possible to obtain the lock a second time");
	}


	@TxManagerTest
	public void testTakeLockBeforeExecutingInsertInOtherThread(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();

		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore otherReady = new Semaphore(0);
			Semaphore otherContinue = new Semaphore(0);
			Semaphore otherFinished = new Semaphore(0);
			LockerTester lockerTester = new LockerTester(env);

			lockerTester.setInitActionDone(otherReady);
			lockerTester.setWaitBeforeAction(otherContinue);
			lockerTester.setFinalizeActionDone(otherFinished);
			lockerTester.start();

			otherReady.acquire();
			String objectId = locker.acquire();
			otherContinue.release();
			otherFinished.acquire();

			assertNotNull(objectId);
			assertNotNull(lockerTester.getCaught());

		} finally {
			if (testTimeout.cancel()) {
				fail("test timed out");
			}
		}
	}

	@TxManagerTest
	public void testTakeLockFailsAfterExecutingInsertInOtherThread(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();

		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore otherReady = new Semaphore(0);
			Semaphore otherContinue = new Semaphore(0);
			Semaphore otherFinished = new Semaphore(0);
			LockerTester lockerTester = new LockerTester(env);

			lockerTester.setActionDone(otherReady);
			lockerTester.setWaitAfterAction(otherContinue);
			lockerTester.setFinalizeActionDone(otherFinished);
			lockerTester.start();

			otherReady.acquire();
			Timer timer = new Timer("let other thread commit after one second");
			timer.schedule(new TimerTask() {
								@Override
								public void run() {
									otherContinue.release();
								}
							}, 1000L);
			String objectId = locker.acquire();
			otherFinished.acquire();

			assertNull(objectId);
			assertNull(lockerTester.getCaught());

		} finally {
			if (testTimeout.cancel()) {
				fail("test timed out");
			}
		}
	}

	@TxManagerTest
	public void testLockWaitTimeout(DatabaseTestEnvironment env) throws Exception {
		assumeFalse(env.getDbmsSupport().getDbms() == Dbms.ORACLE, "works on Oracle, but causes '(SQLRecoverableException) SQLState [08003], errorCode [17008]: Gesloten verbinding' on subsequent tests");
		locker.setObjectId("myLocker");
		locker.setLockWaitTimeout(1);
		locker.configure();

		boolean lockerUnderTestReturned = false;

		log.debug("Creating Timeout Guard");
		TimeoutGuard testTimeout = new TimeoutGuard(20,"Testtimeout");
		try {
			Semaphore otherInsertReady = new Semaphore(0);
			Semaphore otherContinue = new Semaphore(0);
			Semaphore otherFinished = new Semaphore(0);
			log.debug("Preparing LockerTester");
			LockerTester lockerTester = new LockerTester(env);

			lockerTester.setActionDone(otherInsertReady);
			lockerTester.setWaitAfterAction(otherContinue);
			lockerTester.setFinalizeActionDone(otherFinished);
			log.debug("Inserting lock into table in other thread");
			lockerTester.start();
			log.debug("Waiting for other thread to return from insert");

			otherInsertReady.acquire();
			log.debug("other thread returned from insert, acquiring lock");
			String objectId = locker.acquire();
			assertNull(objectId);

			log.debug("Locker returned, releasing process in other thread to finish");

			lockerUnderTestReturned = true;

			otherContinue.release();
			log.debug("Other threads process released, waiting to finish");
			try {
				otherFinished.acquire();
			} catch (Throwable t) {
				// we do not consider this a failure condition:
				// This test is not about the other thread to complete without problems,
				// only about this thread to wait at most <timeout> seconds for the lock.
				boolean interrupted = Thread.interrupted();
				log.warn("Ignoring exception waiting for other thread to complete, interrupted ["+interrupted+"]", t);
			}

			// N.B. commented out test for other thread:
			// This test is not about the other thread to complete without problems,
			// only about this thread to wait at most <timeout> seconds for the lock.

			//log.debug("Other threads process finished, testing conditions");
			//assertNull(lockerTester.getCaught());
			log.debug("testLockWaitTimeout() passed");

		} catch (Throwable t) {
			log.error("testLockWaitTimeout() threw exception", t);
			throw t;
		} finally {
			log.debug("cancel timeout guard");
			if (testTimeout.cancel()) {
				log.debug("test timed out");
				if (!lockerUnderTestReturned) {
					fail("test timed out");
				}
			}
			log.debug("test did not time out");
		}
	}

	/*
	 * Test the mechanism of the locker.
	 */
	@TxManagerTest
	public void testNoInsertAfterInsert(DatabaseTestEnvironment env) throws Exception {
		locker.setObjectId("myLocker");
		locker.configure();

		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore waitBeforeInsert = new Semaphore(0);
			Semaphore insertDone = new Semaphore(0);
			Semaphore waitBeforeCommit = new Semaphore(0);
			LockerTester other = new LockerTester(env);

			other.setWaitBeforeAction(waitBeforeInsert);
			other.setActionDone(insertDone);
			other.setWaitAfterAction(waitBeforeCommit);

			other.start();

			IbisTransaction mainItx;
			PlatformTransactionManager txManager = env.getConfiguration().getBean("txManager", PlatformTransactionManager.class);
			TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
			mainItx = new IbisTransaction(txManager, txdef, "locker ");

			try (Connection conn = env.getConnection()) {
				waitBeforeInsert.release(); // now this thread has started its transaction, let the other thread do its insert
				insertDone.acquire();		// and wait that to be finished

				try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO IBISLOCK (OBJECTID) VALUES('myLocker')")) {

					SQLException e = assertThrows(SQLException.class, ()-> {
						Timer timer = new Timer("let other thread commit after one second");
						timer.schedule(new TimerTask() {
											@Override
											public void run() {
												waitBeforeCommit.release();
											}
										}, 1000L);
						stmt.executeUpdate();
					});
					if (locker.getDbmsSupport().isConstraintViolation(e) || e.getMessage().toLowerCase().contains("timeout")) {
						log.debug("Caught expected UniqueConstraintViolation or Timeout ("+e.getClass().getName()+"): "+e.getMessage());
					} else {
						fail("Expected UniqueConstraintViolation, but was: ("+e.getClass().getName()+"): "+e.getMessage());
					}
				}

				waitBeforeCommit.release();
			} catch (Exception e) {
				log.warn("exception for second insert: "+e.getMessage(), e);
			} finally {
				if(mainItx != null) {
					mainItx.complete();
				}
			}

		} finally {
			if (testTimeout.cancel()) {
				fail("test timed out");
			}
		}
	}


	@TxManagerTest
	public void testLockerUnlock(DatabaseTestEnvironment env) throws Exception {
		String lockObjectId = null;

		locker.setTransactionAttribute(TransactionAttribute.REQUIRED);
		locker.setObjectId("myLocker");
		locker.configure();

		lockObjectId = locker.acquire();

		assertNotNull(lockObjectId);
		assertEquals(1, getRowCount(env));

		locker.release(lockObjectId);
		assertEquals(0, getRowCount(env));

		lockObjectId = locker.acquire();

		assertNotNull(lockObjectId);
		assertEquals(1, getRowCount(env));

	}

	public int getRowCount(DatabaseTestEnvironment env) throws Exception {
		try(Connection connection = env.getConnection()) {
			return JdbcTestUtil.executeIntQuery(connection, "SELECT COUNT(*) FROM IBISLOCK");
		}
	}

	private class LockerTester extends ConcurrentManagedTransactionTester {

		private DatabaseTestEnvironment env;
		private Connection conn;

		public LockerTester(DatabaseTestEnvironment testEnv) {
			super(testEnv.getConfiguration().getBean("txManager", PlatformTransactionManager.class));
			env = testEnv;
		}

		@Override
		public void initAction() throws SQLException {
			super.initAction();
			conn = env.getConnection();
		}

		@Override
		public void action() throws SQLException, JdbcException {
			JdbcTestUtil.executeStatement(conn, "INSERT INTO "+LockerTest.TABLENAME+" (OBJECTID) VALUES('myLocker')");
		}

		@Override
		public void finalizeAction() throws SQLException {
			try {
				if (conn!=null) {
					conn.close();
				}
			} finally {
				super.finalizeAction();
			}
		}

	}

}
