package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.TransactionAttribute;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.jdbc.dbms.ConcurrentManagedTransactionTester;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.task.TimeoutGuard;

public class LockerTest extends TransactionManagerTestBase {

	private Locker locker;
	private boolean tableCreated = false;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		createDbTableIfNotExists(); //cannot run migrator as the ibislock table name is not configurable

		locker = new Locker();
		autowire(locker);
		locker.setFirstDelay(0);
	}

	private void createDbTableIfNotExists() throws Exception {
		if (!isTablePresent("IBISLOCK")) {
			JdbcUtil.executeStatement(connection,
				"CREATE TABLE IBISLOCK(" + 
				"OBJECTID "+dbmsSupport.getTextFieldType()+"(100) NOT NULL PRIMARY KEY, " + 
				"TYPE "+dbmsSupport.getTextFieldType()+"(1) NULL, " + 
				"HOST "+dbmsSupport.getTextFieldType()+"(100) NULL, " + 
				"CREATIONDATE "+dbmsSupport.getTimestampFieldType()+" NULL, " + 
				"EXPIRYDATE "+dbmsSupport.getTimestampFieldType()+" NULL)");
			tableCreated = true;
		}
	}

	@After
	@Override
	public void teardown() throws Exception {
		if (tableCreated) {
			dropTable("IBISLOCK");// drop the table if it was created, to avoid interference with Liquibase
		}
		super.teardown();
	}

	@Test
	public void testBasicLockNoTransactionManager() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testBasicLockNoTransactionManagerSecondFails() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());

		objectId = locker.acquire();
		assertNull("Should not be possible to obtain the lock a second time", objectId);
	}

	@Test
	public void testBasicLockWithTransactionManager() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testBasicLockWithTransactionManagerSecondFails() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.acquire();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());

		objectId = locker.acquire();
		assertNull("Should not be possible to obtain the lock a second time", objectId);
	}


	@Test
	public void testTakeLockBeforeExecutingInsertInOtherThread() throws Exception {
		cleanupLocks();
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.configure();
		
		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore otherReady = new Semaphore();
			Semaphore otherContinue = new Semaphore();
			Semaphore otherFinished = new Semaphore();
			LockerTester lockerTester = new LockerTester(txManager);

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
	
	@Test
	public void testTakeLockFailsAfterExecutingInsertInOtherThread() throws Exception {
		cleanupLocks();
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.configure();
		
		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore otherReady = new Semaphore();
			Semaphore otherContinue = new Semaphore();
			Semaphore otherFinished = new Semaphore();
			LockerTester lockerTester = new LockerTester(txManager);

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

	@Test
	public void testLockWaitTimeout() throws Exception {
		cleanupLocks();
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.setLockWaitTimeout(1);
		locker.configure();
		
		boolean lockerUnderTestReturned = false;
		
		log.debug("Creating Timeout Guard");
		TimeoutGuard testTimeout = new TimeoutGuard(20,"Testtimeout");
		try {
			Semaphore otherInsertReady = new Semaphore();
			Semaphore otherContinue = new Semaphore();
			Semaphore otherFinished = new Semaphore();
			log.debug("Preparing LockerTester");
			LockerTester lockerTester = new LockerTester(txManager);

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
	@Test
	public void testNoInsertAfterInsert() throws Exception {
		cleanupLocks();
		locker.setTxManager(txManager);
		locker.setObjectId("myLocker");
		locker.configure();

		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore waitBeforeInsert = new Semaphore();
			Semaphore insertDone = new Semaphore();
			Semaphore waitBeforeCommit = new Semaphore();
			LockerTester other = new LockerTester(txManager);

			other.setWaitBeforeAction(waitBeforeInsert);
			other.setActionDone(insertDone);
			other.setWaitAfterAction(waitBeforeCommit);

			other.start();

			IbisTransaction mainItx = null;
			if (txManager!=null) {
				TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);

				mainItx = new IbisTransaction(txManager, txdef, "locker ");
			}

			try (Connection conn = getConnection()) {
				waitBeforeInsert.release(); // now this thread has started its transaction, let the other thread do its insert
				insertDone.acquire();		// and wait that to be finished

				try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO IBISLOCK (OBJECTID) VALUES('myLocker')")) {

					try {
						Timer timer = new Timer("let other thread commit after one second");
						timer.schedule(new TimerTask() {
											@Override
											public void run() {
												waitBeforeCommit.release();
											}
										}, 1000L);
						stmt.executeUpdate();
						log.debug("lock inserted");
						fail("should not be possible to do a second insert");
					} catch (SQLException e) {
						if (locker.getDbmsSupport().isConstraintViolation(e) || e.getMessage().toLowerCase().contains("timeout")) {
							log.debug("Caught expected UniqueConstraintViolation or Timeout ("+e.getClass().getName()+"): "+e.getMessage());
						} else {
							fail("Expected UniqueConstraintViolation, but was: ("+e.getClass().getName()+"): "+e.getMessage());
						}
					}
				}

				waitBeforeCommit.release();
			} catch (Exception e) {
				log.warn("exception for second insert: "+e.getMessage(), e);
			} finally {
				if(mainItx != null) {
					mainItx.commit();
				}
			}

		} finally {
			if (testTimeout.cancel()) {
				fail("test timed out");
			}
		}
	}


	@Test
	public void testLockerUnlock() throws Exception {
		cleanupLocks();
		String lockObjectId = null; 

		locker.setTxManager(txManager);
		locker.setTransactionAttribute(TransactionAttribute.REQUIRED);
		locker.setObjectId("myLocker");
		locker.configure();

		lockObjectId = locker.acquire();

		assertNotNull(lockObjectId);
		assertEquals(1, getRowCount());

		locker.release(lockObjectId);
		assertEquals(0, getRowCount());

		lockObjectId = locker.acquire();

		assertNotNull(lockObjectId);
		assertEquals(1, getRowCount());

	}

	public void cleanupLocks() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM IBISLOCK");
	}

	public int getRowCount() throws Exception {
		return JdbcUtil.executeIntQuery(connection, "SELECT COUNT(*) FROM IBISLOCK");
	}

	private class LockerTester extends ConcurrentManagedTransactionTester {

		private Connection conn;

		public LockerTester(PlatformTransactionManager txManager) {
			super(txManager);
		}

		@Override
		public void initAction() throws Exception {
			super.initAction();
			conn = getConnection();
		}
		
		@Override
		public void action() throws Exception {
			executeTranslatedQuery(conn, "INSERT INTO IBISLOCK (OBJECTID) VALUES('myLocker')", QueryType.OTHER);
		}

		@Override
		public void finalizeAction() throws Exception {
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
