package nl.nn.adapterframework.monitoring.events;

import org.springframework.context.ApplicationEvent;

import nl.nn.adapterframework.monitoring.EventThrowing;

public class MonitorEvent extends ApplicationEvent {
	private String eventCode;

	public MonitorEvent(EventThrowing source, String eventCode) {
		super(source);
		this.eventCode = eventCode;
	}

	@Override
	public EventThrowing getSource() {
		return (EventThrowing) super.getSource();
	}

	public String getEventCode() {
		return eventCode;
	}
}
