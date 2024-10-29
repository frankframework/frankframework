package org.frankframework.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ConfigurableApplicationContext;

import lombok.Getter;

import org.frankframework.core.Adapter;
import org.frankframework.monitoring.events.ConsoleMonitorEvent;
import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlBuilder;

public class TriggerTest implements EventThrowing {
	private static final String EVENT_CODE = "dummy code";
	private @Getter String eventSourceName = "TriggerTestClass";
	private @Getter Adapter adapter;

	private ConfigurableApplicationContext applContext;
	private IMonitorDestination destination;
	private MonitorManager manager;
	private Monitor monitor;

	@BeforeEach
	public void setup() {
		applContext = mock(ConfigurableApplicationContext.class);
		destination = mock(IMonitorDestination.class);
		when(destination.getName()).thenReturn("dummy destination");

		manager = spy(MonitorManager.class);
		monitor = spy(Monitor.class);
		monitor.setApplicationContext(applContext);

		monitor.setName("monitorName");
		manager.addMonitor(monitor);
		manager.addDestination(destination);
		monitor.setDestinations(destination.getName());
	}

	@Test
	public void testTriggerEvent() throws Exception {
		// Arrange
		Trigger trigger = spy(Alarm.class);

		ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
		ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
		ArgumentCaptor<MonitorEvent> monitorEventCaptor = ArgumentCaptor.forClass(MonitorEvent.class);

		doNothing().when(destination).fireEvent(anyString(), eventTypeCaptor.capture(), severityCaptor.capture(), anyString(), monitorEventCaptor.capture());

		monitor.addTrigger(trigger);

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);

		manager.configure();

		// Act
		FireMonitorEvent event = new FireMonitorEvent(this, EVENT_CODE);
		trigger.onApplicationEvent(event);

		// Assert
		verify(trigger, times(1)).configure();
		assertTrue(trigger.isAlarm());
		assertEquals(EventType.TECHNICAL, eventTypeCaptor.getValue());
		assertEquals(Severity.CRITICAL, severityCaptor.getValue());
		MonitorEvent capturedEvent = monitorEventCaptor.getValue();
		assertNotNull(capturedEvent);
		assertEquals(this, capturedEvent.getSource());
		assertNull(capturedEvent.getEventMessage());
		assertTrue(monitor.isRaised());
		assertNotNull(monitor.getRaisedBy());
		assertEquals("TriggerTestClass", capturedEvent.getEventSourceName());
		assertEquals(EVENT_CODE, capturedEvent.getEventCode());
		assertEquals(0, monitor.getAdditionalHitCount());

		// Act
		trigger.onApplicationEvent(event);

		// Assert
		assertEquals(1, monitor.getAdditionalHitCount());
		assertTrue(monitor.isRaised());
	}

	@Test
	public void testTriggerMultipleEventsWithThreshold() throws Exception {
		// Arrange
		Trigger trigger = spy(Alarm.class);

		ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
		ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
		ArgumentCaptor<String> eventCode = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<MonitorEvent> monitorEventCaptor = ArgumentCaptor.forClass(MonitorEvent.class);

		doNothing().when(destination).fireEvent(anyString(), eventTypeCaptor.capture(), severityCaptor.capture(), eventCode.capture(), monitorEventCaptor.capture());

		monitor.addTrigger(trigger);

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);
		trigger.setThreshold(5);
		trigger.setPeriod(1);

		manager.configure();

		// Act
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE));

		// Assert
		verify(trigger, times(1)).configure();
		assertFalse(monitor.isRaised());
		assertEquals(0, monitor.getAdditionalHitCount());

		// Act
		Thread.sleep(1050); // Invalidates the first event
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE));
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE));
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE));
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE));

		// Assert
		assertFalse(monitor.isRaised());

		// Act
		trigger.onApplicationEvent(new FireMonitorEvent(this, EVENT_CODE)); // Trigger a new event to reach the threshold of 5

		// Assert
		assertEquals(0, monitor.getAdditionalHitCount());
		assertTrue(monitor.isRaised());
		assertEquals(EventType.TECHNICAL, eventTypeCaptor.getValue());
		assertEquals(Severity.CRITICAL, severityCaptor.getValue());
		assertNotNull(monitorEventCaptor.getValue());
	}

	@Test
	public void testTriggerMultipleEventsHitCount() throws Exception {
		// Arrange
		Trigger trigger = spy(Alarm.class);

		ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
		ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
		ArgumentCaptor<MonitorEvent> monitorEventCaptor = ArgumentCaptor.forClass(MonitorEvent.class);

		doNothing().when(destination).fireEvent(anyString(), eventTypeCaptor.capture(), severityCaptor.capture(), anyString(), monitorEventCaptor.capture());

		monitor.addTrigger(trigger);
		monitor.setAlarmSeverity(Severity.WARNING);

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.WARNING);

		manager.configure();

		// Act
		FireMonitorEvent event = new FireMonitorEvent(this, EVENT_CODE);
		trigger.onApplicationEvent(event);

		// Assert
		verify(trigger, times(1)).configure();
		assertTrue(monitor.isRaised());
		assertEquals(0, monitor.getAdditionalHitCount());

		// Act
		trigger.onApplicationEvent(event);
		trigger.onApplicationEvent(event);
		trigger.onApplicationEvent(event);
		trigger.onApplicationEvent(event);

		// Assert
		assertEquals(4, monitor.getAdditionalHitCount());
		assertTrue(monitor.isRaised());
		assertEquals(EventType.TECHNICAL, eventTypeCaptor.getValue());
		assertEquals(Severity.WARNING, severityCaptor.getValue());
		assertNotNull(monitorEventCaptor.getValue());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testTriggerAndClearConsoleEvent(boolean eventCausedByMonitor) throws Exception {
		// Arrange
		Trigger trigger = spy(Alarm.class);

		ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
		ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
		ArgumentCaptor<String> eventCode = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<MonitorEvent> monitorEventCaptor = ArgumentCaptor.forClass(MonitorEvent.class);

		doNothing().when(destination).fireEvent(anyString(), eventTypeCaptor.capture(), severityCaptor.capture(), eventCode.capture(), monitorEventCaptor.capture());

		monitor.addTrigger(trigger);

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);

		manager.configure();

		// Act
		MonitorEvent event = eventCausedByMonitor ? new ConsoleMonitorEvent("dummyUser") : new FireMonitorEvent(this, EVENT_CODE);
		monitor.changeState(true, Severity.CRITICAL, event);

		// Assert
		verify(trigger, times(1)).configure();
		assertTrue(trigger.isAlarm());
		assertEquals(EventType.TECHNICAL, eventTypeCaptor.getValue());
		assertEquals(Severity.CRITICAL, severityCaptor.getValue());
		MonitorEvent capturedEvent = monitorEventCaptor.getValue();
		assertNotNull(capturedEvent);
		assertNull(capturedEvent.getEventMessage());
		assertTrue(monitor.isRaised());
		assertNotNull(monitor.getRaisedBy());
		assertEquals(0, monitor.getAdditionalHitCount());

		// Act
		monitor.changeState(false, Severity.WARNING, new ConsoleMonitorEvent("dummyUser")); //WARNING, cleared by MONITOR

		// Assert
		assertEquals(EventType.CLEARING, eventTypeCaptor.getValue());
		assertEquals(Severity.CRITICAL, severityCaptor.getValue()); // OLD STATE 'CRITICAL'
		MonitorEvent capturedEvent2 = monitorEventCaptor.getValue();
		assertNotNull(capturedEvent2);
		assertNull(capturedEvent2.getEventMessage());
		assertFalse(monitor.isRaised());
		assertNull(monitor.getRaisedBy());
		assertEquals(0, monitor.getAdditionalHitCount());
		assertEquals(eventCausedByMonitor ? "CONSOLE" : EVENT_CODE, eventCode.getValue());
		assertEquals("Frank!Console on behalf of 'dummyUser'", capturedEvent2.getEventSourceName());
		assertEquals("CONSOLE", capturedEvent2.getEventCode());
	}

	@Test
	public void testMonitoringXML() throws Exception {
		// Arrange
		Trigger trigger = spy(Alarm.class);
		Monitor monitor = spy(Monitor.class);
		monitor.setApplicationContext(applContext);
		MonitorManager manager = spy(MonitorManager.class);
		IMonitorDestination destination = mock(IMonitorDestination.class);
		when(destination.getName()).thenReturn("dummy destination");
		when(destination.toXml()).thenReturn(new XmlBuilder("destination"));

		manager.addMonitor(monitor);
		monitor.addTrigger(trigger);
		manager.addDestination(destination);
		monitor.setDestinations(destination.getName());
		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);
		monitor.setAlarmSeverity(Severity.WARNING);

		// Act
		manager.configure();

		// Assert
		verify(trigger, times(1)).configure();
		assertEquals(TestFileUtils.getTestFile("/Management/Monitoring/getManagerToXML.xml"), manager.toXml().asXmlString());
	}
}
