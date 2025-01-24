package org.frankframework.monitoring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.monitoring.ITrigger.TriggerType;
import org.frankframework.monitoring.events.ConsoleMonitorEvent;
import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlBuilder;

public class TriggerTest implements EventThrowing {
	private static final String EVENT_CODE = "dummy code";
	private @Getter String eventSourceName = "TriggerTestClass";
	private @Getter Adapter adapter;

	private IMonitorDestination destination;
	private MonitorManager manager;
	private Monitor monitor;

	@BeforeEach
	public void setup() {
		destination = mock(IMonitorDestination.class);
		when(destination.getName()).thenReturn("dummy destination");
		when(destination.toXml()).thenReturn(new XmlBuilder("destination"));

		manager = spy(MonitorManager.class);
		manager.refresh();
		monitor = spy(SpringUtils.createBean(manager, Monitor.class));
		monitor.setApplicationContext(manager);

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

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);

		monitor.addTrigger(trigger);

		manager.configure();

		// Act
		FireMonitorEvent event = new FireMonitorEvent(this, EVENT_CODE);
		trigger.onApplicationEvent(event);

		// Assert
		verify(trigger, times(1)).configure();
		assertEquals(TriggerType.ALARM, trigger.getTriggerType());
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

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);
		trigger.setThreshold(5);
		trigger.setPeriod(1);

		monitor.addTrigger(trigger);

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

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.WARNING);

		monitor.addTrigger(trigger);
		monitor.setAlarmSeverity(Severity.WARNING);

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

		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);

		monitor.addTrigger(trigger);

		manager.configure();

		// Act
		MonitorEvent event = eventCausedByMonitor ? new ConsoleMonitorEvent("dummyUser") : new FireMonitorEvent(this, EVENT_CODE);

		monitor.changeState(TriggerType.ALARM, Severity.CRITICAL, event);

		// Assert
		verify(trigger, times(1)).configure();
		assertEquals(TriggerType.ALARM, trigger.getTriggerType());
		assertEquals(EventType.TECHNICAL, eventTypeCaptor.getValue());
		assertEquals(Severity.CRITICAL, severityCaptor.getValue());
		MonitorEvent capturedEvent = monitorEventCaptor.getValue();
		assertNotNull(capturedEvent);
		assertNull(capturedEvent.getEventMessage());
		assertTrue(monitor.isRaised());
		assertNotNull(monitor.getRaisedBy());
		assertEquals(0, monitor.getAdditionalHitCount());

		// Act
		monitor.changeState(TriggerType.CLEARING, Severity.WARNING, new ConsoleMonitorEvent("dummyUser")); // WARNING, cleared by MONITOR

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
		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);

		monitor.addTrigger(trigger);
		monitor.setAlarmSeverity(Severity.WARNING);

		// Act
		manager.configure();

		// Assert
		verify(trigger, times(1)).configure();
		assertEquals(TestFileUtils.getTestFile("/Management/Monitoring/getManagerToXML.xml"), manager.toXml().asXmlString());
	}

	@Test
	public void testTriggerConfigure() {
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Trigger trigger = new Trigger();

			ConfigurationException e1 = assertThrows(ConfigurationException.class, trigger::configure);
			assertEquals("no monitor autowired", e1.getMessage());

			trigger.setMonitor(monitor);
			trigger.setEventCode(EVENT_CODE);
			trigger.setThreshold(1);
			trigger.setPeriod(0);

			ConfigurationException e2 = assertThrows(ConfigurationException.class, trigger::configure);
			assertEquals("you must define a period when using threshold > 0", e2.getMessage());

			trigger.setPeriod(1);

			ConfigurationException e3 = assertThrows(ConfigurationException.class, trigger::configure);
			assertEquals("you must define a severity for the trigger", e3.getMessage());

			trigger.setSeverity(Severity.CRITICAL);

			assertDoesNotThrow(trigger::configure);

			assertThat(appender.getLogLines(), not(hasItem(containsString("should have at least one eventCode specified"))));

			trigger.setEventCodes(List.of());
			assertDoesNotThrow(trigger::configure);

			assertThat(appender.getLogLines(), hasItem(containsString("should have at least one eventCode specified")));
		}
	}
}
