package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.JdbcUtil;
import oracle.jdbc.pool.OracleDataSource;

public class JdbcTableListenerTest extends JdbcTestBase {

	private JdbcTableListener listener;
	
	public JdbcTableListenerTest(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked, boolean testSkipLocked) throws SQLException {
		super(productKey, url, userid, password, testPeekDoesntFindRecordsAlreadyLocked, testSkipLocked);
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
	public void testDoubleGet() throws Exception {
		assumeFalse("H2 does not support multithreaded JdbcListeners", dbmsSupport.getDbmsName().equals("H2"));
		listener.configure();
		listener.open();
		
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (10,1)", null);
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			Object rawMessage1 = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage1);

			JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO TEMP (TKEY,TINT) VALUES (11,1)", null);
			Object rawMessage2 = listener.getRawMessage(null);
			assertEquals("11",rawMessage2);
			
		}
	}

}
