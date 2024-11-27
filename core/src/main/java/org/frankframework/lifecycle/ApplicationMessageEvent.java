/*
   Copyright 2021, 2022 WeAreFrank!

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

import java.time.Instant;

import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.MessageKeeperMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

public class ApplicationMessageEvent extends ApplicationContextEvent {
	private @Getter MessageKeeperMessage messageKeeperMessage;
	protected final Logger log = LogUtil.getLogger(ApplicationMessageEvent.class);

	protected ApplicationMessageEvent(ApplicationContext source) {
		super(source);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message) {
		this(source, message, MessageKeeperLevel.INFO);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level) {
		this(source, message, level, null);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, Exception e) {
		this(source, message, MessageKeeperLevel.ERROR, e);
	}

	public ApplicationMessageEvent(ApplicationContext source, String message, MessageKeeperLevel level, Exception e) {
		this(source);

		StringBuilder m = new StringBuilder();

		String applicationName = source.getId();
		m.append("Application [").append(applicationName).append("] ");

		String version = ConfigurationUtils.getApplicationVersion();
		if (version != null) {
			m.append("[").append(version).append("] ");
		}

		m.append(message);

		//We must use .toString() here else the StringBuilder will be passed on which add the stacktrace Message to the log
		if (MessageKeeperLevel.ERROR == level) {
			log.error(m.toString(), e);
		} else if (MessageKeeperLevel.WARN == level) {
			log.warn(m.toString(), e);
		} else {
			log.info(m.toString(), e);
		}

		if (e != null) {
			m.append(": (").append(ClassUtils.nameOf(e)).append(") ").append(e.getMessage());
		}

		messageKeeperMessage = new MessageKeeperMessage(m.toString(), Instant.ofEpochMilli(getTimestamp()), level);
	}
}
