/*
   Copyright 2020-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doAnswer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import lombok.Getter;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.jdbc.dbms.ConcurrentJdbcActionTester;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Semaphore;

public class JdbcTableListenerTest extends JdbcTestBase {

	private JdbcTableListener listener;

	/*
	 * set testNegativePeekWhileGet=true to test that peek does not see new records when there is a record in process.
	 * This test fails currently for Oracle and MsSqlServer. It can be fixed by adding 'FOR UPDATE SKIP LOCKED' or 'WITH(updlock)' respectively.
	 * Doing that, however, increases the amount of locks on the table. For now, the overhead of peeking some messages that do not exist is considered
	 * less expensive than setting locks on the database to have a more secure peek.
	 */
	private boolean testNegativePeekWhileGet = false;

	@Before
	@Override
	public void setup() throws Exception {
		super.setup();

		listener = new JdbcTableListener();
		autowire(listener);
		listener.setTableName(TEST_TABLE);
		listener.setKeyField("TKEY");
		listener.setStatusField("TINT");
		listener.setStatusValueAvailable("1");
		listener.setStatusValueProcessed("2");
		listener.setStatusValueError("3");
	}

	@After
	@Override
	public void teardown() throws Exception {
		if(listener != null) {
			listener.close();
		}
		super.teardown();
	}

	public JdbcTableMessageBrowser getMessageBrowser(ProcessState state) throws JdbcException, ConfigurationException {
		JdbcTableMessageBrowser browser = Mockito.spy((JdbcTableMessageBrowser)listener.getMessageBrowser(state));
		doAnswer(arg -> {
			autowire(browser);
			return null;
		}).when(browser).copyFacadeSettings(listener);
		browser.configure();
		return browser;
	}

	@Test
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	@Test
	public void testSelectQuery() throws ConfigurationException {
		listener.setOrderField("ORDRFLD");
		listener.configure();

		String expected = "SELECT TKEY FROM "+TEST_TABLE+" t WHERE TINT='1' ORDER BY ORDRFLD";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryNoAvailable() throws ConfigurationException {
		listener.setStatusValueAvailable(null);
		listener.configure();

		String expected = "SELECT TKEY FROM "+TEST_TABLE+" t WHERE TINT NOT IN ('3','2')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT TKEY FROM "+TEST_TABLE+" t WHERE TINT='1' AND (t.TVARCHAR='x')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testSelectConditionWithForbiddenField1() throws ConfigurationException {
		// Arrange
		listener.setSelectCondition("t.T_TIMESTAMP IS NULL");
		listener.setTimestampField("T_TIMESTAMP");

		// Act
		listener.configure();

		// Assert
		ConfigurationWarnings warnings = getConfiguration().getConfigurationWarnings();
		assertFalse(warnings.isEmpty());
		assertThat(warnings.getWarnings(), hasItem(containsString("may not reference the timestampField or commentField. Found: [T_TIMESTAMP]")));
	}

	@Test
	public void testSelectConditionWithForbiddenField2() throws ConfigurationException {
		// Arrange
		listener.setSelectCondition("t.TCMNT2 IS NULL");
		listener.setCommentField("TCMNT2");

		// Act
		listener.configure();

		// Assert
		ConfigurationWarnings warnings = getConfiguration().getConfigurationWarnings();
		assertFalse(warnings.isEmpty());
		assertThat(warnings.getWarnings(), hasItem(containsString("may not reference the timestampField or commentField. Found: [TCMNT2]")));
	}

	@Test
	public void testSelectQueryWithMessageIdAndCorrelationId() throws ConfigurationException {
		listener.setMessageIdField("MIDFIELD");
		listener.setCorrelationIdField("CIDFIELD");
		listener.configure();

		String expected = "SELECT TKEY,MIDFIELD,CIDFIELD FROM "+TEST_TABLE+" t WHERE TINT='1'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@Test
	public void testUpdateStatusQuery() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE "+TEST_TABLE+" SET TINT='3' WHERE TINT!='3' AND TKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@Test
	public void testGetMessageCountQueryAvailable() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE+" t WHERE (TINT='1')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryError() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE+" t WHERE (TINT='3')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryAvailableWithSelectCondition() throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@Test
	public void testGetMessageCountQueryErrorSelectCondition() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM "+TEST_TABLE+" t WHERE (TINT='3' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	public void testGetRawMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,"+status+")", null, new PipeLineSession());
		RawMessageWrapper<?> rawMessage = listener.getRawMessage(new HashMap<>());
		if (expectMessage) {
			assertEquals("10",rawMessage.getRawMessage());
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

	@Test
	public void testGetRawMessageWithSelectConditionTrue() throws Exception {
		listener.setSelectCondition("1=1");
		testGetRawMessage("1",true);
	}

	@Test
	public void testGetRawMessageWithSelectConditionFalse() throws Exception {
		listener.setSelectCondition("1=0");
		testGetRawMessage("1",false);
	}

	@Test
	public void testGetRawMessageWithSelectConditionComplex() throws Exception {
		listener.setSelectCondition("TKEY=(SELECT r.TKEY FROM "+TEST_TABLE+" r WHERE r.TINT = t.TINT)");
		testGetRawMessage("1",true);
	}


	@Test
	public void testCreateQueryTexts() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setMessageField("MSGFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
		browser.setCorrelationIdField("CIDFLD");
		browser.setIdField("IDFLD");

		browser.createQueryTexts(dbmsSupport);

		assertEquals("DELETE FROM "+TEST_TABLE+" WHERE TKEY=?", browser.deleteQuery);
		assertEquals("SELECT TKEY,IDFLD,CIDFLD FROM "+TEST_TABLE+" WHERE TKEY=?", browser.selectContextQuery);
		assertEquals("SELECT TKEY,MSGFLD,IDFLD,CIDFLD FROM "+TEST_TABLE+" WHERE TKEY=?", browser.selectDataQuery);
		assertEquals("SELECT IDFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition)) AND IDFLD=?", browser.checkMessageIdQuery);
		assertEquals("SELECT CIDFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition)) AND CIDFLD=?", browser.checkCorrelationIdQuery);
		assertEquals("SELECT COUNT(*) FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition))", browser.getMessageCountQuery);
	}

	@Test
	public void testGetSelectListQuery() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setMessageField("MSGFLD");
		listener.setTimestampField("TMFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.setCommentField("CMTFLD");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
		browser.setCorrelationIdField("CIDFLD");
		browser.setIdField("IDFLD");

		Date start = new Date();
		Date end = new Date();

		//assertEquals("SELECT TKEY,IDFLD,CIDFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition))", browser.getSelectListQuery(dbmsSupport, null, null, null));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? ORDER BY TMFLD DESC", browser.getSelectListQuery(dbmsSupport, start , null, SortOrder.NONE));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD<? ORDER BY TMFLD ASC", browser.getSelectListQuery(dbmsSupport, null , end, SortOrder.ASC));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? AND TMFLD<? ORDER BY TMFLD DESC", browser.getSelectListQuery(dbmsSupport, start , end, SortOrder.DESC));
	}

	@Test
	public void testCreateUpdateStatusQuery() throws Exception {
		assumeThat(dbmsSupport.getDbms(),equalTo(Dbms.H2));
		listener.setMessageField("MSGFLD");
		listener.setTimestampField("TMFLD");
		listener.setCommentField("CMTFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		assertEquals("UPDATE "+TEST_TABLE+" SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=?,fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE "+TEST_TABLE+" SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=? WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	@Test
	public void testCreateUpdateStatusQueryLessFields() throws Exception {
		listener.configure();

		assertEquals("UPDATE "+TEST_TABLE+" SET TINT='fakeValue',fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE "+TEST_TABLE+" SET TINT='fakeValue' WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	public void testGetMessageCount(String status, ProcessState state, int expectedCount) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT,TVARCHAR) VALUES (10,"+status+",'A')", null, new PipeLineSession());

		JdbcTableMessageBrowser browser = getMessageBrowser(state);

		assertEquals(expectedCount, browser.getMessageCount());
	}

	@Test
	public void testGetMessageCount() throws Exception {
		testGetMessageCount("1",ProcessState.AVAILABLE, 1);
	}

	@Test
	public void testGetMessageCountAvailableWithWithTableAliasSelected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("1",ProcessState.AVAILABLE, 1);
	}

	@Test
	public void testGetMessageCountAvailableWithWithTableAliasUnselected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("1",ProcessState.AVAILABLE, 0);
	}

	@Test
	public void testGetMessageCountAvailableWithWithOrClauseUnselected() throws Exception {
		listener.setSelectCondition("TVARCHAR!='A' OR 1=1");
		// a record for state done is inserted, so there should be no record in state available.
		// Missing parentheses would cause the OR to select one
		testGetMessageCount("2",ProcessState.AVAILABLE, 0);
	}

	@Test
	public void testGetMessageCountDoneWithWithTableAliasSelected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("2",ProcessState.DONE, 1);
	}

	@Test
	public void testGetMessageCountDoneWithWithTableAliasUnselected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("2",ProcessState.DONE, 0);
	}

	public void testPeekMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,"+status+")", null, new PipeLineSession());
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
	public void testGetIdFromRawMessage() throws Exception {
		listener.setMessageIdField("tVARCHAR");
		listener.setCorrelationIdField("tCLOB");
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport, connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB) VALUES (10,1,'fakeMid','fakeCid')", null, new PipeLineSession());

		RawMessageWrapper<?> rawMessage = listener.getRawMessage(new HashMap<>());

		String mid = rawMessage.getId();
		String cid = rawMessage.getCorrelationId();

		assertEquals("fakeMid", mid);
		assertEquals("fakeCid", cid);
	}


	@Test
	public void testParallelGet() throws Exception {
		if (!dbmsSupport.hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (11,1)", null, new PipeLineSession());
			RawMessageWrapper<?> rawMessage2 = listener.getRawMessage(new HashMap<>());
			assertEquals("11",rawMessage2.getRawMessage());

		}
	}

	public void testParallelChangeProcessState(boolean mainThreadFirst) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(dbmsSupport,connection, "DELETE FROM "+TEST_TABLE+" WHERE TKEY=10", null, new PipeLineSession());
		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		ChangeProcessStateTester changeProcessStateTester = new ChangeProcessStateTester(() -> getConnection());
		RawMessageWrapper rawMessage1;
		Semaphore waitBeforeUpdate = new Semaphore();
		Semaphore updateDone = new Semaphore();
		Semaphore waitBeforeCommit = new Semaphore();
		Semaphore commitDone = new Semaphore();
		try (Connection conn = getConnection()) {
			conn.setAutoCommit(false);
			try {
				changeProcessStateTester.setWaitBeforeAction(waitBeforeUpdate);
				changeProcessStateTester.setActionDone(updateDone);
				changeProcessStateTester.setWaitAfterAction(waitBeforeCommit);
				changeProcessStateTester.setFinalizeActionDone(commitDone);
				changeProcessStateTester.start();
				if (!mainThreadFirst) {
					waitBeforeUpdate.release();
					updateDone.acquire();
				}
				String key = "10";
				RawMessageWrapper<String> rawMessage = new RawMessageWrapper<>(key, key, key);
				rawMessage.getContext().put(PipeLineSession.STORAGE_ID_KEY, key);
				rawMessage1 = listener.changeProcessState(conn, rawMessage, ProcessState.ERROR, "test");
				if (mainThreadFirst) {
					waitBeforeUpdate.release();
				} else {
					waitBeforeCommit.release();
					commitDone.acquire();
					commitDone.release();
				}
				assertEquals(mainThreadFirst, rawMessage1!=null);
			} finally {
				waitBeforeCommit.release();
				conn.commit();
			}
		}
		commitDone.acquire();
		assertTrue(changeProcessStateTester.numRowsUpdated>=0);
		assertTrue(changeProcessStateTester.numRowsUpdated<=1);
		assertEquals(mainThreadFirst, changeProcessStateTester.numRowsUpdated==0);
	}

	@Test
	public void testParallelChangeProcessStateMainThreadFirst() throws Exception {
		testParallelChangeProcessState(true);
	}

	@Test
	public void testParallelChangeProcessStateMainThreadSecond() throws Exception {
		testParallelChangeProcessState(false);
	}

	private class ChangeProcessStateTester extends ConcurrentJdbcActionTester {

		private @Getter int numRowsUpdated=-1;
		private String query;

		public ChangeProcessStateTester(ThrowingSupplier<Connection,SQLException> connectionSupplier) {
			super(connectionSupplier);
		}

		@Override
		public void initAction(Connection conn) throws Exception {
			String rawQuery = "UPDATE " + TEST_TABLE + " SET TINT=3 WHERE TINT!=3 AND TKEY=10";
			query = dbmsSupport.convertQuery(rawQuery, "Oracle");
			connection.setAutoCommit(false);
		}

		@Override
		public void action(Connection conn) throws Exception {
			try (PreparedStatement statement = conn.prepareStatement(query)) {
				numRowsUpdated = statement.executeUpdate();
			}
		}

		@Override
		public void finalizeAction(Connection conn) throws Exception {
			connection.commit();
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

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
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

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (11,1)", null, new PipeLineSession());
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
		RawMessageWrapper rawMessage;

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = getConnection()) {
			connection1.setAutoCommit(false);
			rawMessage = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage.getRawMessage());
			if (useStatusInProcess=listener.changeProcessState(connection1, rawMessage, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			} else {
				connection1.rollback();
			}
		}

		if (useStatusInProcess) {
			listener.changeProcessState(connection, rawMessage, ProcessState.AVAILABLE, "test");
		}
		String status = JdbcUtil.executeStringQuery(connection, "SELECT TINT FROM "+TEST_TABLE+" WHERE TKEY=10");
		assertEquals("status should be returned to available, to be able to try again", "1", status);
	}

	private boolean getMessageInParallel() throws Exception {
		// execute peek, the result does not matter, but it should not throw an exception;
		listener.hasRawMessageAvailable();
		// execute read, return the result, it should not return an exception
		RawMessageWrapper<?> rawMessage = listener.getRawMessage(new HashMap<>());
		if (rawMessage==null) {
			return false;
		}
		String key = (String) rawMessage.getRawMessage();
		assertEquals("10", key);
		try {
			JdbcUtil.executeStatement(dbmsSupport,connection, "UPDATE "+TEST_TABLE+" SET TINT=4 WHERE TKEY=10", null, new PipeLineSession());
		} catch (Exception e) {
			if (dbmsSupport.getDbms()==Dbms.MSSQL) {
				log.info("Allow MSSQL to fail concurrent update with an exception (happens in case 3, 4 and 5): "+e.getMessage());
				return false;
			}
			fail("Got the message, but cannot update the row: "+e.getMessage());
		}
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

		JdbcUtil.executeStatement(dbmsSupport,connection, "INSERT INTO "+TEST_TABLE+" (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());

		try (Connection connection = getConnection()) {
			try {
				connection.setAutoCommit(false);

				if (checkpoint==1) secondaryRead = getMessageInParallel();

				String query = dbmsSupport.prepareQueryTextForWorkQueueReading(1, "SELECT TKEY,TINT FROM "+TEST_TABLE+" WHERE TINT='1'");
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
								try (PreparedStatement stmt2 = connection.prepareStatement("UPDATE "+TEST_TABLE+" SET TINT='4' WHERE TKEY=?")) {
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
			} finally {
				connection.rollback(); // required for DB2
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
