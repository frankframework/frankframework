package nl.nn.adapterframework.monitoring;

import org.springframework.context.ApplicationEvent;

import nl.nn.adapterframework.core.INamedObject;

public class MonitorEvent extends ApplicationEvent {
	public String eventCode;

	public MonitorEvent(INamedObject source, String eventCode) {
		super(source);
		this.eventCode = eventCode;
	}
}
