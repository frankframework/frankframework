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
package org.frankframework.lifecycle;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationListener;

import org.frankframework.configuration.ConfigurationMessageEvent;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.MessageKeeper;

public class MessageEventListener implements ApplicationListener<ApplicationMessageEvent> {
	private static final int MESSAGEKEEPER_SIZE = 10;

	private final Map<String, MessageKeeper> messageKeepers = new HashMap<>();

	public MessageEventListener() {
		globalLog().setMaxSize(MESSAGEKEEPER_SIZE * 2);
	}

	/**
	 * Get MessageKeeper for the application. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @return MessageKeeper for the application
	 */
	public MessageKeeper getMessageKeeper() {
		return getMessageKeeper(BusMessageUtils.ALL_CONFIGS_KEY);
	}

	/**
	 * Get MessageKeeper for a specific configuration. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @param configurationName configuration name to get the MessageKeeper object from
	 * @return MessageKeeper for specified configurations
	 */
	public MessageKeeper getMessageKeeper(String configurationName) {
		return messageKeepers.get(configurationName);
	}

	private MessageKeeper globalLog() {
		return configLog(BusMessageUtils.ALL_CONFIGS_KEY);
	}

	private MessageKeeper configLog(String key) {
		MessageKeeper messageKeeper = messageKeepers.get(key);
		if (messageKeeper == null) {
			messageKeeper = new MessageKeeper(MESSAGEKEEPER_SIZE);
			messageKeepers.put(key, messageKeeper);
		}
		return messageKeeper;
	}

	@Override
	public void onApplicationEvent(ApplicationMessageEvent event) {
		if(event instanceof ConfigurationMessageEvent messageEvent) {
			String configurationName = messageEvent.getSource().getName();
			configLog(configurationName).add(event.getMessageKeeperMessage());
		}
		globalLog().add(event.getMessageKeeperMessage());
	}
}
