package nl.nn.adapterframework.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.monitoring.events.Event;
import nl.nn.adapterframework.monitoring.events.MonitorEvent;

public class MonitorEventTest {

	@Test
	public void test2TheSameEventsButDifferentObjects() {
		Map<MonitorEvent, Event> events = new HashMap<>();

		MonitorEvent monitorEvent = new MonitorEvent("test");
		Event event = events.getOrDefault(monitorEvent, new Event());
		events.put(monitorEvent, event);

		MonitorEvent monitorEvent2 = new MonitorEvent("test");
		Event event2 = events.getOrDefault(monitorEvent2, new Event());
		events.put(monitorEvent2, event2);

		assertTrue(monitorEvent.equals(monitorEvent2));
		assertEquals(1, events.size());
	}

	@Test
	public void test2DifferentEvents() {
		Map<MonitorEvent, Event> events = new HashMap<>();

		MonitorEvent monitorEvent = new MonitorEvent("test1");
		Event event = events.getOrDefault(monitorEvent, new Event());
		events.put(monitorEvent, event);

		MonitorEvent monitorEvent2 = new MonitorEvent("test2");
		Event event2 = events.getOrDefault(monitorEvent2, new Event());
		events.put(monitorEvent2, event2);

		assertFalse(monitorEvent.equals(monitorEvent2));
		assertEquals(2, events.size());
	}

}
