package nl.nn.adapterframework.monitoring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import nl.nn.adapterframework.core.INamedObject;

public class EventPublisher implements ApplicationEventPublisherAware {
	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void fireEvent(INamedObject source, String eventCode) {
		MonitorEvent event = new MonitorEvent(source, eventCode);
		applicationEventPublisher.publishEvent(event);
	}
}
