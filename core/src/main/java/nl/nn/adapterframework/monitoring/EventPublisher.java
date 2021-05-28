package nl.nn.adapterframework.monitoring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import nl.nn.adapterframework.monitoring.events.FireMonitorEvent;
import nl.nn.adapterframework.monitoring.events.RegisterMonitorEvent;

public class EventPublisher implements ApplicationEventPublisherAware, EventHandler {
	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void registerEvent(EventThrowing source, String eventCode) {
		applicationEventPublisher.publishEvent(new RegisterMonitorEvent(source, eventCode));
	}

	@Override
	public void fireEvent(EventThrowing source, String eventCode) {
		applicationEventPublisher.publishEvent(new FireMonitorEvent(source, eventCode));
	}
}
