package org.frankframework.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

/**
 * This test tests the Spring pub/sub (publishEvent and onApplicationEvent) methods.
 */
public class MonitorTest {
	private static final String EVENT_CODE = "I'm an error code";

	private TestConfiguration configuration;
	private MonitorManager manager;

	@BeforeEach
	public void setup() {
		configuration = new TestConfiguration(false, "testMonitoringContext.xml");
		configuration.refresh();
		manager = configuration.getBean("monitorManager", MonitorManager.class);
	}

	@AfterEach
	public void teardown() {
		configuration.close();
	}

	@Test
	public void testFireSpringEvent() throws Exception {
		Monitor monitor = SpringUtils.createBean(manager);
		monitor.setName("monitorName");

		Alarm trigger = SpringUtils.createBean(manager);
		trigger.addEventCodeText(EVENT_CODE);
		trigger.setSeverity(Severity.CRITICAL);
		monitor.addTrigger(trigger);

		manager.addMonitor(monitor);

		MonitorDestination destination = spy(configuration.createBean(MonitorDestination.class));
		destination.setName("myTestDestination");

		MessageCapturingEchoSender sender = spy(new MessageCapturingEchoSender());
		SpringUtils.autowireByType(manager, sender);
		verify(sender, times(1)).setConfiguration(configuration);

		destination.setSender(sender);
		manager.addDestination(destination);
		monitor.setDestinations(destination.getName());
		Message message = new Message("very important message");
		message.getContext().put("special-key", 123);

		configuration.configure();
		verify(destination, times(1)).configure();
		verify(sender, times(1)).configure();

		configuration.start();
		verify(destination, times(1)).start();
		verify(sender, times(1)).start();

		// Act
		configuration.publishEvent(EventThrowingClass.createMonitorEvent(message));

		// Assert
		Message capturedMessage = sender.getInputMessage();
		String result = "<event hostname=\"XXX\" monitor=\"monitorName\" source=\"TriggerTestClass\" type=\"TECHNICAL\" severity=\"CRITICAL\" event=\"I'm an error code\"/>";
		assertEquals(result, ignoreHostname(capturedMessage.asString()));
		PipeLineSession session = sender.getInputSession();
		assertTrue(session.containsKey(PipeLineSession.ORIGINAL_MESSAGE_KEY));
		Message originalMessage = (Message) session.get(PipeLineSession.ORIGINAL_MESSAGE_KEY);
		assertEquals(message.asString(), sender.getSessionOriginalMessageValue());
		assertEquals(123, originalMessage.getContext().get("special-key"));
	}

	private String ignoreHostname(String result) {
		String firstPart = result.substring(result.indexOf("hostname=")+10);
		String hostname = firstPart.substring(0, firstPart.indexOf("\" "));
		return result.replaceAll(hostname, "XXX");
	}

	private static class EventThrowingClass implements EventThrowing {

		private @Getter String name = "TriggerTestClass";
		private @Getter ApplicationContext applicationContext;

		private static FireMonitorEvent createMonitorEvent(Message message) {
			EventThrowingClass event = new EventThrowingClass();
			return new FireMonitorEvent(event, EVENT_CODE, message);
		}
	}
}
