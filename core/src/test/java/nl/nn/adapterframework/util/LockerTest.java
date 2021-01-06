package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTestBase;

public class LockerTest extends JdbcTestBase {

	private Locker locker;
	private TransactionAwareDataSourceProxy datasource;
	private DataSourceTransactionManager transactionManager;

	@Before
	public void setup() throws JdbcException, SQLException {
		if(!dbmsSupport.isTablePresent(getConnection(), "IBISLOCK")) {
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
			public Connection getConnection() {
				try {
					return getDbConnection(); //transactionManager.getDataSource().getConnection();
				} catch (SQLException e) {
					return null;
				}
			}

		};
		DriverManagerDataSource dmds = new DriverManagerDataSource(url, userid, password);
		datasource = new TransactionAwareDataSourceProxy(dmds);
		transactionManager = new DataSourceTransactionManager(datasource);
		transactionManager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		locker.setFirstDelay(1);

	}

	@Test
	public void testBasicLockWithoutTransaction() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		String objectId = null;
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testBasicLockWithTransaction() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		String objectId = null;
		locker.setTxManager(transactionManager);
		locker.setTransactionAttribute("RequiresNew");
		locker.setObjectId("myLocker");
		locker.configure();
		objectId = locker.lock();

		assertNotNull(objectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testTransactionInsertWithoutCommit() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		IbisTransaction mainItx = null;
		if (transactionManager!=null) {
			TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED,20);
			
			mainItx = new IbisTransaction(transactionManager, txdef, "locker ");
		}
		String lockObjectId = null; 
		try {
			String timestamp = productKey.equals("Oracle") ? "TO_TIMESTAMP('2020-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2020-12-11 13:24:54'";
			String expiry = productKey.equals("Oracle") ? "TO_TIMESTAMP('2030-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2030-12-11 13:24:54'";

			try(Connection conn = getDbConnection()) {
				executeTranslatedQuery(conn, "INSERT INTO IBISLOCK VALUES('myLocker','T', 'dummy', "+timestamp+", "+expiry+")", "INSERT");
				assertEquals(1, getRowCount());
			}

			locker.setTxManager(transactionManager);
			locker.setTransactionAttribute("RequiresNew");
			locker.setDbmsSupport(dbmsSupport);
			locker.setObjectId("myLocker");
			locker.setTransactionTimeout(10);
			locker.configure();
			lockObjectId = locker.lock();

		} finally {
			if(mainItx != null) {
				mainItx.commit();
			}
		}
		assertNull(lockObjectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testLockerCannotGetTheLock() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		IbisTransaction mainItx = null;
		if (transactionManager!=null) {
			TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED,20);
			mainItx = new IbisTransaction(transactionManager, txdef, "locker ");
		}
		String lockObjectId = null; 

		String timestamp = productKey.equals("Oracle") ? "TO_TIMESTAMP('2020-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2020-12-11 13:24:54'";
		String expiry = productKey.equals("Oracle") ? "TO_TIMESTAMP('2030-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2030-12-11 13:24:54'";

		try(Connection conn = getDbConnection()) {

			executeTranslatedQuery(conn, "INSERT INTO IBISLOCK VALUES('myLocker','T', 'dummy', "+timestamp+", "+expiry+")", "INSERT");
			assertEquals(1, getRowCount());
		
			locker.setName("myLockerInstance");
			locker.setTxManager(transactionManager);
			locker.setTransactionAttribute("RequiresNew");
			locker.setDbmsSupport(dbmsSupport);
			locker.setObjectId("myLocker");
			locker.setTransactionTimeout(10);
			locker.configure();
			lockObjectId = locker.lock();
		}
		finally {
			if(mainItx != null) {
				mainItx.commit();
			}
		}
		assertNull(lockObjectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testLockTakenByLocker() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		IbisTransaction mainItx = null;

		String timestamp = productKey.equals("Oracle") ? "TO_TIMESTAMP('2020-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2020-12-11 13:24:54'";
		String expiry = productKey.equals("Oracle") ? "TO_TIMESTAMP('2030-12-11 13:24:54','yyyy/mm/dd hh24:mi:ss')" : "'2030-12-11 13:24:54'";
		String lockObjectId = null;
		try(Connection conn = getDbConnection()) {

			locker.setTxManager(transactionManager);
			locker.setTransactionAttribute("Required");
			locker.setDbmsSupport(dbmsSupport);
			locker.setObjectId("myLocker");
			locker.setTransactionTimeout(10);
			locker.configure();
			lockObjectId = locker.lock();

			boolean isUniqueConstraintViolation = false;
			try {
				if (transactionManager!=null) {
					TransactionDefinition txdef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED,20);
					mainItx = new IbisTransaction(transactionManager, txdef, "locker ");
				}
				PreparedStatement stmt = conn.prepareStatement("INSERT INTO IBISLOCK VALUES('myLocker','T', 'dummy', "+timestamp+", "+expiry+")");
				stmt.execute();
			} catch(Exception e) {
				if(mainItx != null) {
					mainItx.getStatus().setRollbackOnly();
				}
				if(e instanceof SQLException)
					isUniqueConstraintViolation = dbmsSupport.isUniqueConstraintViolation((SQLException) e);

				if(!isUniqueConstraintViolation) {
					throw e;
				}
			}
		} finally {
			if(mainItx != null) {
				mainItx.commit();
			}
		}

		assertNotNull(lockObjectId);
		assertEquals(1, getRowCount());
	}

	@Test
	public void testLockerUnlock() throws JdbcException, SQLException, InterruptedException, ConfigurationException {
		cleanupLocks();
		String lockObjectId = null; 
		try(Connection conn = getDbConnection()){

			locker.setTxManager(transactionManager);
			locker.setTransactionAttribute("Required");
			locker.setDbmsSupport(dbmsSupport);
			locker.setObjectId("myLocker");
			locker.setTransactionTimeout(10);
			locker.configure();
			lockObjectId = locker.lock();

			assertNotNull(lockObjectId);
			assertEquals(1, getRowCount());

			locker.unlock(lockObjectId);

		}
		assertEquals(0, getRowCount());

	}

	public Connection getDbConnection() throws SQLException {
		return datasource.getConnection();
	}
	public void cleanupLocks() throws JdbcException, SQLException {
		JdbcUtil.executeStatement(getDbConnection(), "DELETE FROM IBISLOCK");
	}

	public int getRowCount() throws JdbcException, SQLException {
		return JdbcUtil.executeIntQuery(getDbConnection(), "SELECT COUNT(*) FROM IBISLOCK");
	}

	private void createDbTable() throws JdbcException, SQLException {
		JdbcUtil.executeStatement(getConnection(),
				"CREATE TABLE IBISLOCK(" + 
				"OBJECTID "+dbmsSupport.getTextFieldType()+"(100) NOT NULL PRIMARY KEY, " + 
				"TYPE "+dbmsSupport.getTextFieldType()+"(1) NULL, " + 
				"HOST "+dbmsSupport.getTextFieldType()+"(100) NULL, " + 
				"CREATIONDATE "+dbmsSupport.getTimestampFieldType()+" NULL, " + 
				"EXPIRYDATE "+dbmsSupport.getTimestampFieldType()+" NULL)");
	}
}
