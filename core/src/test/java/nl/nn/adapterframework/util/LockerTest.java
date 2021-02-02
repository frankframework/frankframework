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

import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.task.TimeoutGuard;

public class LockerTest extends TransactionManagerTestBase {

	private Locker locker;
	
	private boolean tableCreated = false;
	
	@Before
	public void setup() throws JdbcException, SQLException {
		if (!dbmsSupport.isTablePresent(connection, "IBISLOCK")) {
			createDbTable();
			tableCreated = true;
		}
	}

	@After
	public void teardown() throws JdbcException, SQLException {
		if (tableCreated) {
			JdbcUtil.executeStatement(connection, "DROP TABLE IBISLOCK"); // drop the table if it was created, to avoid interference with Liquibase
		}
	}

	public LockerTest(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked) throws SQLException, NamingException {
		super(productKey, url, userid, password, testPeekDoesntFindRecordsAlreadyLocked);

		locker = new Locker();
		locker.setDatasourceName(DEFAULT_DATASOURCE_NAME);
		locker.setDataSourceFactory(dataSourceFactory);
		locker.setFirstDelay(0);
		
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
			LockerTester lockerTester = new LockerTester();

			lockerTester.setBeginDone(otherReady);
			lockerTester.setWaitBeforeInsert(otherContinue);
			lockerTester.setCommitDone(otherFinished);
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
			LockerTester lockerTester = new LockerTester();

			lockerTester.setInsertDone(otherReady);
			lockerTester.setWaitBeforeCommit(otherContinue);
			lockerTester.setCommitDone(otherFinished);
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
		
		TimeoutGuard testTimeout = new TimeoutGuard(10,"Testtimeout");
		try {
			Semaphore otherInsertReady = new Semaphore();
			Semaphore otherContinue = new Semaphore();
			Semaphore otherFinished = new Semaphore();
			LockerTester lockerTester = new LockerTester();

			lockerTester.setInsertDone(otherInsertReady);
			lockerTester.setWaitBeforeCommit(otherContinue);
			lockerTester.setCommitDone(otherFinished);
			lockerTester.start();
			
			otherInsertReady.acquire();
			String objectId = locker.acquire();

			otherContinue.release();
			otherFinished.acquire();
			
			assertNull(objectId);
			assertNull(lockerTester.getCaught());
			
		} finally {
			if (testTimeout.cancel()) {
				fail("test timed out");
			}
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
			LockerTester other = new LockerTester();

			other.setWaitBeforeInsert(waitBeforeInsert);
			other.setInsertDone(insertDone);
			other.setWaitBeforeCommit(waitBeforeCommit);

			other.start();
			
			IbisTransaction mainItx = null;
			if (txManager!=null) {
				TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
				
				mainItx = new IbisTransaction(txManager, txdef, "locker ");
			}

			try {
				try {
					try (Connection conn = txManagedDataSource.getConnection()) {
						
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
					}
				} finally {
					if(mainItx != null) {
						mainItx.commit();
					}
				}
			} catch (Exception e) {
				log.warn("exception for second insert: "+e.getMessage(), e);
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
		locker.setTransactionAttribute("Required");
		locker.setDbmsSupport(dbmsSupport);
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

	public void cleanupLocks() throws JdbcException, SQLException {
		JdbcUtil.executeStatement(connection, "DELETE FROM IBISLOCK");
	}

	public int getRowCount() throws JdbcException, SQLException {
		return JdbcUtil.executeIntQuery(connection, "SELECT COUNT(*) FROM IBISLOCK");
	}

	private void createDbTable() throws JdbcException, SQLException {
		JdbcUtil.executeStatement(connection,
				"CREATE TABLE IBISLOCK(" + 
				"OBJECTID "+dbmsSupport.getTextFieldType()+"(100) NOT NULL PRIMARY KEY, " + 
				"TYPE "+dbmsSupport.getTextFieldType()+"(1) NULL, " + 
				"HOST "+dbmsSupport.getTextFieldType()+"(100) NULL, " + 
				"CREATIONDATE "+dbmsSupport.getTimestampFieldType()+" NULL, " + 
				"EXPIRYDATE "+dbmsSupport.getTimestampFieldType()+" NULL)");
	}
	
	
	private class LockerTester extends Thread {

		private @Setter Semaphore beginDone;
		private @Setter Semaphore waitBeforeInsert;
		private @Setter Semaphore insertDone;
		private @Setter Semaphore waitBeforeCommit;
		private @Setter Semaphore commitDone;
		
		private @Getter Exception caught;
		

		@Override
		public void run() {
			try {
				TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
				IbisTransaction mainItx = IbisTransaction.getTransaction(txManager, txDef, "locker tester");

				if (beginDone!=null) beginDone.release();

				try {
					try (Connection conn = txManagedDataSource.getConnection()) {
						if (waitBeforeInsert!=null) waitBeforeInsert.acquire();
						executeTranslatedQuery(conn, "INSERT INTO IBISLOCK (OBJECTID) VALUES('myLocker')", "INSERT");
						if (insertDone!=null) insertDone.release();
						if (waitBeforeCommit!=null) waitBeforeCommit.acquire();
					}
				} finally {
					if(mainItx != null) {
						mainItx.commit();
					}
				}
			
			} catch (Exception e) {
				log.warn("Exception in lockerTester: "+ e.getMessage());
				caught = e;
			} finally {
				if (commitDone!=null) commitDone.release();
			}
		}

		
	}

}
