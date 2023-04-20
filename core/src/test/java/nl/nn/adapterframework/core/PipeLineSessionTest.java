package nl.nn.adapterframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.stream.Message;

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
		session.put("key8c", Message.asMessage("456"));
		session.put("key8d", Message.asByteArray((Object)"456"));
		session.put("key9", true);
		session.put("key9b", "true");
		session.put("key9c", "false");
		session.put("key10", map);
		session.put("key11", 123L);
		session.put("key11b", "456");
	}

	@Test
	public void testString() {
		assertEquals("test1", session.get("key1"));
		assertEquals("test1", session.get("key1", "default"));
		assertEquals("default", session.get("key1a", "default"));
	}

	@Test
	public void testTsSentTsReceived() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(1634150000000L);
		Date tsReceived = cal.getTime();

		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(1634500000000L);
		Date tsSent = cal2.getTime();

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
	public void testMessage() throws Exception {
		Message message1 = session.getMessage("key1");
		Message message2 = session.getMessage("key2");
		Message message3 = session.getMessage("key3");
		Message message4 = session.getMessage("key4");
		Message message5 = session.getMessage("doenst-exist");

		assertEquals("test1", message1.asString());
		assertEquals("test2", message2.asString());
		assertEquals("test3", message3.asString());
		assertEquals("test4", message4.asString());
		assertTrue(message5.isEmpty());
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
		assertTrue(session.get("key7") instanceof Date);
	}

	@Test
	public void testInteger() {
		assertEquals(123, session.get("key8"));
		assertEquals(123, session.get("key8", 0));
		assertEquals(0, session.get("key8a", 0));
		assertEquals(456, session.get("key8b", 0));
		assertEquals(456, session.get("key8c", 0));
		assertEquals(456, session.get("key8d", 0));
	}

	@Test
	public void testBoolean() {
		assertEquals(true, session.get("key9"));
		assertEquals(true, session.get("key9", false));
		assertEquals(false, session.get("key9a", false));
		assertEquals(true, session.get("key9b", false));
		assertEquals(false, session.get("key9c", true));
	}

	@Test
	public void testMap() {
		assertTrue(session.get("key10") instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) session.get("key10");
		assertEquals(3, map.size());
	}

	@Test
	public void testLong() {
		assertEquals(123L, session.get("key11"));
		assertEquals(123L, session.get("key11", 0L));
		assertEquals(0L, session.get("key11a", 0L));
		assertEquals(456L, session.get("key11b", 0L));
	}

	@Test
	public void ladybugStubMessageIDTest() {
		String messageId = "I am a messageID!";

		session.put(PipeLineSession.messageIdKey, messageId); //key inserted as String
		assertEquals(messageId, session.getMessageId());

		session.put(PipeLineSession.messageIdKey, Message.asMessage(messageId)); //key inserted as Message
		assertEquals(messageId, session.getMessageId());
	}

	@Test
	public void ladybugStubCorrelationIDTest() {
		String correlationId = "I am something else";

		session.put(PipeLineSession.correlationIdKey, correlationId); //key inserted as String
		assertEquals(correlationId, session.getCorrelationId());

		session.put(PipeLineSession.correlationIdKey, Message.asMessage(correlationId)); //key inserted as Message
		assertEquals(correlationId, session.getCorrelationId());
	}
}
