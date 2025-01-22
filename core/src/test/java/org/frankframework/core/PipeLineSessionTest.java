package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.input.ReaderInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.MessageTestUtils.MessageType;

/**
 * Lots of bugs tests, focus on getMessage(String)
 */
public class PipeLineSessionTest {

	private PipeLineSession session;

	@BeforeEach
	public void setUp() throws Exception {
		session = new PipeLineSession();

		Map<String, Object> map = new HashMap<>();
		map.put("1", 1);
		map.put("2", true);
		map.put("3", "string");

		List<Object> list = new ArrayList<>(map.values());
		list.add("123");
		list.add("456");
		list.add("789");

		session.put("key1", "test");
		session.put("key1", "test1"); //Overwrite a key
		session.put("key2", new Message("test2"));
		session.put("key3", "test3".getBytes());
		session.put("key4", new ByteArrayInputStream("test4".getBytes()));
		session.put("key5", list);
		session.put("key6", list.toArray());
		session.put("key7", new Date());
		session.put("key8", 123);
		session.put("key8b", "456");
		session.put("key8c", new Message("456"));
		session.put("key8d", "456".getBytes());
		session.put("key9", true);
		session.put("key9b", "true");
		session.put("key9c", "false");
		session.put("key10", map);
		session.put("key11", 123L);
		session.put("key11b", "456");
	}

	@Test
	public void testPutWithCloseable() {
		// Arrange
		InputStream closeable = new ReaderInputStream(new StringReader("xyz"));

		// Act
		session.put("x", closeable);

		// Assert
		assertTrue(session.getCloseables().contains(closeable));
	}

	@Test
	public void testPutAllWithCloseable() {
		// Arrange
		InputStream closeable = new ReaderInputStream(new StringReader("xyz"));
		Map<String, Object> values = new HashMap<>();
		values.put("v1", 1);
		values.put("v2", 2);
		values.put("x", closeable);

		// Act
		session.putAll(values);

		// Assert
		assertTrue(session.getCloseables().contains(closeable));
	}

	@Test
	public void testString() {
		assertEquals("test1", session.get("key1"));
		assertEquals("test1", session.get("key1", "default"));
		assertEquals("default", session.get("key1a", "default"));
	}

	@Test
	public void testTsSentTsReceived() {
		Instant tsReceived = Instant.ofEpochMilli(1634150000000L);
		Instant tsSent = Instant.ofEpochMilli(1634500000000L);

		//Should set the value as a String
		PipeLineSession.updateListenerParameters(session, null, null, tsReceived, tsSent);

		assertEquals(tsReceived, session.getTsReceived());
		assertEquals(tsSent, session.getTsSent());

		//Sets the raw value
		session.put(PipeLineSession.TS_RECEIVED_KEY, tsReceived);
		session.put(PipeLineSession.TS_SENT_KEY, tsSent);

		assertEquals(tsReceived, session.getTsReceived());
		assertEquals(tsSent, session.getTsSent());
	}

	@Test
	public void testGetMessage() throws Exception {
		Message message1 = session.getMessage("key1");
		Message message2 = session.getMessage("key2");
		Message message3 = session.getMessage("key3");
		Message message4 = session.getMessage("key4");
		Message message5 = session.getMessage("doesnt-exist");

		assertEquals("test1", message1.asString());
		assertEquals("test2", message2.asString());
		assertEquals("test3", message3.asString());
		assertEquals("test4", message4.asString());
		assertTrue(message5.isEmpty(), "If key does not exist, result message should be empty");
		assertFalse(((Message) session.get("key2")).isNull(), "SessionKey 'key2' stored in Message should not be closed after reading value");
	}

	@Test
	public void testGetString() {
		String message1 = session.getString("key1");
		String message2 = session.getString("key2");
		String message3 = session.getString("key3");
		String message4 = session.getString("key4");
		String message5 = session.getString("doesnt-exist");

		assertEquals("test1", message1);
		assertEquals("test2", message2);
		assertEquals("test3", message3);
		assertEquals("test4", message4);
		assertNull(message5, "If key does not exist, result string should be NULL");
		assertFalse(((Message) session.get("key2")).isNull(), "SessionKey 'key2' stored in Message should not be closed after reading value");
	}

	@Test
	public void testList() {
		assertEquals("[1, true, string, 123, 456, 789]", session.get("key5").toString());
	}

	@Test
	public void testArray() {
		assertTrue(session.get("key6") instanceof Object[]);
		Object[] array = (Object[]) session.get("key6");
		assertEquals(6, array.length);
	}

	@Test
	public void testDate() {
    assertInstanceOf(Date.class, session.get("key7"));
	}

	@Test
	public void testInteger() {
		// Arrange
		session.put("key8e", "123");
		session.put("key8f", 123L);
		session.put("key8g", new Message("123"));

		// Act / Assert
		assertEquals(123, session.getInteger("key8"));
		assertEquals(123, session.getInteger("key8e"));
		assertEquals(123, session.getInteger("key8f"));
	}

	@Test
	public void testIntegerWithDefault() {
		assertEquals(123, session.get("key8"));
		assertEquals(123, session.get("key8", 0));
		assertEquals(0, session.get("key8a", 0));
		assertEquals(456, session.get("key8b", 0));
		assertEquals(456, session.get("key8c", 0));
		assertEquals(456, session.get("key8d", 0));

		assertFalse(((Message) session.get("key8c")).isNull(), "SessionKey 'key8c' stored in Message should not be closed after reading value");
	}

	@Test
	public void testBoolean() {
		// Arrange
		session.put("key9d", "true");
		session.put("key9e", "false");

		// Act / Assert
		assertTrue(session.getBoolean("key9"));
		assertTrue(session.getBoolean("key9d"));
		assertFalse(session.getBoolean("key9e"));
		assertNull(session.getBoolean("key-not-there"));
	}

	@Test
	public void testBooleanWithDefault() {
		assertTrue((Boolean) session.get("key9"));
		assertTrue(session.get("key9", false));
		assertFalse(session.get("key9a", false));
		assertTrue(session.get("key9b", false));
		assertFalse(session.get("key9c", true));
	}

	@Test
	public void testMap() {
    	assertInstanceOf(Map.class, session.get("key10"));
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) session.get("key10");
		assertEquals(3, map.size());
	}

	@Test
	public void testLongWithDefault() {
		assertEquals(123L, session.get("key11"));
		assertEquals(123L, session.get("key11", 0L));
		assertEquals(0L, session.get("key11a", 0L));
		assertEquals(456L, session.get("key11b", 0L));
	}

	@Test
	public void ladybugStubMessageIDTest() {
		String messageId = "I am a messageID!";

		session.put(PipeLineSession.MESSAGE_ID_KEY, messageId); //key inserted as String
		assertEquals(messageId, session.getMessageId());

		session.put(PipeLineSession.MESSAGE_ID_KEY, new Message(messageId)); //key inserted as Message
		assertEquals(messageId, session.getMessageId());
	}

	@Test
	public void ladybugStubCorrelationIDTest() {
		String correlationId = "I am something else";

		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId); //key inserted as String
		assertEquals(correlationId, session.getCorrelationId());

		session.put(PipeLineSession.CORRELATION_ID_KEY, new Message(correlationId)); //key inserted as Message
		assertEquals(correlationId, session.getCorrelationId());
	}

	/**
	 * Method: mergeToParentSession(String keys, Map<String,Object> from, Map<String,Object>
	 * to)
	 */
	@Test
	public void testMergeToParentSession() {
		// Arrange
		PipeLineSession from = new PipeLineSession();
		PipeLineSession to = new PipeLineSession();
		String keys = "a,b";
		from.put("a", 15);
		from.put("b", 16);

		// Act
		from.mergeToParentSession(keys, to);

		// Assert
		assertEquals(from,to);
	}

	@Test
	public void testMergeToParentContextMap() {
		// Arrange
		PipeLineSession from = new PipeLineSession();
		Map<String, Object> to = new HashMap<>();
		String keys = "a,b";
		from.put("a", 15);
		from.put("b", 16);

		Message message1 = new Message("17");
		from.put("c", message1);
		to.put("c", message1);

		// Act
		from.mergeToParentSession(keys, to);

		// Assert
		assertEquals(from,to);
	}

	@Test
	public void testMergeToParentContextMap2() throws Exception {
		// Arrange
		PipeLineSession parent = new PipeLineSession();
		Message originalA = new Message("0");
		parent.put("a", originalA);
		Message originalB = new Message("2");
		parent.put("b", originalB);

		// Act
		for(int i = 1; i <= 10; i++) {
			AutoCloseable closeable = mock(AutoCloseable.class);
			try (PipeLineSession sub = new PipeLineSession()) {
				sub.put("a", Message.asMessage(parent.getMessage("a").asString()));
				Message bMessage = Message.asMessage(parent.getMessage("b").asString());
				sub.put("b", bMessage);
				sub.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, bMessage);

				sub.scheduleCloseOnSessionExit(closeable);
				sub.scheduleCloseOnSessionExit(closeable); // Just for good measure, add it twice..
				sub.put("c", closeable); //Store under `c` which we merge later on

				for(int j = 1; j <= 10; j++) {

					AutoCloseable streamMessage = spy(MessageTestUtils.getMessage(MessageType.BINARY));
					AutoCloseable closeMe = spy(Message.asMessage(""+j));
					sub.put("d", closeMe);
					try (PipeLineSession child = new PipeLineSession(sub)) {
						assertEquals(j, sub.getInteger("d"));

						int valueA = child.getInteger("a") + 1;
						Message valueAsMessage = Message.asMessage(""+valueA);
						child.put("a", valueAsMessage); // Should be a string-value Message
						child.put("b", Message.asMessage(sub.getMessage("b").asString()));

						child.scheduleCloseOnSessionExit(streamMessage);
						child.put("d", streamMessage);

						// Overwrite values
						child.put("c", i);
						child.put("d", j);

						child.mergeToParentSession("a,b,c,d", sub);
					}

					assertEquals(1, sub.getCloseables().size()); // <<< Keeps growing without the change!
					verify(streamMessage, times(1)).close();
					assertEquals(j, sub.getInteger("d")); // Only 1 `d`, with the correct value.
				}

				assertEquals(10, sub.getInteger("a"));
				assertEquals(10, sub.getInteger("d"));

				sub.mergeToParentSession("b,c", parent);
			}
			verify(closeable, times(1)).close();

			assertEquals(i, parent.get("c"));
		}


		// Assert
		assertEquals(0, parent.getCloseables().size());
		assertEquals(originalA, parent.get("a"));
		assertEquals("2", parent.getMessage("b").asString());
		assertEquals(10, parent.get("c"));
		assertNull(parent.get("d"));
	}

	@ParameterizedTest
	@CsvSource(value = {"*", ","})
	public void testMergeToParentSessionCopyAllKeys(String keysToCopy) throws Exception {
		// Arrange
		PipeLineSession from = new PipeLineSession();
		PipeLineSession to = new PipeLineSession();
		Message message = new Message("a message");
		Message messageOfCloseable = new Message(new StringReader("a message is closeable"));
		BufferedReader closeable1 = new BufferedReader(new StringReader("a closeable"));
		Message closeable2 = new Message(new StringReader("a message is closeable"));
		from.put("a", 15);
		from.put("b", 16);
		from.put("c", message);
		from.put("d", closeable1);
		from.put("__e", closeable2);
		from.put("f", messageOfCloseable);

		from.scheduleCloseOnSessionExit(message);

		// Act
		from.mergeToParentSession(keysToCopy, to);

		from.close();

		// Assert
		assertEquals(from,to);

		assertFalse(to.getCloseables().contains(message));
		assertTrue(to.getCloseables().contains(messageOfCloseable));
		assertTrue(to.getCloseables().contains(closeable1));
		assertFalse(to.getCloseables().contains(closeable2));
		assertFalse(((Message) to.get("c")).isNull());
		assertEquals("a message", ((Message) to.get("c")).asString());
		assertEquals("a closeable", ((BufferedReader) to.get("d")).readLine());

		// Act
		to.close();

		// Assert
		assertFalse(((Message) to.get("c")).isNull()); // String message are no longer closed
		assertFalse(((Message) to.get("__e")).isNull());
		assertTrue(((Message) to.get("f")).isNull());
	}

	@Test
	public void testMergeToParentSessionLimitedKeys() throws Exception {
		// Arrange
		PipeLineSession from = new PipeLineSession();
		PipeLineSession to = new PipeLineSession();

		Message message1 = new Message("m1");
		Message message2 = new Message("m2");

		String keys = "a,c";
		from.put("a", 15);
		from.put("b", 16);
		from.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, "exitCode");
		from.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, "exitState");

		// Same key different objects; same object different keys.
		// Afterwards message1 should be closed and message2 should not be.
		from.put("d", message1);
		from.put("e", message2);
		to.put("d", message2);

		from.scheduleCloseOnSessionExit(message1);

		// Act
		from.mergeToParentSession(keys, to);

		from.close();

		// Assert
		assertEquals(5, to.size());
		assertTrue(to.containsKey("a"));
		assertTrue(to.containsKey("c"));
		assertTrue(to.containsKey(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
		assertTrue(to.containsKey(PipeLineSession.EXIT_STATE_CONTEXT_KEY));
		assertEquals(15, to.get("a"));
		assertNull(to.get("c"));

		assertTrue(message1.isNull());
		assertEquals("m2", message2.asString());
	}

	@Test
	public void testMergeToParentSessionEmptyKeys() {
		PipeLineSession from = new PipeLineSession();
		PipeLineSession to = new PipeLineSession();
		from.put("a", 15);
		from.put("b", 16);
		from.mergeToParentSession("", to);
		assertEquals(0,to.size());
	}

	@Test
	public void testNotCloseSystemCloseables() throws Exception {
		// Arrange
		jakarta.jms.Session jmsSession = mock(AutoCloseableJmsSession.class);
		session.put("__JmsSession", jmsSession);
		doAnswer(params -> fail("Should not close JMS Session")).when(jmsSession).close();

		java.sql.Connection jdbcConnection = mock(java.sql.Connection.class);
		session.put("CloseThis", jdbcConnection);

		// Act
		session.close();

		// Assert
		verify(jdbcConnection, times(1)).close();
	}

	private interface AutoCloseableJmsSession extends jakarta.jms.Session, AutoCloseable {
		// No methods added
	}
}
