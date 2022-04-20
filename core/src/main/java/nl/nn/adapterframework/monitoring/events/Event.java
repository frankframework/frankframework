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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.monitoring.EventThrowing;

public class Event {
	private List<EventThrowing> throwers = new ArrayList<>();

	public Event() {}

	public Event(EventThrowing thrower) {
		throwers.add(thrower);
	}

	public void addThrower(EventThrowing thrower) {
		throwers.add(thrower);
	}

	private List<EventThrowing> getThrowers() {
		return Collections.unmodifiableList(throwers);
	}

	public List<String> getAdapters() {
		List<String> adapters = new ArrayList<>();
		for(EventThrowing eventThrower : getThrowers()) {
			Adapter adapter = eventThrower.getAdapter();
			if(adapter != null && !adapters.contains(adapter.getName())) {
				adapters.add(adapter.getName());
			}
		}
		return adapters;
	}

	public Map<String, List<String>> getSources() {
		Map<String, List<String>> sources = new HashMap<>();
		for(EventThrowing eventThrower : getThrowers()) {
			Adapter adapter = eventThrower.getAdapter();
			if(adapter != null && StringUtils.isNotEmpty(adapter.getName())) {
				List<String> sourceNames = sources.getOrDefault(adapter.getName(), new ArrayList<>());
				sourceNames.add(eventThrower.getEventSourceName());
				sources.put(adapter.getName(), sourceNames);
			}
		}
		return sources;
	}
}
