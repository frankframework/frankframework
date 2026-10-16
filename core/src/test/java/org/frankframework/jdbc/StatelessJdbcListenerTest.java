package org.frankframework.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.CloseUtils;

@WithLiquibase(tableName = StatelessJdbcListenerTest.TABLE_NAME)
class StatelessJdbcListenerTest {
	static final String TABLE_NAME = "MESSAGES";

	private StatelessJdbcListener listener;
	private PipeLineSession session;

	@BeforeEach
	void setUp(DatabaseTestEnvironment env) {
		session = new PipeLineSession();

		listener = env.createBean();
		listener.setKeyField("MESSAGEKEY");
		listener.setMessageField("COMMENTS"); // Use comments-field for the message b/c it's a VARCHAR so easier to insert into with all databases
		listener.setMessageIdField("MESSAGEID");
		listener.setCorrelationIdField("CORRELATIONID");
		listener.setAdditionalFields("MESSAGEDATE,LABEL");
		listener.setSelectQuery("SELECT MESSAGEKEY, COMMENTS, MESSAGEID, CORRELATIONID, MESSAGEDATE, LABEL FROM " + TABLE_NAME);
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session);
		if (listener != null) {
			listener.stop(); // does this trigger an exception
		}
	}

	void insertData(String messageData, String messageId, String correlationId, String label) throws SQLException, JdbcException {
		try (Connection conn = listener.getConnection()) {
			JdbcTestUtil.executeStatement(conn,
					"INSERT INTO " + TABLE_NAME + "(COMMENTS, MESSAGEID, CORRELATIONID, LABEL, MESSAGEDATE) VALUES (?,?,?,?," +
					listener.getDbmsSupport().getSysDate() + ")", messageData, messageId, correlationId, label);
		}
	}

	void deleteAllData() throws SQLException, JdbcException {
		try (Connection conn = listener.getConnection()) {
			JdbcTestUtil.executeStatement(conn, "DELETE FROM " + TABLE_NAME);
		}
	}

	@DatabaseTest
	void hasRawMessageAvailableNo() throws Exception {
		// Arrange
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();

		// Act
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertFalse(hasRawMessageAvailable);
	}

	@DatabaseTest
	void hasRawMessageAvailableYes() throws Exception {
		// Arrange
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("value", "m1", "c1", "lbl");

		// Act
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertTrue(hasRawMessageAvailable);
	}

	@DatabaseTest
	void getRawMessageNullWhenNoData() throws Exception {
		// Arrange
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();

		// Act
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Assert
		assertFalse(hasRawMessageAvailable);
		assertNull(rawMessage);
	}

	@DatabaseTest
	void getRawMessageNoKeyField() throws Exception {
		// Arrange
		listener.setKeyField(null);
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertTrue(hasRawMessageAvailable);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
		assertNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
	}

	@DatabaseTest
	void getRawMessageNoMessageIdField() throws Exception {
		// Arrange
		listener.setMessageIdField(null);
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertTrue(hasRawMessageAvailable);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		Object storageKeyObj = rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY);
		assertNotNull(storageKeyObj);
		String storageKey = assertInstanceOf(String .class, storageKeyObj);
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals(storageKey, rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
	}

	@DatabaseTest
	void getRawMessageNoCorrelationIdField() throws Exception {
		// Arrange
		listener.setCorrelationIdField(null);
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertTrue(hasRawMessageAvailable);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertNotNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("m2", rawMessage.getCorrelationId());
	}

	@DatabaseTest
	void getRawMessageNoAfterSelectQuery() throws Exception {
		// Arrange
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailable = listener.hasRawMessageAvailable();

		// Assert
		assertTrue(hasRawMessageAvailable);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
		assertNotNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
		Object additionalQueryFields = rawMessage.getContext().get("Additional_Query_Fields");
		assertNotNull(additionalQueryFields);
		@SuppressWarnings("unchecked")
		Map<String,Message> additionalQueryFieldMap = assertInstanceOf (Map.class, additionalQueryFields);
		assertThat(additionalQueryFieldMap)
				.containsKey("label")
				.containsKey("messagedate");
		assertThat(additionalQueryFieldMap.get("label").asString()).isEqualTo("lbl");
	}

	@DatabaseTest
	void getRawMessageWithAfterSelectQuery() throws Exception {
		// Arrange
		listener.setAfterSelectQuery("DELETE FROM " + TABLE_NAME + " WHERE MESSAGEKEY = ?");
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		boolean hasRawMessageAvailableBeforeSelect = listener.hasRawMessageAvailable();
		assertTrue(hasRawMessageAvailableBeforeSelect);

		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailableAfterSelect = listener.hasRawMessageAvailable();

		// Assert
		assertFalse(hasRawMessageAvailableAfterSelect);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
		assertNotNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
	}

	@DatabaseTest
	void getRawMessageWithAfterSelectQueryNoMessageKey() throws Exception {
		// Arrange
		listener.setKeyField(null);
		listener.setAfterSelectQuery("DELETE FROM " + TABLE_NAME + " WHERE MESSAGEID = ?");
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		boolean hasRawMessageAvailableBeforeSelect = listener.hasRawMessageAvailable();
		assertTrue(hasRawMessageAvailableBeforeSelect);

		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailableAfterSelect = listener.hasRawMessageAvailable();

		// Assert
		assertFalse(hasRawMessageAvailableAfterSelect);
		assertNotNull(rawMessage);
		assertNotNull(rawMessage.getRawMessage());
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
		assertNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
	}

	@DatabaseTest
	void getRawMessageWithAfterProcessedQuery() throws Exception {
		// Arrange
		listener.setAfterProcessQuery("DELETE FROM " + TABLE_NAME + " WHERE MESSAGEKEY = ?");
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v2", "m2", "c2", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);
		assertNotNull(rawMessage);

		// Check if message is still available after selecting it
		boolean hasRawMessageAvailableAfterSelect = listener.hasRawMessageAvailable();
		assertTrue(hasRawMessageAvailableAfterSelect);

		listener.afterMessageProcessed(new PipeLineResult(), rawMessage, session);
		boolean hasRawMessageAvailableAfterProcesses = listener.hasRawMessageAvailable();

		// Assert
		assertFalse(hasRawMessageAvailableAfterProcesses);

		assertNotNull(rawMessage.getRawMessage());
		assertEquals("v2", rawMessage.getRawMessage().toString());
		assertEquals("m2", rawMessage.getId());
		assertEquals("c2", rawMessage.getCorrelationId());
		assertNotNull(rawMessage.getContext().get(PipeLineSession.STORAGE_ID_KEY));
	}

	@DatabaseTest
	void extractMessage() throws Exception {
		// Arrange
		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);
		deleteAllData();
		insertData("v3", "m3", "c3", "lbl");

		// Act
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);
		assertNotNull(rawMessage);
		Message message = listener.extractMessage(rawMessage, session);

		// Assert
		assertEquals("v3", message.asString());
	}

	@DatabaseTest
	void getRawMessageWithAfterSelectInTransaction(DatabaseTestEnvironment env) throws Exception {
		// Arrange
		listener.setAfterSelectQuery("DELETE FROM " + TABLE_NAME + " WHERE MESSAGEKEY = ?");

		assertDoesNotThrow(listener::configure);
		assertDoesNotThrow(listener::start);

		deleteAllData();
		insertData("v3", "m3", "c3", "lbl");

		// Act
		TransactionStatus txStatus1 = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRED);
		boolean hasRawMessageAvailableBeforeSelect = listener.hasRawMessageAvailable();
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(session);

		assertTrue(hasRawMessageAvailableBeforeSelect);
		assertNotNull(rawMessage);

		if (env.getDbmsSupport().hasSkipLockedFunctionality()) {
			TransactionStatus txStatus2 = env.startTransaction(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			boolean hasRawMessageAvailableAfterSelectBeforeCommit = listener.hasRawMessageAvailable();
			assertFalse(hasRawMessageAvailableAfterSelectBeforeCommit);

			env.getTxManager().commit(txStatus2);
		}
		env.getTxManager().rollback(txStatus1);

		boolean hasRawMessageAvailableAfterSelectAfterRollback = listener.hasRawMessageAvailable();
		assertTrue(hasRawMessageAvailableAfterSelectAfterRollback);
	}
}
