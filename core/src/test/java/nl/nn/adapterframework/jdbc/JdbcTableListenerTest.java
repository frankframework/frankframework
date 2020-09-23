package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.JdbcUtil;
import oracle.jdbc.pool.OracleDataSource;

public class JdbcTableListenerTest extends JdbcTestBase {

	private JdbcTableListener listener;
	
	/*
	 * set testNegativePeekWhileGet=true to test that peek does not see new records when there is a record in process.
	 * This test fails currently for Oracle and MsSqlServer. It can be fixed by adding 'FOR UPDATE SKIP LOCKED' or 'WITH(updlock)' respectively.
	 * Doing that, however, increases the amount of locks on the table. For now, the overhead of peeking some messages that do not exist is considered
	 * less expensive than setting locks on the database to have a more secure peek.
	 */
	private boolean testNegativePeekWhileGet = false;
	
	public JdbcTableListenerTest(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked) throws SQLException {
		super(productKey, url, userid, password, testPeekDoesntFindRecordsAlreadyLocked);
		listener = new JdbcTableListener() {

			@Override
			public Connection getConnection() throws JdbcException {
				try {
					return getDbConnection();
				} catch (SQLException e) {
					throw new JdbcException(e);
				}
			}

			@Override
			protected DataSource getDatasource() throws JdbcException {
				try {
					return new OracleDataSource(); // just return one, to have one.
				} catch (SQLException e) {
					throw new JdbcException(e);
				} 
			}
			
		};
		listener.setDatasourceName("dummy");
		listener.setTableName("TEMP");
		listener.setKeyField("TKEY");
		listener.setStatusField("TINT");
		listener.setStatusValueAvailable("1");
		listener.setStatusValueProcessed("2");
		listener.setStatusValueError("3");
	}

	public Connection getDbConnection() throws SQLException {
		return getConnection();
	}
	
	@Test
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	public void testGetRawMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,"+status+")", null);
		Object rawMessage = listener.getRawMessage(null);
		if (expectMessage) {
			assertEquals("10",rawMessage);
		} else {
			assertNull(rawMessage);
		}
	}

	@Test
	public void testGetRawMessageFindAvailable() throws Exception {
		testGetRawMessage("1",true);
	}

	@Test
	public void testGetRawMessageSkipStatusProcessed() throws Exception {
		testGetRawMessage("2",false);
	}
	@Test
	public void testGetRawMessageSkipStatusError() throws Exception {
		testGetRawMessage("3",false);
	}
	
	@Test
	public void testGetRawMessageSkipOtherStatusvalue() throws Exception {
		testGetRawMessage("4",false);
	}

	@Test
	public void testGetRawMessageSkipNullStatus() throws Exception {
		testGetRawMessage("NULL",false);
	}

	public void testPeekMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,"+status+")", null);
		boolean actual = listener.hasRawMessageAvailable();
		assertEquals(expectMessage,actual);
	}

	@Test
	public void testPeekMessageFindAvailable() throws Exception {
		testPeekMessage("1",true);
	}

	@Test
	public void testPeekMessageSkipStatusProcessed() throws Exception {
		testPeekMessage("2",false);
	}
	@Test
	public void testPeekMessageSkipStatusError() throws Exception {
		testPeekMessage("3",false);
	}
	
	@Test
	public void testPeekMessageSkipOtherStatusvalue() throws Exception {
		testPeekMessage("4",false);
	}

	@Test
	public void testPeekMessageSkipNullStatus() throws Exception {
		testPeekMessage("NULL",false);
	}

	@Test
	public void testParallelGet() throws Exception {
		if (!dbmsSupport.hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			Object rawMessage1 = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage1);
			if (listener.setMessageStateToInProcess.operate(connection1, rawMessage1, null)) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (11,1)", null);
			Object rawMessage2 = listener.getRawMessage(null);
			assertEquals("11",rawMessage2);
			
		}
	}

	@Test
	public void testNegativePeekWhileGet() throws Exception {
		assumeTrue(testNegativePeekWhileGet);
		if (!dbmsSupport.hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			Object rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1);
			if (listener.setMessageStateToInProcess.operate(connection1, rawMessage1, null)) {
				connection1.commit();
			}

			assertFalse("Should not peek message when there is none", listener.hasRawMessageAvailable());
			
		}
	}
	@Test
	public void testPositivePeekWhileGet() throws Exception {
		if (!dbmsSupport.hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			Object rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1);
			if (listener.setMessageStateToInProcess.operate(connection1, rawMessage1, null)) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (11,1)", null);
			assertTrue("Should peek message when there is one", listener.hasRawMessageAvailable());
			
		}
	}
	
	@Test
	public void testRollback() throws Exception {
		if (!dbmsSupport.hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();
		boolean useStatusInProcess;
		Object rawMessage;
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			rawMessage = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage);
			if (useStatusInProcess=listener.setMessageStateToInProcess.operate(connection1, rawMessage, null)) {
				connection1.commit();
			} else {
				connection1.rollback();
			}
		}

		if (useStatusInProcess) {
			listener.revertInProcessStatusToAvailable.operate(connection, rawMessage, null);
		}
		String status = JdbcUtil.executeStringQuery(connection, "SELECT TINT FROM TEMP WHERE TKEY=10");
		assertEquals("status should be returned to available, to be able to try again", "1", status);
	}

	private boolean getMessageInParallel() throws Exception {
		// execute peek, the result does not matter, but it should not throw an exception;
		listener.hasRawMessageAvailable();
		// execute read, return the result, it should not return an exception
		String key = (String)listener.getRawMessage(null);
		if (key==null) {
			return false;
		}
		assertEquals("10", key);
		JdbcUtil.executeStatement(dbmsSupport,connection, "UPDATE TEMP SET TINT=4 WHERE TKEY=10", null);
		return true;
	}
	
	/*
	 * if two getMessage attempts run in parallel, they should:
	 * - not both get the message
	 * - not throw exceptions
	 * - preferably one of them gets the message
	 */
	public void testForRaceConditionHandlingOnParallelGet(int checkpoint) throws Exception {
		listener.setStatusValueInProcess("4");
		listener.configure();
		listener.open();
		
		boolean useUpdateRow=false;
		
		boolean primaryRead = false;
		boolean secondaryRead = false;
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		
		try (Connection connection = getConnection()) {
			connection.setAutoCommit(false);
			
			if (checkpoint==1) secondaryRead = getMessageInParallel();
			
			String query = dbmsSupport.prepareQueryTextForWorkQueueReading(1, "SELECT TKEY,TINT FROM TEMP WHERE TINT='1'");
			log.debug("prepare query ["+query+"]");
			try (PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {

				if (checkpoint==2) secondaryRead = getMessageInParallel();

				try (ResultSet rs = stmt.executeQuery()) {
					
					if (checkpoint==3) secondaryRead = getMessageInParallel();

					if (rs.next()) {

						if (checkpoint==4) secondaryRead = getMessageInParallel();

						if (useUpdateRow) {
							rs.updateInt(2, 4);
							if (checkpoint==5) secondaryRead = getMessageInParallel();
							rs.updateRow();
						} else {
							int key = rs.getInt(1);
							try (PreparedStatement stmt2 = connection.prepareStatement("UPDATE TEMP SET TINT='4' WHERE TKEY=?")) {
								stmt2.setInt(1, key);
								if (checkpoint==5) secondaryRead = getMessageInParallel();
								stmt2.execute();
							}
						}

						if (checkpoint==6) secondaryRead = getMessageInParallel();

						connection.commit();
						primaryRead = true;
						if (checkpoint==7) secondaryRead = getMessageInParallel();
					}
				}
			}
		}
		assertFalse("At most one attempt should have passed",primaryRead && secondaryRead);
		assertTrue("At least one attempt should have passed",primaryRead || secondaryRead);
	}
	
	@Test
	public void testForRaceConditionHandlingOnParallelGet1() throws Exception {
		testForRaceConditionHandlingOnParallelGet(1);
	}
	
	@Test
	public void testForRaceConditionHandlingOnParallelGet2() throws Exception {
		testForRaceConditionHandlingOnParallelGet(2);
	}
	@Test
	public void testForRaceConditionHandlingOnParallelGet3() throws Exception {
		testForRaceConditionHandlingOnParallelGet(3);
	}
	@Test
	public void testForRaceConditionHandlingOnParallelGet4() throws Exception {
		testForRaceConditionHandlingOnParallelGet(4);
	}
	@Test
	public void testForRaceConditionHandlingOnParallelGet5() throws Exception {
		testForRaceConditionHandlingOnParallelGet(5);
	}
	@Test
	public void testForRaceConditionHandlingOnParallelGet6() throws Exception {
		testForRaceConditionHandlingOnParallelGet(6);
	}

	@Test
	public void testForRaceConditionHandlingOnParallelGet7() throws Exception {
		testForRaceConditionHandlingOnParallelGet(7);
	}
}
