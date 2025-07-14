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
package org.frankframework.lifecycle.events;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.MessageKeeperMessage;

@Log4j2
public abstract class MessageEvent<T extends ApplicationContext> extends ApplicationContextEvent {
	private static final long serialVersionUID = 1L;

	private final @Getter MessageKeeperMessage messageKeeperMessage;

	@Override
	@SuppressWarnings("unchecked")
	public T getSource() {
		return (T) super.getSource();
	}

	/**
	 * Mandatory message prefix, should contain information about the context {@code <T>}.
	 * 
	 * @return Type, name and or version.
	 */
	protected abstract String getMessagePrefix();

	/**
	 * Optional formatted error message. This is not logged, as it's part of the stacktrace.
	 * 
	 * @param e The exception that was thrown (if any)
	 * @return The formatted exception message.
	 */
	protected String getExceptionMessage(Exception e) {
		return ": " + e.getMessage();
	}

	protected MessageEvent(T source, String message, MessageKeeperLevel level, Exception e) {
		super(source);
		StringBuilder m = new StringBuilder();
		m.append(getMessagePrefix());
		m.append(message);

		// We must use .toString() here else the StringBuilder will be passed on which add the stacktrace Message to the log
		switch (level) {
			case INFO -> log.info(m.toString(), e);
			case WARN -> log.warn(m.toString(), e);
			case ERROR -> log.error(m.toString(), e);
		}

		if (e != null) {
			String exceptionMessage = getExceptionMessage(e);
			if (StringUtils.isNotEmpty(exceptionMessage)) {
				m.append(exceptionMessage);
			}
		}

		messageKeeperMessage = new MessageKeeperMessage(m.toString(), Instant.ofEpochMilli(getTimestamp()), level);
	}
}
