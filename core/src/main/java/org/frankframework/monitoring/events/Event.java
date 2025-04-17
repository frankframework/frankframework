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
package org.frankframework.monitoring.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.frankframework.core.Adapter;
import org.frankframework.monitoring.EventThrowing;

/**
 * A Monitoring Event with a map of potential Adapters that can throw events of this type.
 */
public class Event {
	private final List<EventThrowing> throwers = new ArrayList<>();

	public Event() {}

	public Event(EventThrowing thrower) {
		throwers.add(thrower);
	}

	public void addThrower(EventThrowing thrower) {
		throwers.add(thrower);
	}

	/**
	 * Entities that can throw an Event
	 */
	public Map<String, List<String>> getSources() {
		Map<String, List<String>> sources = new HashMap<>();
		for(EventThrowing eventThrower : throwers) {
			Adapter adapter = eventThrower.getAdapter();
			List<String> sourceNames = sources.getOrDefault(adapter.getName(), new ArrayList<>());
			sourceNames.add(eventThrower.getEventSourceName());
			sources.put(adapter.getName(), sourceNames);
		}
		return Collections.unmodifiableMap(sources);
	}
}
