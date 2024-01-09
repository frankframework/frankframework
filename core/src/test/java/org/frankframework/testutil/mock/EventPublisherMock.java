package org.frankframework.testutil.mock;

import org.frankframework.monitoring.EventPublisher;
import org.frankframework.monitoring.EventThrowing;

public class EventPublisherMock extends EventPublisher {

	@Override
	public void registerEvent(EventThrowing source, String eventCode) {
		//Do nothing
	}
}
