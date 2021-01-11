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

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.task.TimeoutGuard;

public class LockerTest extends JdbcTestBase {

	private Locker locker;
	private TransactionAwareDataSourceProxy datasource;
	private DataSourceTransactionManager transactionManager;
	
	@Before
	public void setup() throws JdbcException, SQLException {
		if(!dbmsSupport.isTablePresent(connection, "IBISLOCK")) {
			createDbTable();
		}
	}

	public LockerTest(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked) throws SQLException {
		super(productKey, url, userid, password, testPeekDoesntFindRecordsAlreadyLocked);

		locker = new Locker() {

			@Override
			protected DataSource getDatasource() {
				return datasource;
			}

			@Override
			public Connection getConnection() throws JdbcException {
				try {
					return getDbConnection(); //transactionManager.getDataSource().getConnection();
				} catch (SQLException e) {
					throw new JdbcException(e);
				}
			}

		};
		locker.setFirstDelay(0);
		DriverManagerDataSource dmds = new DriverManagerDataSource(url, userid, password);
		datasource = new TransactionAwareDataSourceProxy(dmds);
		transactionManager = new DataSourceTransactionManager(dmds);
		transactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		
	}

	@Test
	public void testBasicLockNoTransactionManager() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
		
	}

	@Test
	public void testBasicLockNoTransactionManagerSecondFails() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
		
		objectId = locker.lock();
		assertNull("Should not be possible to obtain the lock a second time", objectId);
	}

	@Test
	public void testBasicLockWithTransactionManager() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setTxManager(transactionManager);
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testBasicLockWithTransactionManagerSecondFails() throws Exception {
		cleanupLocks();
		String objectId = null;
		locker.setTxManager(transactionManager);
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());

		objectId = locker.lock();
		assertNull("Should not be possible to obtain the lock a second time", objectId);
	}
	
	@Test
	public void testTakeLockBeforeExecutingInsertInOtherThread() throws Exception {
		cleanupLocks();
		locker.setTxManager(transactionManager);
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
			String objectId = locker.lock();
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
		locker.setTxManager(transactionManager);
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
			String objectId = locker.lock();
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
		locker.setTxManager(transactionManager);
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
			if (transactionManager!=null) {
				TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
				
				mainItx = new IbisTransaction(transactionManager, txdef, "locker ");
			}

			try {
				try {
					try (Connection conn = datasource.getConnection()) {
						
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
								if (locker.getDbmsSupport().isUniqueConstraintViolation(e)) {
									log.debug("Caught expected UniqueConstraintViolation ("+e.getClass().getName()+"): "+e.getMessage());
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
		try(Connection conn = getDbConnection()){

			locker.setTxManager(transactionManager);
			locker.setTransactionAttribute("Required");
			locker.setDbmsSupport(dbmsSupport);
			locker.setObjectId("myLocker");
			locker.configure();
			lockObjectId = locker.lock();

			assertNotNull(lockObjectId);
			assertEquals(1, getRowCount());

			locker.unlock(lockObjectId);
			assertEquals(0, getRowCount());

			lockObjectId = locker.lock();

			assertNotNull(lockObjectId);
			assertEquals(1, getRowCount());
		}

	}

	public Connection getDbConnection() throws SQLException {
		return datasource.getConnection();
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
				IbisTransaction mainItx = null;
				if (transactionManager!=null) {
					TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,20);
					
					mainItx = new IbisTransaction(transactionManager, txdef, "locker ");
				}
				if (beginDone!=null) beginDone.release();

				try {
					try (Connection conn = datasource.getConnection()) {
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
