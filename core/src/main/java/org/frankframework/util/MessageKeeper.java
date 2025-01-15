/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2024 WeAreFrank!

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
package org.frankframework.util;

import java.time.Instant;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.HasName;

/**
 * Keeps a list of <code>MessageKeeperMessage</code>s.
 * <br/>
 * @author  Johan Verrips IOS
 * @see MessageKeeperMessage
 */
@Log4j2
public class MessageKeeper extends SizeLimitedVector<MessageKeeperMessage> {

	public enum MessageKeeperLevel {
		INFO, WARN, ERROR
	}

	public MessageKeeper() {
		super();
	}

	public MessageKeeper(int maxSize) {
		super(maxSize);
	}

	public synchronized void add(String message) {
		add(message, MessageKeeperLevel.INFO);
	}
	public synchronized void add(String message, MessageKeeperLevel level) {
		super.add(new MessageKeeperMessage(message, level));
	}
	public synchronized void add(String message, Instant date) {
		add(message, date, MessageKeeperLevel.INFO);
	}
	public synchronized void add(String message, Instant date, MessageKeeperLevel level) {
		super.add(new MessageKeeperMessage(message, date, level));
	}

	/**
	 * Get a message by number
	 * @see MessageKeeperMessage
	 */
	public MessageKeeperMessage getMessage(int i) {
		return get(i);
	}

	/**
	 * Add an error message to the {@link #MessageKeeper} and log it as a warning
	 */
	public void add(String message, Throwable t) {
		String msgToLog = message;
		if(t.getMessage() != null) {
			msgToLog += ": "+t.getMessage();
		}
		add(msgToLog, MessageKeeperLevel.ERROR);
		log.warn(msgToLog, t);
	}

	public void info(String msg) {
		add(msg);
	}

	public void info(HasName namedObject, String msg) {
		info(ClassUtils.nameOf(namedObject) + ": " + msg);
	}

	public void warn(String msg) {
		add("WARNING: " + msg, MessageKeeperLevel.WARN);
	}

	public void warn(HasName namedObject, String msg) {
		warn(ClassUtils.nameOf(namedObject) + ": " + msg);
	}

	public void error(String msg) {
		add("ERROR: " + msg, MessageKeeperLevel.ERROR);
	}

	public void error(HasName namedObject, String msg) {
		error(ClassUtils.nameOf(namedObject) + ": " + msg);
	}
}
