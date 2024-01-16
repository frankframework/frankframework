/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.monitoring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.monitoring.events.RegisterMonitorEvent;
import org.frankframework.stream.Message;

/**
 * Publisher to wrap the monitoring events and publish them to the Spring Context
 *
 * @author Niels Meijer
 *
 */
public class EventPublisher implements ApplicationEventPublisherAware {
	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void registerEvent(EventThrowing source, String eventCode) {
		applicationEventPublisher.publishEvent(new RegisterMonitorEvent(source, eventCode));
	}

	public void fireEvent(EventThrowing source, String eventCode) {
		applicationEventPublisher.publishEvent(new FireMonitorEvent(source, eventCode));
	}

	public void fireEvent(EventThrowing source, String eventCode, Message eventMessage) {
		applicationEventPublisher.publishEvent(new FireMonitorEvent(source, eventCode, eventMessage));
	}
}
