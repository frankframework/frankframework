package nl.nn.adapterframework.monitoring.events;

import nl.nn.adapterframework.monitoring.EventThrowing;

public class FireMonitorEvent extends MonitorEvent {

	public FireMonitorEvent(EventThrowing source, String eventCode) {
		super(source, eventCode);
	}

}
