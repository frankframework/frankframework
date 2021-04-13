package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.stream.Message;

/**
 * Lots of bugs tests, focus on getMessage(String)
 */
public class PipeLineSessionTest {

	private PipeLineSession session;

	@Before
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
		session.put("key9", true);
		session.put("key10", map);
	}

	@Test
	public void testString() {
		assertEquals("test1", session.get("key1"));
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
	}

	@Test
	public void testBoolean() {
		assertEquals(true, session.get("key9"));
	}

	@Test
	public void testMap() {
		assertTrue(session.get("key10") instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) session.get("key10");
		assertEquals(3, map.size());
	}
}
