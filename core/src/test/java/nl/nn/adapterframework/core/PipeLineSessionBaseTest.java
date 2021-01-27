package nl.nn.adapterframework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import lombok.ToString;
import nl.nn.adapterframework.util.LogUtil;

public class PipeLineSessionBaseTest {
	protected Logger log = LogUtil.getLogger(this);

	@Mock
	private PipeLineSessionBase session = new PipeLineSessionBase();

	private static final double DELTA = 1e-15;
	private static final Object TEST_OBJECT = new Object();

	@Before
	public void setup() {
		session.put("boolean1", true);
		session.put("boolean2", false);
		session.put("boolean3", "true");
		session.put("boolean4", "false");

		session.put("string1", "test");
		session.put("string2", true);
		session.put("string3", "");
		session.put("string4", "null");
		session.put("string5", null);

		session.put("int1", 0);
		session.put("int2", 1);
		session.put("int3", -1);
		session.put("int4", "0");
		session.put("int5", "1");
		session.put("int6", "-1");

		session.put("double1", 1.0);
		session.put("double2", 1.23);
		session.put("double3", 0d);
		session.put("double4", 0.0d);
		session.put("double5", 123.456d);

		session.put("long1", 1L);
		session.put("long2", -1L);
		session.put("long3", 12345678910L);

		session.put("object1", TEST_OBJECT);
	}

	@Test
	public void testBoolean() {
		assertEquals(true, session.get("boolean1", true));
		assertEquals(true, session.get("boolean1", false));
		assertEquals(false, session.get("boolean2", true));
		assertEquals(false, session.get("boolean2", false));
		assertEquals(true, session.get("boolean3", true));
		assertEquals(true, session.get("boolean3", false));
		assertEquals(false, session.get("boolean4", false));
		assertEquals(false, session.get("boolean4", true));

		assertEquals(false, session.get("string3", true));
		assertEquals(false, session.get("string4", true));
		assertEquals(true, session.get("string5", true));

		assertEquals(false, session.get("object1", true));
		assertEquals(false, session.get("object1", false));
	}

	@Test
	public void testString() {
		assertEquals("test", session.get("string1", ""));
		assertEquals("test", session.get("string1", "not test"));
		assertEquals("true", session.get("string2", ""));
		assertEquals("true", session.get("string2", "false"));
		assertEquals("", session.get("string3", ""));
		assertEquals("", session.get("string3", "not empty"));
		assertEquals("null", session.get("string4", "null"));
		assertEquals("null", session.get("string4", "not null"));
		assertEquals(null, session.get("string5", null));
		assertEquals("", session.get("string5", new String()));
	}

	@Test
	public void testInt() {
		assertEquals(0, session.get("int1", 0));
		assertEquals(0, session.get("int1", -123));
		assertEquals(1, session.get("int2", 0));
		assertEquals(1, session.get("int2", -123));
		assertEquals(-1, session.get("int3", 0));
		assertEquals(-1, session.get("int3", -123));
		assertEquals(0, session.get("int4", 0));
		assertEquals(0, session.get("int4", -123));
		assertEquals(1, session.get("int5", 0));
		assertEquals(1, session.get("int5", -123));
		assertEquals(-1, session.get("int6", 0));
		assertEquals(-1, session.get("int6", -123));
		assertEquals(123, session.get("non-existing-key", 123));
	}

	@Test
	public void testDouble() {
		assertEquals(1.0, session.get("double1", 0d), DELTA);
		assertEquals(1, session.get("double1", -123d), DELTA);
		assertEquals(1.23, session.get("double2", 0d), DELTA);
		assertEquals(1.23, session.get("double2", -123d), DELTA);
		assertEquals(0, session.get("double3", 0d), DELTA);
		assertEquals(0, session.get("double3", -123d), DELTA);
		assertEquals(0, session.get("double4", 0d), DELTA);
		assertEquals(0, session.get("double4", -123d), DELTA);
		assertEquals(123.456, session.get("double5", 0d), DELTA);
		assertEquals(123.456, session.get("double5", -123d), DELTA);
	}

	@Test
	public void testLong() {
		assertEquals(1L, session.get("long1", 0L));
		assertEquals(1L, session.get("long1", -123L));
		assertEquals(-1L, session.get("long2", 0L));
		assertEquals(-1L, session.get("long2", -123L));
		assertEquals(12345678910L, session.get("long3", 0L));
		assertEquals(12345678910L, session.get("long3", -123L));
	}

	@Test
	public void testObject() {
		assertEquals(TEST_OBJECT, session.get("object1"));
		assertEquals(TEST_OBJECT.toString(), session.get("object1", "dummy"));
	}
	
	
	@ToString
	private class StateObservableInputStream extends InputStream {
		protected int closes = 0;
		protected String name;
		
		StateObservableInputStream(String name) {
			this.name=name;
		}
		
		@Override
		public void close() {
			log.debug("closing inputstream ["+name+"]");
			closes++;
		}

		@Override
		public int read() throws IOException {
			return 0;
		}
	}
	
	@Test
	public void testCloseables() throws IOException {
		StateObservableInputStream a = new StateObservableInputStream("a");
		StateObservableInputStream b = new StateObservableInputStream("b");
		StateObservableInputStream c = new StateObservableInputStream("c");
		StateObservableInputStream d = new StateObservableInputStream("d");
		
		InputStream p = session.scheduleCloseOnSessionExit(a);
		InputStream q = session.scheduleCloseOnSessionExit(a);
		assertTrue("scheduling a resource twice must yield the same object", p==q); 
		
		InputStream r = session.scheduleCloseOnSessionExit(b);
		InputStream s = session.scheduleCloseOnSessionExit(c);
		InputStream t = session.scheduleCloseOnSessionExit(d);
		InputStream u = session.scheduleCloseOnSessionExit(t);
		assertTrue("rescheduling the wrapper of a scheduled object must yield the same wrapped object", u==t);

		log.debug("test calling close on wrapped(b)");
		r.close();

		log.debug("test unschedule wrapped(c)");
		session.unscheduleCloseOnSessionExit(s);
		
		session.close();
		
		assertEquals(1, a.closes);
		assertEquals(1, b.closes);
		assertEquals(0, c.closes);
		assertEquals(1, d.closes);
	}
}
