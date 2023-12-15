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
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doAnswer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.DbmsException;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.jdbc.dbms.ConcurrentJdbcActionTester;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.junit.DatabaseTest;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;
import nl.nn.adapterframework.testutil.junit.WithLiquibase;
import nl.nn.adapterframework.util.DbmsUtil;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

@WithLiquibase(tableName = JdbcTableListenerTest.TABLE_NAME, file = "Migrator/JdbcTestBaseQuery.xml")
public class JdbcTableListenerTest {

	private JdbcTableListener listener;
	protected static final String TABLE_NAME = "JTL_TABLE";
	private @Getter TestConfiguration configuration;
	protected static Logger log = LogUtil.getLogger(JdbcTableListenerTest.class);

	/*
	 * set testNegativePeekWhileGet=true to test that peek does not see new records when there is a record in process.
	 * This test fails currently for Oracle and MsSqlServer. It can be fixed by adding 'FOR UPDATE SKIP LOCKED' or 'WITH(updlock)' respectively.
	 * Doing that, however, increases the amount of locks on the table. For now, the overhead of peeking some messages that do not exist is considered
	 * less expensive than setting locks on the database to have a more secure peek.
	 */
	private final boolean testNegativePeekWhileGet = false;

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	private TestConfiguration getConfiguration() {
		return transactionManagerType.getConfigurationContext(dataSourceName);
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener = new JdbcTableListener();
		listener.setTableName(TABLE_NAME);
		listener.setDatasourceName(dataSourceName);
		listener.setName("JTL_TABLE");
		listener.setKeyField("TKEY");
		listener.setStatusField("TINT");
		listener.setStatusValueAvailable("1");
		listener.setStatusValueProcessed("2");
		listener.setStatusValueError("3");
		getConfiguration().autowireByName(listener);

		getConfiguration().getIbisManager();
		getConfiguration().autowireByName(listener);
	}

	@AfterEach
	public void teardown(DatabaseTestEnvironment databaseTestEnvironment) throws Throwable {
		if(listener != null) {
			listener.close();
		}
		databaseTestEnvironment.close();
	}

	protected void autowire(JdbcFacade jdbcFacade) {
		configuration.autowireByName(jdbcFacade);
		jdbcFacade.setDatasourceName(dataSourceName);
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

	@DatabaseTest
	public void testSetup() throws ConfigurationException, ListenerException {
		listener.configure();
		listener.open();
	}

	@DatabaseTest
	public void testSelectQuery() throws ConfigurationException {
		listener.setOrderField("ORDRFLD");
		listener.configure();

		String expected = "SELECT TKEY FROM " + TABLE_NAME + " t WHERE TINT='1' ORDER BY ORDRFLD";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryNoAvailable() throws ConfigurationException {
		listener.setStatusValueAvailable(null);
		listener.configure();

		String expected = "SELECT TKEY FROM " + TABLE_NAME + " t WHERE TINT NOT IN ('3','2')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT TKEY FROM " + TABLE_NAME + " t WHERE TINT='1' AND (t.TVARCHAR='x')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithMessageIdAndCorrelationId() throws ConfigurationException {
		listener.setMessageIdField("MIDFIELD");
		listener.setCorrelationIdField("CIDFIELD");
		listener.configure();

		String expected = "SELECT TKEY,MIDFIELD,CIDFIELD FROM " + TABLE_NAME + " t WHERE TINT='1'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testUpdateStatusQuery() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE " + TABLE_NAME + " SET TINT='3' WHERE TINT!='3' AND TKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailable(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM " + TABLE_NAME + " t WHERE (TINT='1')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryError(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM " + TABLE_NAME + " t WHERE (TINT='3')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailableWithSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.VARCHAR='A'");
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryErrorSelectCondition(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM " + TABLE_NAME + " t WHERE (TINT='3' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	public void testGetRawMessage(String status, boolean expectMessage, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10," + status + ")", null, new PipeLineSession());
		RawMessageWrapper<?> rawMessage = listener.getRawMessage(new HashMap<>());
		if (expectMessage) {
			assertEquals("10",rawMessage.getRawMessage());
		} else {
			assertNull(rawMessage);
		}
	}

	@DatabaseTest
	public void testGetRawMessageFindAvailable(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetRawMessage("1", true, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageSkipStatusProcessed(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetRawMessage("2", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageSkipStatusError(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetRawMessage("3", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageSkipOtherStatusvalue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetRawMessage("4", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageSkipNullStatus(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetRawMessage("NULL", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionTrue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("1=1");
		testGetRawMessage("1", true, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionFalse(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("1=0");
		testGetRawMessage("1", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionComplex(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("TKEY=(SELECT r.TKEY FROM " + TABLE_NAME + " r WHERE r.TINT = t.TINT)");
		testGetRawMessage("1", true, databaseTestEnvironment);
	}


	@DatabaseTest
	public void testCreateQueryTexts(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.setMessageField("MSGFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		JdbcTableMessageBrowser browser = getMessageBrowser(ProcessState.AVAILABLE);
		browser.setCorrelationIdField("CIDFLD");
		browser.setIdField("IDFLD");

		browser.createQueryTexts(databaseTestEnvironment.getDbmsSupport());

		assertEquals("DELETE FROM " + TABLE_NAME + " WHERE TKEY=?", browser.deleteQuery);
		assertEquals("SELECT TKEY,IDFLD,CIDFLD FROM " + TABLE_NAME + " WHERE TKEY=?", browser.selectContextQuery);
		assertEquals("SELECT TKEY,MSGFLD FROM " + TABLE_NAME + " WHERE TKEY=?", browser.selectDataQuery);
		assertEquals("SELECT IDFLD FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND IDFLD=?", browser.checkMessageIdQuery);
		assertEquals("SELECT CIDFLD FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND CIDFLD=?", browser.checkCorrelationIdQuery);
		assertEquals("SELECT COUNT(*) FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition))", browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetSelectListQuery(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
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
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? ORDER BY TMFLD DESC", browser.getSelectListQuery(databaseTestEnvironment.getDbmsSupport(), start, null, SortOrder.NONE));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD<? ORDER BY TMFLD ASC", browser.getSelectListQuery(databaseTestEnvironment.getDbmsSupport(), null, end, SortOrder.ASC));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TABLE_NAME + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? AND TMFLD<? ORDER BY TMFLD DESC", browser.getSelectListQuery(databaseTestEnvironment.getDbmsSupport(), start, end, SortOrder.DESC));
	}

	@DatabaseTest
	public void testCreateUpdateStatusQuery(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeThat(databaseTestEnvironment.getDbmsSupport().getDbms(), equalTo(Dbms.H2));
		listener.setMessageField("MSGFLD");
		listener.setTimestampField("TMFLD");
		listener.setCommentField("CMTFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		assertEquals("UPDATE " + TABLE_NAME + " SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=?,fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE " + TABLE_NAME + " SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=? WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	@DatabaseTest
	public void testCreateUpdateStatusQueryLessFields() throws Exception {
		listener.configure();

		assertEquals("UPDATE " + TABLE_NAME + " SET TINT='fakeValue',fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE " + TABLE_NAME + " SET TINT='fakeValue' WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	public void testGetMessageCount(String status, ProcessState state, int expectedCount, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT,TVARCHAR) VALUES (10," + status + ",'A')", null, new PipeLineSession());

		JdbcTableMessageBrowser browser = getMessageBrowser(state);

		assertEquals(expectedCount, browser.getMessageCount());
	}

	@DatabaseTest
	public void testGetMessageCount(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testGetMessageCount("1", ProcessState.AVAILABLE, 1, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithTableAliasSelected(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("1", ProcessState.AVAILABLE, 1, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithTableAliasUnselected(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("1", ProcessState.AVAILABLE, 0, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithOrClauseUnselected(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("TVARCHAR!='A' OR 1=1");
		// a record for state done is inserted, so there should be no record in state available.
		// Missing parentheses would cause the OR to select one
		testGetMessageCount("2", ProcessState.AVAILABLE, 0, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetMessageCountDoneWithWithTableAliasSelected(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("2", ProcessState.DONE, 1, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetMessageCountDoneWithWithTableAliasUnselected(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("2", ProcessState.DONE, 0, databaseTestEnvironment);
	}

	public void testPeekMessage(String status, boolean expectMessage, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10," + status + ")", null, new PipeLineSession());
		boolean actual = listener.hasRawMessageAvailable();
		assertEquals(expectMessage,actual);
	}

	@DatabaseTest
	public void testPeekMessageFindAvailable(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testPeekMessage("1", true, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testPeekMessageSkipStatusProcessed(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testPeekMessage("2", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testPeekMessageSkipStatusError(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testPeekMessage("3", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testPeekMessageSkipOtherStatusvalue(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testPeekMessage("4", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testPeekMessageSkipNullStatus(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testPeekMessage("NULL", false, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testGetIdFromRawMessage(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setMessageIdField("tVARCHAR");
		listener.setCorrelationIdField("tCLOB");
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT,TVARCHAR,TCLOB) VALUES (10,1,'fakeMid','fakeCid')", null, new PipeLineSession());

		RawMessageWrapper<?> rawMessage = listener.getRawMessage(new HashMap<>());

		String mid = rawMessage.getId();
		String cid = rawMessage.getCorrelationId();

		assertEquals("fakeMid", mid);
		assertEquals("fakeCid", cid);
	}


	@DatabaseTest
	public void testParallelGet(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		if (!databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = databaseTestEnvironment.getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (11,1)", null, new PipeLineSession());
			RawMessageWrapper<?> rawMessage2 = listener.getRawMessage(new HashMap<>());
			assertEquals("11",rawMessage2.getRawMessage());

		}
	}

	public void testParallelChangeProcessState(boolean mainThreadFirst, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "DELETE FROM " + TABLE_NAME + " WHERE TKEY=10", null, new PipeLineSession());
		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		ChangeProcessStateTester changeProcessStateTester = new ChangeProcessStateTester(databaseTestEnvironment::getConnection);
		RawMessageWrapper rawMessage1;
		Semaphore waitBeforeUpdate = new Semaphore();
		Semaphore updateDone = new Semaphore();
		Semaphore waitBeforeCommit = new Semaphore();
		Semaphore commitDone = new Semaphore();
		try (Connection conn = databaseTestEnvironment.getConnection()) {
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

	@DatabaseTest
	public void testParallelChangeProcessStateMainThreadFirst(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testParallelChangeProcessState(true, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testParallelChangeProcessStateMainThreadSecond(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testParallelChangeProcessState(false, databaseTestEnvironment);
	}

	private static class ChangeProcessStateTester extends ConcurrentJdbcActionTester {

		private @Getter int numRowsUpdated=-1;
		private String query;

		public ChangeProcessStateTester(ThrowingSupplier<Connection,SQLException> connectionSupplier) {
			super(connectionSupplier);
		}

		@Override
		public void initAction(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, DbmsException {
			String rawQuery = "UPDATE " + TABLE_NAME + " SET TINT=3 WHERE TINT!=3 AND TKEY=10";
			query = databaseTestEnvironment.getDbmsSupport().convertQuery(rawQuery, "Oracle");
			databaseTestEnvironment.getConnection().setAutoCommit(false);
		}

		@Override
		public void action(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
			try (PreparedStatement statement = databaseTestEnvironment.getConnection().prepareStatement(query)) {
				numRowsUpdated = statement.executeUpdate();
			}
		}

		@Override
		public void finalizeAction(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
			databaseTestEnvironment.getConnection().commit();
		}
	}

	@DatabaseTest
	public void testNegativePeekWhileGet(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		assumeTrue(testNegativePeekWhileGet);
		if (!databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = databaseTestEnvironment.getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			assertFalse(listener.hasRawMessageAvailable(), "Should not peek message when there is none");

		}
	}

	@DatabaseTest
	public void testPositivePeekWhileGet(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		if (!databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = databaseTestEnvironment.getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<?> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (11,1)", null, new PipeLineSession());
			assertTrue(listener.hasRawMessageAvailable(), "Should peek message when there is one");
		}
	}

	@DatabaseTest
	public void testRollback(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		if (!databaseTestEnvironment.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.open();
		boolean useStatusInProcess;
		RawMessageWrapper rawMessage;

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		try (Connection connection1 = databaseTestEnvironment.getConnection()) {
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
			listener.changeProcessState(databaseTestEnvironment.getConnection(), rawMessage, ProcessState.AVAILABLE, "test");
		}
		String status = DbmsUtil.executeStringQuery(databaseTestEnvironment.getConnection(), "SELECT TINT FROM " + TABLE_NAME + " WHERE TKEY=10");
		assertEquals("status should be returned to available, to be able to try again", "1", status);
	}

	private boolean getMessageInParallel(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
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
			JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "UPDATE " + TABLE_NAME + " SET TINT=4 WHERE TKEY=10", null, new PipeLineSession());
		} catch (Exception e) {
			if (databaseTestEnvironment.getDbmsSupport().getDbms() == Dbms.MSSQL) {
				log.info("Allow MSSQL to fail concurrent update with an exception (happens in case 3, 4 and 5): " + e.getMessage());
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
	public void testForRaceConditionHandlingOnParallelGet(int checkpoint, DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		listener.setStatusValueInProcess("4");
		listener.configure();
		listener.open();

		boolean useUpdateRow=false;

		boolean primaryRead = false;
		boolean secondaryRead = false;

		JdbcUtil.executeStatement(databaseTestEnvironment.getDbmsSupport(), databaseTestEnvironment.getConnection(), "INSERT INTO " + TABLE_NAME + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());

		try (Connection connection = databaseTestEnvironment.getConnection()) {
			try {
				connection.setAutoCommit(false);

				if (checkpoint == 1) secondaryRead = getMessageInParallel(databaseTestEnvironment);

				String query = databaseTestEnvironment.getDbmsSupport().prepareQueryTextForWorkQueueReading(1, "SELECT TKEY,TINT FROM " + TABLE_NAME + " WHERE TINT='1'");
				log.debug("prepare query [" + query + "]");
				try (PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {

					if (checkpoint == 2) secondaryRead = getMessageInParallel(databaseTestEnvironment);

					try (ResultSet rs = stmt.executeQuery()) {

						if (checkpoint == 3) secondaryRead = getMessageInParallel(databaseTestEnvironment);

						if (rs.next()) {

							if (checkpoint == 4) secondaryRead = getMessageInParallel(databaseTestEnvironment);

							if (useUpdateRow) {
								rs.updateInt(2, 4);
								if (checkpoint == 5) secondaryRead = getMessageInParallel(databaseTestEnvironment);
								rs.updateRow();
							} else {
								int key = rs.getInt(1);
								try (PreparedStatement stmt2 = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET TINT='4' WHERE TKEY=?")) {
									stmt2.setInt(1, key);
									if (checkpoint == 5) secondaryRead = getMessageInParallel(databaseTestEnvironment);
									stmt2.execute();
								}
							}

							if (checkpoint == 6) secondaryRead = getMessageInParallel(databaseTestEnvironment);

							connection.commit();
							primaryRead = true;
							if (checkpoint == 7) secondaryRead = getMessageInParallel(databaseTestEnvironment);
						}
					}
				}
			} finally {
				connection.rollback(); // required for DB2
			}
		}
		assertFalse(primaryRead && secondaryRead, "At most one attempt should have passed");
		assertTrue(primaryRead || secondaryRead, "At least one attempt should have passed");
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet1(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(1, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet2(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(2, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet3(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(3, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet4(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(4, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet5(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(5, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet6(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(6, databaseTestEnvironment);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet7(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		testForRaceConditionHandlingOnParallelGet(7, databaseTestEnvironment);
	}
}
