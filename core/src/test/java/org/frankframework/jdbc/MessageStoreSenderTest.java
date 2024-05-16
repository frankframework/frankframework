package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.UUID;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@WithLiquibase(tableName = MessageStoreSenderTest.TABLE_NAME)
public class MessageStoreSenderTest {
	public static final String TABLE_NAME = "MSG_STOR_SND_TST";

	private MessageStoreSender sender;
	private PipeLineSession session;

	@BeforeEach
	public void setUp(DatabaseTestEnvironment databaseTestEnvironment) {

		sender = databaseTestEnvironment.getConfiguration().createBean(MessageStoreSender.class);
		sender.setDatasourceName(databaseTestEnvironment.getDataSourceName());
		sender.setTableName(TABLE_NAME);
		sender.setSequenceName("SEQ_" + TABLE_NAME);

		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown(DatabaseTestEnvironment databaseTestEnvironment) {
		if (session != null) {
			session.close();
		}
	}

	@DatabaseTest
	public void testSendMessageBasic() throws ConfigurationException, SenderException, TimeoutException, IOException {
		// Arrange
		sender.setSlotId("testSendMessageBasic");
		sender.configure();
		sender.open();

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
		assertTrue(result.getResult().asString().matches("<id>\\d+</id>"));
	}
}
