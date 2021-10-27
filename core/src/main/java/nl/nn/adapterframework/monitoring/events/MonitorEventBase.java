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
package nl.nn.adapterframework.monitoring.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import nl.nn.adapterframework.monitoring.EventThrowing;

/**
 * Base class for registering and throwing {@link MonitorEvent MonitorEvents}.
 * 
 * @author Niels Meijer
 */
public abstract class MonitorEventBase extends ApplicationEvent {

	private @Getter MonitorEvent monitorEvent;

	public MonitorEventBase(EventThrowing source, MonitorEvent event) {
		super(source);
		this.monitorEvent = event;
	}

	@Override
	public EventThrowing getSource() {
		return (EventThrowing) super.getSource();
	}
}
