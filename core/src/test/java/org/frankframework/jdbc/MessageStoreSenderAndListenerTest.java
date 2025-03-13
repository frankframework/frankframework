package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.CloseUtils;

@WithLiquibase(tableName = MessageStoreSenderAndListenerTest.TEST_TABLE_NAME)
public class MessageStoreSenderAndListenerTest {

	static final String TEST_TABLE_NAME = "MESSAGE_STORE_SENDER_AND_LISTENER_TEST";
	private static final String SLOT_ID = "mssalt";
	private static final String TEST_DATA = "test content";

	MessageStoreSender sender;
	MessageStoreListener listener;

	PipeLineSession session;
	Message input;
	Message result;

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) {
		Receiver<Serializable> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);

		listener = env.createBean(MessageStoreListener.class);
		listener.setTableName(TEST_TABLE_NAME);
		listener.setSlotId(SLOT_ID);
		listener.setReceiver(receiver);

		sender = env.createBean(MessageStoreSender.class);
		sender.setTableName(TEST_TABLE_NAME);
		sender.setSlotId(SLOT_ID);

		session = new PipeLineSession();
		session.put("key1", "value1");
		session.put("key2", "value2");
		PipeLineSession.updateListenerParameters(session, "msg-id", "msg-cid");

		input = Message.asMessage(new StringReader(TEST_DATA));
	}


	@AfterEach
	public void teardown() {
		CloseUtils.closeSilently(session, input, result);
		if (listener != null) {
			listener.stop(); //does this trigger an exception
		}
		if (sender != null) {
			sender.stop();
		}
	}

	@DatabaseTest
	public void testSendReceiveWithoutSessionKeys() throws Exception {
		// Arrange
		sender.configure();
		sender.start();
		listener.configure();
		listener.start();

		Map<String, Object> threadContext = new HashMap<>();

		// Act
		sender.sendMessage(input, session);
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(threadContext);
		result = listener.extractMessage(rawMessage, threadContext);

		// Assert
		assertNotNull(result);
		assertEquals(TEST_DATA, result.asString());

		assertFalse(threadContext.containsKey("key1"), "ThreadContext should not contain key1");
		assertFalse(threadContext.containsKey("key2"), "ThreadContext should not contain key2");
	}

	@DatabaseTest
	public void testSendReceiveSessionKeys() throws Exception {
		// Arrange
		sender.setSessionKeys("key1, key3");

		sender.configure();
		sender.start();
		listener.configure();
		listener.start();

		Map<String, Object> threadContext = new HashMap<>();

		// Act
		sender.sendMessage(input, session);
		RawMessageWrapper<Serializable> rawMessage = listener.getRawMessage(threadContext);
		result = listener.extractMessage(rawMessage, threadContext);

		// Assert
		assertNotNull(result);
		assertEquals(TEST_DATA, result.asString());

		assertTrue(threadContext.containsKey("key1"), "ThreadContext should key1");
		assertFalse(threadContext.containsKey("key2"), "ThreadContext should not contain key2"); // Key not specified
		assertFalse(threadContext.containsKey("key3"), "ThreadContext should not contain key3"); // Key does not exist in session
	}
}
