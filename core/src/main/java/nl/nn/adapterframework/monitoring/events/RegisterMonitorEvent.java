package nl.nn.adapterframework.monitoring.events;

import nl.nn.adapterframework.monitoring.EventThrowing;

public class RegisterMonitorEvent extends MonitorEvent {

	public RegisterMonitorEvent(EventThrowing source, String eventCode) {
		super(source, eventCode);
	}

}
