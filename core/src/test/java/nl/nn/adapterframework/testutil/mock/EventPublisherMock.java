package nl.nn.adapterframework.testutil.mock;

import nl.nn.adapterframework.monitoring.EventPublisher;
import nl.nn.adapterframework.monitoring.EventThrowing;

public class EventPublisherMock extends EventPublisher {

	@Override
	public void registerEvent(EventThrowing source, String eventCode) {
		//Do nothing
	}
}
