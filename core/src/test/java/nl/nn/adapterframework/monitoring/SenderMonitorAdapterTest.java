package nl.nn.adapterframework.monitoring;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;

import lombok.Getter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.monitoring.events.MonitorEvent;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;

public class SenderMonitorAdapterTest implements EventThrowing {
	private @Getter Adapter adapter;
	private @Getter String eventSourceName = "MONITOR_DESTINATION_TEST";
	private static final String EVENTCODE = "MONITOR_EVENT_CODE";

	@Test
	public void testSenderMonitorAdapter() throws Exception {
		// Arrange
		SenderMonitorAdapter destination = new SenderMonitorAdapter();
		EchoSender sender = spy(EchoSender.class);
		ArgumentCaptor<Message> messageCapture = ArgumentCaptor.forClass(Message.class);
		destination.setSender(sender);
		destination.configure();

		when(sender.sendMessage(messageCapture.capture(), any(PipeLineSession.class))).thenCallRealMethod();
		MonitorEvent event = new MonitorEvent(this, EVENTCODE, null);

		// Act
		destination.fireEvent("monitor-name", EventType.FUNCTIONAL, Severity.WARNING, EVENTCODE, event);

		// Assert
		Message message = messageCapture.getValue();
		String result = "<event hostname=\"XXX\" monitor=\"monitor-name\" source=\"MONITOR_DESTINATION_TEST\" type=\"FUNCTIONAL\" severity=\"WARNING\" event=\"MONITOR_EVENT_CODE\"/>";
		assertEquals(result, ignoreHostname(message.asString()));
	}

	@Test
	public void testSenderMonitorAdapterWithMessage() throws Exception {
		// Arrange
		SenderMonitorAdapter destination = new SenderMonitorAdapter();
		MessageCapturingEchoSender sender = new MessageCapturingEchoSender();
		destination.setSender(sender);
		destination.configure();
		String eventText = "<ik>ben<xml/></ik>";

		MonitorEvent event = new MonitorEvent(this, EVENTCODE, new Message(eventText));

		// Act
		destination.fireEvent(null, EventType.FUNCTIONAL, Severity.WARNING, EVENTCODE, event);

		// Assert
		Message message = sender.getInputMessage();
		String result = "<event hostname=\"XXX\" source=\"MONITOR_DESTINATION_TEST\" type=\"FUNCTIONAL\" severity=\"WARNING\" event=\"MONITOR_EVENT_CODE\"/>";
		Assertions.assertEquals(result, ignoreHostname(message.asString()));
		PipeLineSession session = sender.getInputSession();
		Assertions.assertTrue(session.containsKey(PipeLineSession.originalMessageKey));
		Assertions.assertEquals(eventText, sender.getSessionOriginalMessageValue());
	}

	private String ignoreHostname(String result) {
		String firstPart = result.substring(result.indexOf("hostname=")+10);
		String hostname = firstPart.substring(0, firstPart.indexOf("\" "));
		return result.replaceAll(hostname, "XXX");
	}
}
