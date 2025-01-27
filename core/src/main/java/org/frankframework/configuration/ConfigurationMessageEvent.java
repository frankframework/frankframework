/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.configuration;

import java.time.Instant;

import org.springframework.context.ApplicationContext;

import lombok.Getter;

import org.frankframework.lifecycle.ApplicationMessageEvent;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.MessageKeeperMessage;

public class ConfigurationMessageEvent extends ApplicationMessageEvent {
	private final @Getter MessageKeeperMessage messageKeeperMessage;

	public ConfigurationMessageEvent(Configuration source, String message) {
		this(source, message, MessageKeeperLevel.INFO);
	}

	public ConfigurationMessageEvent(Configuration source, String message, MessageKeeperLevel level) {
		this(source, message, level, null);
	}

	public ConfigurationMessageEvent(Configuration source, String message, Exception e) {
		this(source, message, MessageKeeperLevel.ERROR, e);
	}

	@Override
	public Configuration getSource() {
		return (Configuration) super.getSource();
	}

	private ConfigurationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level, Exception e) {
		super(source);
		StringBuilder m = new StringBuilder();

		String configurationName = getSource().getName();
		m.append("Configuration [").append(configurationName).append("] ");

		String version = getSource().getVersion();
		if (version != null) {
			m.append("[").append(version).append("] ");
		}

		m.append(message);

		// We must use .toString() here else the StringBuilder will be passed on which add the stacktrace Message to the log
		if (MessageKeeperLevel.ERROR == level) {
			log.error(m.toString(), e);
		} else if (MessageKeeperLevel.WARN == level) {
			log.warn(m.toString(), e);
		} else {
			log.info(m.toString(), e);
		}

		if (e != null) {
			m.append(": ").append(e.getMessage());
		}

		messageKeeperMessage = new MessageKeeperMessage(m.toString(), Instant.ofEpochMilli(getTimestamp()), level);
	}
}
