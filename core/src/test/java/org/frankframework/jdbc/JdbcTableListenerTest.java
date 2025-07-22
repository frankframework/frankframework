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
package org.frankframework.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IMessageBrowser.SortOrder;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.DbmsException;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.jdbc.dbms.ConcurrentJdbcActionTester;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.TimeProvider;

@Log4j2
@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = JdbcTableListenerTest.TEST_TABLE)
public class JdbcTableListenerTest {

	static final String TEST_TABLE = "temp";

	private JdbcTableListener<String> listener;
	private DatabaseTestEnvironment env;

	/*
	 * set testNegativePeekWhileGet=true to test that peek does not see new records when there is a record in process.
	 * This test fails currently for Oracle and MsSqlServer. It can be fixed by adding 'FOR UPDATE SKIP LOCKED' or 'WITH(updlock)' respectively.
	 * Doing that, however, increases the amount of locks on the table. For now, the overhead of peeking some messages that do not exist is considered
	 * less expensive than setting locks on the database to have a more secure peek.
	 */
	private final boolean testNegativePeekWhileGet = false;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		Receiver<String> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);

		listener = env.createBean(JdbcTableListener.class);
		listener.setTableName(TEST_TABLE);
		listener.setKeyField("TKEY");
		listener.setStatusField("TINT");
		listener.setStatusValueAvailable("1");
		listener.setStatusValueProcessed("2");
		listener.setStatusValueError("3");
		listener.setReceiver(receiver);
		this.env = env;
	}

	@AfterEach
	public void teardown() {
		if(listener != null) {
			listener.stop();
		}
	}

	private JdbcTableMessageBrowser<String> getMessageBrowser(ProcessState state) throws ConfigurationException {
		JdbcTableMessageBrowser<String> browser = (JdbcTableMessageBrowser<String>)listener.getMessageBrowser(state);
		browser.configure();
		return browser;
	}

	@DatabaseTest
	public void testSetup() throws ConfigurationException {
		listener.configure();
		listener.start();
	}

	@DatabaseTest
	public void testSelectQuery() throws ConfigurationException {
		listener.setOrderField("ORDRFLD");
		listener.configure();

		String expected = "SELECT TKEY FROM " + TEST_TABLE + " t WHERE TINT='1' ORDER BY ORDRFLD";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryNoAvailable() throws ConfigurationException {
		listener.setStatusValueAvailable(null);
		listener.configure();

		String expected = "SELECT TKEY FROM " + TEST_TABLE + " t WHERE TINT NOT IN ('3','2')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testSelectQueryWithSelectCondition() throws ConfigurationException {
		listener.setSelectCondition("t.TVARCHAR='x'");
		listener.configure();

		String expected = "SELECT TKEY FROM " + TEST_TABLE + " t WHERE TINT='1' AND (t.TVARCHAR='x')";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest(cleanupBeforeUse = true)
	public void testSelectConditionWithForbiddenField1() throws ConfigurationException {
		// Arrange
		listener.setSelectCondition("t.T_TIMESTAMP IS NULL");
		listener.setTimestampField("T_TIMESTAMP");

		// Act
		listener.configure();

		// Assert
		ConfigurationWarnings warnings = env.getConfiguration().getConfigurationWarnings();
		assertFalse(warnings.isEmpty());
		assertThat(warnings.getWarnings(), hasItem(containsString("may not reference the timestampField or commentField. Found: [T_TIMESTAMP]")));
	}

	@DatabaseTest(cleanupBeforeUse = true)
	public void testSelectConditionWithForbiddenField2() throws ConfigurationException {
		// Arrange
		listener.setSelectCondition("t.TCMNT2 IS NULL");
		listener.setCommentField("TCMNT2");

		// Act
		listener.configure();

		// Assert
		ConfigurationWarnings warnings = env.getConfiguration().getConfigurationWarnings();
		assertFalse(warnings.isEmpty());
		assertThat(warnings.getWarnings(), hasItem(containsString("may not reference the timestampField or commentField. Found: [TCMNT2]")));
	}

	@DatabaseTest(cleanupBeforeUse = true)
	public void testSelectConditionWithFieldSimilarToForbiddenFields() throws ConfigurationException {
		// Arrange
		listener.setSelectCondition("TCMNT2 IS NULL AND t.T_TIMESTAMP2 IS NULL");
		listener.setCommentField("TCMNT");
		listener.setTimestampField("T_TIMESTAMP");

		// Act
		listener.configure();

		// Assert
		ConfigurationWarnings warnings = env.getConfiguration().getConfigurationWarnings();
		assertThat(warnings.getWarnings(), not(hasItem(containsString("may not reference the timestampField or commentField. Found: [TCMNT]"))));
		assertThat(warnings.getWarnings(), not(hasItem(containsString("may not reference the timestampField or commentField. Found: [T_TIMESTAMP]"))));
	}

	@DatabaseTest
	public void testSelectQueryWithMessageIdAndCorrelationId() throws ConfigurationException {
		listener.setMessageIdField("MIDFIELD");
		listener.setCorrelationIdField("CIDFIELD");
		listener.configure();

		String expected = "SELECT TKEY,MIDFIELD,CIDFIELD FROM " + TEST_TABLE + " t WHERE TINT='1'";

		assertEquals(expected, listener.getSelectQuery());
	}

	@DatabaseTest
	public void testUpdateStatusQuery() throws ConfigurationException {
		listener.configure();

		String expected = "UPDATE " + TEST_TABLE + " SET TINT='3' WHERE TINT!='3' AND TKEY=?";

		assertEquals(expected, listener.getUpdateStatusQuery(ProcessState.ERROR));
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailable() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM " + TEST_TABLE + " t WHERE (TINT='1')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryError() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM " + TEST_TABLE + " t WHERE (TINT='3')";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryAvailableWithSelectCondition() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.AVAILABLE);

		String expected = "SELECT COUNT(*) FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetMessageCountQueryErrorSelectCondition() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.setSelectCondition("t.VARCHAR='A'");
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.ERROR);

		String expected = "SELECT COUNT(*) FROM " + TEST_TABLE + " t WHERE (TINT='3' AND (t.VARCHAR='A'))";

		assertEquals(expected, browser.getMessageCountQuery);
	}

	public void testGetRawMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10," + status + ")", null, new PipeLineSession());
		}

		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());
		if (expectMessage) {
			assertEquals("10",rawMessage.getRawMessage());
		} else {
			assertNull(rawMessage);
		}
	}

	@DatabaseTest
	public void testGetRawMessageFindAvailable() throws Exception {
		testGetRawMessage("1", true);
	}

	@DatabaseTest
	public void testGetRawMessageSkipStatusProcessed() throws Exception {
		testGetRawMessage("2", false);
	}

	@DatabaseTest
	public void testGetRawMessageSkipStatusError() throws Exception {
		testGetRawMessage("3", false);
	}

	@DatabaseTest
	public void testGetRawMessageSkipOtherStatusvalue() throws Exception {
		testGetRawMessage("4", false);
	}

	@DatabaseTest
	public void testGetRawMessageSkipNullStatus() throws Exception {
		testGetRawMessage("NULL", false);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionTrue() throws Exception {
		listener.setSelectCondition("1=1");
		testGetRawMessage("1", true);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionFalse() throws Exception {
		listener.setSelectCondition("1=0");
		testGetRawMessage("1", false);
	}

	@DatabaseTest
	public void testGetRawMessageWithSelectConditionComplex() throws Exception {
		listener.setSelectCondition("TKEY=(SELECT r.TKEY FROM " + TEST_TABLE + " r WHERE r.TINT = t.TINT)");
		testGetRawMessage("1", true);
	}

	@DatabaseTest
	public void testGetRawMessageWithMessageFieldIsClob() throws Exception {
		listener.setMessageField("TCLOB");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.CLOB);
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TCLOB) VALUES (10,1,'TEST')", null, new PipeLineSession());
		}

		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());
		Message message = listener.extractMessage(rawMessage, new PipeLineSession());
		assertEquals("TEST",message.asString());
	}

	@DatabaseTest
	public void testGetRawMessageWithMessageFieldIsBlob() throws Exception {
		listener.setMessageField("TBLOB");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.BLOB);
		listener.setBlobSmartGet(false);
		listener.setBlobsCompressed(false);
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			if (env.getDbmsSupport().getDbms() == Dbms.MSSQL) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBLOB) VALUES (10,1,CONVERT(VARBINARY,'TEST'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.ORACLE) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBLOB) VALUES (10,1,utl_raw.cast_to_raw('TEST'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.DB2) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBLOB) VALUES (10,1,CAST('TEST' AS BLOB))", null, new PipeLineSession());
			} else {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TBLOB) VALUES (10,1,'TEST')", null, new PipeLineSession());
			}
		}

		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());
		Message message = listener.extractMessage(rawMessage, new PipeLineSession());
		assertEquals("TEST",message.asString());
	}

	@DatabaseTest
	public void testGetRawMessageWithMessageFieldIsVarchar() throws Exception {
		listener.setMessageField("TVARCHAR");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.STRING);
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR) VALUES (10,1,'TEST')", null, new PipeLineSession());
		}

		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());
		Message message = listener.extractMessage(rawMessage, new PipeLineSession());
		assertEquals("TEST",message.asString());
	}

	@DatabaseTest
	public void testCreateQueryTexts() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.setMessageField("MSGFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.AVAILABLE);
		browser.setCorrelationIdField("CIDFLD");
		browser.setIdField("IDFLD");

		browser.createQueryTexts(env.getDbmsSupport());

		assertEquals("DELETE FROM " + TEST_TABLE + " WHERE TKEY=?", browser.deleteQuery);
		assertEquals("SELECT TKEY,IDFLD,CIDFLD FROM " + TEST_TABLE + " WHERE TKEY=?", browser.selectContextQuery);
		assertEquals("SELECT TKEY,MSGFLD FROM " + TEST_TABLE + " WHERE TKEY=?", browser.selectDataQuery);
		assertEquals("SELECT IDFLD FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND IDFLD=?", browser.checkMessageIdQuery);
		assertEquals("SELECT CIDFLD FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND CIDFLD=?", browser.checkCorrelationIdQuery);
		assertEquals("SELECT COUNT(*) FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition))", browser.getMessageCountQuery);
	}

	@DatabaseTest
	public void testGetSelectListQuery() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.setMessageField("MSGFLD");
		listener.setTimestampField("TMFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.setCommentField("CMTFLD");
		listener.configure();

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(ProcessState.AVAILABLE);
		browser.setCorrelationIdField("CIDFLD");
		browser.setIdField("IDFLD");

		Date start = TimeProvider.nowAsDate();
		Date end = TimeProvider.nowAsDate();

		//assertEquals("SELECT TKEY,IDFLD,CIDFLD FROM "+TEST_TABLE+" t WHERE (TINT='1' AND (fakeSelectCondition))", browser.getSelectListQuery(dbmsSupport, null, null, null));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? ORDER BY TMFLD DESC", browser.getSelectListQuery(env.getDbmsSupport(), start, null, SortOrder.NONE));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD<? ORDER BY TMFLD ASC", browser.getSelectListQuery(env.getDbmsSupport(), null, end, SortOrder.ASC));
		assertEquals("SELECT TKEY,IDFLD,CIDFLD,TMFLD,CMTFLD FROM " + TEST_TABLE + " t WHERE (TINT='1' AND (fakeSelectCondition)) AND TMFLD>=? AND TMFLD<? ORDER BY TMFLD DESC", browser.getSelectListQuery(env.getDbmsSupport(), start, end, SortOrder.DESC));
	}

	@DatabaseTest
	public void testCreateUpdateStatusQuery() throws Exception {
		assumeTrue(env.getDbmsSupport().getDbms() == Dbms.H2);
		listener.setMessageField("MSGFLD");
		listener.setTimestampField("TMFLD");
		listener.setCommentField("CMTFLD");
		listener.setSelectCondition("fakeSelectCondition");
		listener.configure();

		assertEquals("UPDATE " + TEST_TABLE + " SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=?,fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE " + TEST_TABLE + " SET TINT='fakeValue',TMFLD=NOW(),CMTFLD=? WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	@DatabaseTest
	public void testCreateUpdateStatusQueryLessFields() throws Exception {
		listener.configure();

		assertEquals("UPDATE " + TEST_TABLE + " SET TINT='fakeValue',fakeAdditionalClause WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", "fakeAdditionalClause"));
		assertEquals("UPDATE " + TEST_TABLE + " SET TINT='fakeValue' WHERE TINT!='fakeValue' AND TKEY=?", listener.createUpdateStatusQuery("fakeValue", null));

	}

	public void testGetMessageCount(String status, ProcessState state, int expectedCount) throws Exception {
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR) VALUES (10," + status + ",'A')", null, new PipeLineSession());
		}

		JdbcTableMessageBrowser<String> browser = getMessageBrowser(state);

		assertEquals(expectedCount, browser.getMessageCount());
	}

	@DatabaseTest
	public void testGetMessageCount() throws Exception {
		testGetMessageCount("1", ProcessState.AVAILABLE, 1);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithTableAliasSelected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("1", ProcessState.AVAILABLE, 1);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithTableAliasUnselected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("1", ProcessState.AVAILABLE, 0);
	}

	@DatabaseTest
	public void testGetMessageCountAvailableWithWithOrClauseUnselected() throws Exception {
		listener.setSelectCondition("TVARCHAR!='A' OR 1=1");
		// a record for state done is inserted, so there should be no record in state available.
		// Missing parentheses would cause the OR to select one
		testGetMessageCount("2", ProcessState.AVAILABLE, 0);
	}

	@DatabaseTest
	public void testGetMessageCountDoneWithWithTableAliasSelected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR='A'");
		testGetMessageCount("2", ProcessState.DONE, 1);
	}

	@DatabaseTest
	public void testGetMessageCountDoneWithWithTableAliasUnselected() throws Exception {
		listener.setSelectCondition("t.TVARCHAR!='A'");
		testGetMessageCount("2", ProcessState.DONE, 0);
	}

	public void testPeekMessage(String status, boolean expectMessage) throws Exception {
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10," + status + ")", null, new PipeLineSession());
		}

		boolean actual = listener.hasRawMessageAvailable();
		assertEquals(expectMessage,actual);
	}

	@DatabaseTest
	public void testPeekMessageFindAvailable() throws Exception {
		testPeekMessage("1", true);
	}

	@DatabaseTest
	public void testPeekMessageSkipStatusProcessed() throws Exception {
		testPeekMessage("2", false);
	}

	@DatabaseTest
	public void testPeekMessageSkipStatusError() throws Exception {
		testPeekMessage("3", false);
	}

	@DatabaseTest
	public void testPeekMessageSkipOtherStatusvalue() throws Exception {
		testPeekMessage("4", false);
	}

	@DatabaseTest
	public void testPeekMessageSkipNullStatus() throws Exception {
		testPeekMessage("NULL", false);
	}

	@DatabaseTest
	public void testGetIdFromRawMessage() throws Exception {
		listener.setMessageIdField("tVARCHAR");
		listener.setCorrelationIdField("tCLOB");
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB) VALUES (10,1,'fakeMid','fakeCid')", null, new PipeLineSession());
		}

		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());

		String mid = rawMessage.getId();
		String cid = rawMessage.getCorrelationId();

		assertEquals("fakeMid", mid);
		assertEquals("fakeCid", cid);
	}

	@DatabaseTest
	public void testParallelGet() throws Exception {
		if (!env.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.setMessageField("tVARCHAR");
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR) VALUES (10,1,'Message 1')", null, new PipeLineSession());
		}

		try (Connection connection1 = env.getConnection()) {
			connection1.setAutoCommit(false);
			boolean shouldStillCommitBeforeClose;
			RawMessageWrapper<String> rawMessage1 = listener.getRawMessage(connection1,null);
			assertEquals("Message 1", rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test") != null) {
				connection1.commit();
				shouldStillCommitBeforeClose = false;
			} else {
				shouldStillCommitBeforeClose = true;
			}

			try(Connection connection = env.getConnection()) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR) VALUES (11,1,'Message 2')", null, new PipeLineSession());
			}
			RawMessageWrapper<String> rawMessage2 = listener.getRawMessage(new HashMap<>());

			// Clean connection status before we assert
			if (shouldStillCommitBeforeClose) {
				connection1.rollback(); // Make sure connection has no unfinished connection on it before closing. Required for DB2.
			}
			connection1.setAutoCommit(true);

			assertEquals("Message 2", rawMessage2.getRawMessage());
		}
	}

	public void testParallelChangeProcessState(boolean mainThreadFirst) throws Exception {
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "DELETE FROM " + TEST_TABLE + " WHERE TKEY=10", null, new PipeLineSession());
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		}

		ChangeProcessStateTester changeProcessStateTester = new ChangeProcessStateTester(env::getConnection);
		RawMessageWrapper<String> rawMessage1;
		Semaphore waitBeforeUpdate = new Semaphore(0);
		Semaphore updateDone = new Semaphore(0);
		Semaphore waitBeforeCommit = new Semaphore(0);
		Semaphore commitDone = new Semaphore(0);
		try (Connection conn = env.getConnection()) {
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
	public void testParallelChangeProcessStateMainThreadFirst() throws Exception {
		testParallelChangeProcessState(true);
	}

	// Niels doesn't quite understand these tests... This is not possible, the table does a rowlock, as it should.
	@DatabaseTest
	@Disabled("This test does not work and can never work")
	public void testParallelChangeProcessStateMainThreadSecond() throws Exception {
		testParallelChangeProcessState(false);
	}

	private class ChangeProcessStateTester extends ConcurrentJdbcActionTester {

		private @Getter int numRowsUpdated=-1;
		private String query;

		public ChangeProcessStateTester(ThrowingSupplier<Connection, SQLException> connectionSupplier) {
			super(connectionSupplier);
		}

		@Override
		public void initAction(Connection conn) throws SQLException, DbmsException {
			String rawQuery = "UPDATE " + TEST_TABLE + " SET TINT=3 WHERE TINT!=3 AND TKEY=10";
			query = env.getDbmsSupport().convertQuery(rawQuery, "Oracle");
		}

		@Override
		public void action(Connection conn) throws SQLException {
			try (PreparedStatement statement = conn.prepareStatement(query)) {
				numRowsUpdated = statement.executeUpdate();
			}
		}

	}

	@DatabaseTest
	public void testNegativePeekWhileGet() throws Exception {
		assumeTrue(testNegativePeekWhileGet);
		if (!env.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.start();


		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		}
		try (Connection connection1 = env.getConnection()) {
			connection1.setAutoCommit(false);
			RawMessageWrapper<String> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10",rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test")!=null) {
				connection1.commit();
			}

			assertFalse(listener.hasRawMessageAvailable(), "Should not peek message when there is none");

		}
	}

	@DatabaseTest
	public void testPositivePeekWhileGet() throws Exception {
		if (!env.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		}
		try (Connection connection1 = env.getConnection()) {
			connection1.setAutoCommit(false);
			boolean shouldStillCommitBeforeClose;
			RawMessageWrapper<String> rawMessage1 = listener.getRawMessage(connection1, null);
			assertEquals("10", rawMessage1.getRawMessage());
			if (listener.changeProcessState(connection1, rawMessage1, ProcessState.INPROCESS, "test") != null) {
				connection1.commit();
				shouldStillCommitBeforeClose = false;
			} else {
				shouldStillCommitBeforeClose = true;
			}

			try (Connection connection = env.getConnection()) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (11,1)", null, new PipeLineSession());
			}
			// Clean connection status before we assert
			if (shouldStillCommitBeforeClose) {
				connection1.rollback(); // Make sure connection has no unfinished connection on it before closing. Required for DB2.
			}
			connection1.setAutoCommit(true);

			assertTrue(listener.hasRawMessageAvailable(), "Should peek message when there is one");
		}
	}

	@DatabaseTest
	public void testRollback() throws Exception {
		if (!env.getDbmsSupport().hasSkipLockedFunctionality()) {
			listener.setStatusValueInProcess("4");
		}
		listener.configure();
		listener.start();
		boolean useStatusInProcess;
		RawMessageWrapper<String> rawMessage;

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		}
		try (Connection connection1 = env.getConnection()) {
			connection1.setAutoCommit(false);
			rawMessage = listener.getRawMessage(connection1,null);
			assertEquals("10",rawMessage.getRawMessage());
			useStatusInProcess = listener.changeProcessState(connection1, rawMessage, ProcessState.INPROCESS, "test") != null;
			if (useStatusInProcess) {
				connection1.commit();
			} else {
				connection1.rollback();
			}
		}

		if (useStatusInProcess) {
			try(Connection connection = env.getConnection()) {
				listener.changeProcessState(connection, rawMessage, ProcessState.AVAILABLE, "test");
			}
		}
		try(Connection connection = env.getConnection()) {
			String status = JdbcTestUtil.executeStringQuery(connection, "SELECT TINT FROM " + TEST_TABLE + " WHERE TKEY=10");
			assertEquals("1", status, "status should be returned to available, to be able to try again");
		}
	}

	private boolean getMessageInParallel() throws Exception {
		// execute peek, the result does not matter, but it should not throw an exception;
		listener.hasRawMessageAvailable();
		// execute read, return the result, it should not return an exception
		RawMessageWrapper<String> rawMessage = listener.getRawMessage(new HashMap<>());
		if (rawMessage==null) {
			return false;
		}
		String key = rawMessage.getRawMessage();
		assertEquals("10", key);
		try (Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "UPDATE " + TEST_TABLE + " SET TINT=4 WHERE TKEY=10", null, new PipeLineSession());
		} catch (Exception e) {
			if (env.getDbmsSupport().getDbms() == Dbms.MSSQL) {
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
	public void testForRaceConditionHandlingOnParallelGet(int checkpoint) throws Exception {
		listener.setStatusValueInProcess("4");
		listener.configure();
		listener.start();

		boolean useUpdateRow=false;

		boolean primaryRead = false;
		boolean secondaryRead = false;

		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT) VALUES (10,1)", null, new PipeLineSession());
		}

		try (Connection connection = env.getConnection()) {
			try {
				connection.setAutoCommit(false);

				if (checkpoint == 1) secondaryRead = getMessageInParallel();

				String query = env.getDbmsSupport().prepareQueryTextForWorkQueueReading(1, "SELECT TKEY,TINT FROM " + TEST_TABLE + " WHERE TINT='1'");
				log.debug("prepare query [" + query + "]");
				try (PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {

					if (checkpoint == 2) secondaryRead = getMessageInParallel();

					try (ResultSet rs = stmt.executeQuery()) {

						if (checkpoint == 3) secondaryRead = getMessageInParallel();

						if (rs.next()) {

							if (checkpoint == 4) secondaryRead = getMessageInParallel();

							if (useUpdateRow) {
								rs.updateInt(2, 4);
								if (checkpoint == 5) secondaryRead = getMessageInParallel();
								rs.updateRow();
							} else {
								int key = rs.getInt(1);
								try (PreparedStatement stmt2 = connection.prepareStatement("UPDATE " + TEST_TABLE + " SET TINT='4' WHERE TKEY=?")) {
									stmt2.setInt(1, key);
									if (checkpoint == 5) secondaryRead = getMessageInParallel();
									stmt2.execute();
								}
							}

							if (checkpoint == 6) secondaryRead = getMessageInParallel();

							connection.commit();
							primaryRead = true;
							if (checkpoint == 7) secondaryRead = getMessageInParallel();
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
	public void testForRaceConditionHandlingOnParallelGet1() throws Exception {
		testForRaceConditionHandlingOnParallelGet(1);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet2() throws Exception {
		testForRaceConditionHandlingOnParallelGet(2);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet3() throws Exception {
		testForRaceConditionHandlingOnParallelGet(3);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet4() throws Exception {
		testForRaceConditionHandlingOnParallelGet(4);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet5() throws Exception {
		testForRaceConditionHandlingOnParallelGet(5);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet6() throws Exception {
		testForRaceConditionHandlingOnParallelGet(6);
	}

	@DatabaseTest
	public void testForRaceConditionHandlingOnParallelGet7() throws Exception {
		testForRaceConditionHandlingOnParallelGet(7);
	}

	@DatabaseTest
	public void testSelectQueryWithAdditionalFields() throws ConfigurationException {
		listener.setOrderField("ORDRFLD");
		listener.setMessageIdField("tINT");
		listener.setMessageField("tCLOB");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.CLOB);
		listener.setAdditionalFields(", tBLOB, tVARCHAR,  ");
		listener.configure();

		String expected = "SELECT TKEY,tINT,tCLOB,tBLOB,tVARCHAR FROM " + TEST_TABLE + " t WHERE TINT='1' ORDER BY ORDRFLD";

		assertEquals(expected, listener.getSelectQuery());
		assertEquals(List.of("tBLOB", "tVARCHAR"), listener.getAdditionalFieldsList());
	}

	@DatabaseTest
	public void testGetExtraValues() throws Exception {
		listener.setMessageIdField("tINT");
		listener.setMessageField("tCLOB");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.CLOB);
		listener.setAdditionalFields("tBLOB, tVARCHAR");
		listener.setBlobsCompressed(false);
		listener.setBlobSmartGet(false);
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			if (env.getDbmsSupport().getDbms() == Dbms.MSSQL) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',convert(varbinary, 'fBLOB'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.ORACLE) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',utl_raw.cast_to_raw('fBLOB'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.DB2) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',CAST('fBLOB' AS BLOB))", null, new PipeLineSession());
			} else {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message','fBLOB')", null, new PipeLineSession());
			}
		}

		PipeLineSession session = new PipeLineSession();
		RawMessageWrapper<String> rawMessage = listener.getRawMessage(session);
		Message message = listener.extractMessage(rawMessage, session);

		// Assert
		assertEquals("message", message.asString());
		assertTrue(session.containsKey("tBLOB"), "Session should contain tBLOB");
		assertTrue(session.containsKey("tVARCHAR"), "Session should contain tVARCHAR");
		assertEquals("fVC", session.get("tVARCHAR"));
		assertEquals("fBLOB", session.get("tBLOB"));
	}

	@DatabaseTest
	public void testGetExtraValuesSameAsOtherFields() throws Exception {
		listener.setMessageIdField("tINT");
		listener.setMessageField("tCLOB");
		listener.setMessageFieldType(JdbcListener.MessageFieldType.CLOB);
		listener.setAdditionalFields("tBLOB, tVARCHAR, tCLOB, tINT");
		listener.setBlobsCompressed(false);
		listener.setBlobSmartGet(false);
		listener.configure();
		listener.start();

		try(Connection connection = env.getConnection()) {
			if (env.getDbmsSupport().getDbms() == Dbms.MSSQL) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',convert(varbinary, 'fBLOB'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.ORACLE) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',utl_raw.cast_to_raw('fBLOB'))", null, new PipeLineSession());
			} else if (env.getDbmsSupport().getDbms() == Dbms.DB2) {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message',CAST('fBLOB' AS BLOB))", null, new PipeLineSession());
			} else {
				JdbcTestUtil.executeStatement(env.getDbmsSupport(), connection, "INSERT INTO " + TEST_TABLE + " (TKEY,TINT,TVARCHAR,TCLOB,TBLOB) VALUES (10,1,'fVC','message','fBLOB')", null, new PipeLineSession());
			}
		}
		Map<String, Object> threadContext = listener.openThread();
		RawMessageWrapper<String> rawMessage = listener.getRawMessage(threadContext);

		PipeLineSession session = new PipeLineSession();
		assertTrue(rawMessage.getContext().containsKey(JdbcListener.ADDITIONAL_QUERY_FIELDS_KEY), "RawMessage Context should contain map of additional fields");

		// Extract message, then the additional fields should be copied to the session.
		Message message = listener.extractMessage(rawMessage, session);

		// Assert
		assertEquals("message", message.asString());
		assertTrue(session.containsKey("tINT"), "Session should contain tINT");
		assertTrue(session.containsKey("tBLOB"), "Session should contain tBLOB");
		assertTrue(session.containsKey("tCLOB"), "Session should contain tCLOB");
		assertTrue(session.containsKey("tVARCHAR"), "Session should contain tVARCHAR");
		assertEquals("1", session.get("tINT"));
		assertEquals("fVC", session.get("tVARCHAR"));
		assertEquals("fBLOB", session.get("tBLOB"));
		assertEquals("message", session.get("tCLOB"));
	}
}
