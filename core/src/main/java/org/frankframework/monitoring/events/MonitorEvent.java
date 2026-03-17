/*
   Copyright 2021-2023 WeAreFrank!

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

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

import org.frankframework.core.Adapter;
import org.frankframework.monitoring.EventThrowing;
import org.frankframework.stream.Message;

public class MonitorEvent extends ApplicationEvent {
	private final @Getter String eventCode;
	private final @Getter Message eventMessage;
	private transient String adapterName;

	public MonitorEvent(EventThrowing source, String eventCode, Message eventMessage) {
		super(source);
		this.eventCode = eventCode;
		this.eventMessage = eventMessage;
	}

	@Override
	public EventThrowing getSource() {
		return (EventThrowing) super.getSource();
	}

	public Instant getEventTime() {
		return Instant.ofEpochMilli(getTimestamp());
	}

	public String getEventSourceName() {
		return StringUtils.trimToNull(getSource().getName());
	}

	public String getAdapterName() {
		if (adapterName == null) {
			adapterName = findAdapterName(getSource().getApplicationContext());
		}

		return adapterName;
	}

	private static @Nullable String findAdapterName(@Nullable ApplicationContext source) {
		if (source == null) {
			return null;
		}
		if (source instanceof Adapter adapter) {
			return adapter.getName();
		}
		return findAdapterName(source.getParent());
	}
}
