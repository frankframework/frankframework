package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.CloseUtils;

@WithLiquibase(tableName = MessageStoreSenderTest.TABLE_NAME)
public class MessageStoreSenderTest {
	public static final String TABLE_NAME = "MSG_STOR_SND_TST";

	private DatabaseTestEnvironment env;
	private MessageStoreSender sender;
	private PipeLineSession session;

	@BeforeEach
	public void setUp(DatabaseTestEnvironment databaseTestEnvironment) {
		env = databaseTestEnvironment;

		sender = databaseTestEnvironment.getConfiguration().createBean();
		sender.setDatasourceName(databaseTestEnvironment.getDataSourceName());
		sender.setTableName(TABLE_NAME);
		sender.setSequenceName("SEQ_" + TABLE_NAME);

		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) {
		CloseUtils.closeSilently(session);
	}

	@DatabaseTest
	public void testSendMessageBasic() throws ConfigurationException, SenderException, TimeoutException, IOException, SQLException {
		// Arrange
		sender.setSlotId("testSendMessageBasic");
		sender.configure();
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);

		String messageId = UUID.randomUUID().toString();
		String correlationId = "cid-" + messageId;
		session.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getResult());
		assertTrue(result.getResult().asString().matches("<id>\\d+</id>"), "Message [" + result.getResult().asString() + "] did not match pattern [<id>\\d+</id>]");

		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));
	}

	@DatabaseTest
	public void testSendMessageMessageIdFromParameter() throws ConfigurationException, SenderException, TimeoutException, IOException, SQLException {
		// Arrange
		String messageId = UUID.randomUUID().toString();
		sender.setSlotId("testSendMessageBasic");
		sender.addParameter(new Parameter(MessageStoreSender.PARAM_MESSAGEID, messageId));
		sender.configure();
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);

		String correlationId = "cid-" + messageId;
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		// Act
		SenderResult result = sender.sendMessage(message, session);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getResult());
		assertTrue(result.getResult().asString().matches("<id>\\d+</id>"), "Message [" + result.getResult().asString() + "] did not match pattern [<id>\\d+</id>]");

		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));
	}

	@DatabaseTest
	public void testSendMessageStore2ndTimeNoInsert() throws ConfigurationException, SenderException, TimeoutException, IOException, SQLException {
		// Arrange
		sender.setSlotId("testSendMessageStore2ndTimeNoInsert");
		sender.configure();
		sender.start();

		String input = "<dummy/>";
		Message message = new Message(input);

		String messageId = UUID.randomUUID().toString();
		String correlationId = "cid-" + messageId;
		session.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		// Act 1 - First store, does insert
		SenderResult result1 = sender.sendMessage(message, session);

		// Assert 1
		assertNotNull(result1);
		assertNotNull(result1.getResult());
		assertTrue(result1.getResult().asString().matches("<id>\\d+</id>"), "Message [" + result1.getResult().asString() + "] did not match pattern [<id>\\d+</id>]");

		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));

		// Act 2 - Second store, does not insert
		SenderResult result2 = sender.sendMessage(message, session);

		// Assert 2
		assertNotNull(result2);
		assertNotNull(result2.getResult());
		String expected = "<results>" + result1.getResult().asString() +
				"<result>WARN_MESSAGEID_ALREADY_EXISTS</result></results>";
		assertEquals(expected, result2.getResult().asString());
		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));
	}

	@DatabaseTest
	public void testSendMessageStore2ndTimeNoInsertMessageIsDifferent() throws ConfigurationException, SenderException, TimeoutException, IOException, SQLException {
		// Arrange
		sender.setSlotId("testSendMessageStore2ndTimeNoInsert");
		sender.configure();
		sender.start();

		String input = "<dummy1/>";
		Message message1 = new Message(input);

		String messageId = UUID.randomUUID().toString();
		String correlationId = "cid-" + messageId;
		session.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		// Act 1 - First store, does insert
		SenderResult result1 = sender.sendMessage(message1, session);

		// Assert 1
		assertNotNull(result1);
		assertNotNull(result1.getResult());
		assertTrue(result1.getResult().asString().matches("<id>\\d+</id>"), "Message [" + result1.getResult().asString() + "] did not match pattern [<id>\\d+</id>]");

		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));

		// Arrange 2 - Different message
		Message message2 = new Message("Another Message");
		// Act 2 - Second store, does not insert
		SenderResult result2 = sender.sendMessage(message2, session);

		// Assert 2
		assertNotNull(result2);
		assertNotNull(result2.getResult());
		String expected = "<results>" + result1.getResult().asString() +
				"<result>WARN_MESSAGEID_ALREADY_EXISTS</result><result>ERROR_MESSAGE_IS_DIFFERENT</result></results>";
		assertEquals(expected, result2.getResult().asString());
		assertEquals(1, countRecordsBySlotAndMessageId(sender.getSlotId(), messageId));
	}

	public int countRecordsBySlotAndMessageId(String slotId, String messageId) throws SQLException {
		try (Connection connection = env.getConnection()) {
			String selectQuery = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + sender.getSlotIdField() + "=? AND " + sender.getIdField() + "=?";
			System.err.println("Counting query: " + selectQuery);
			try (PreparedStatement statement =connection.prepareStatement(selectQuery)) {
				statement.setString(1, slotId);
				statement.setString(2, messageId);
				try (ResultSet rs = statement.executeQuery()) {
					if (rs.next()) {
						return rs.getInt(1);
					} else {
						fail("The query [" + selectQuery + "] did not return any results");
						return -1;
					}
				}
			}
		}
	}
}
