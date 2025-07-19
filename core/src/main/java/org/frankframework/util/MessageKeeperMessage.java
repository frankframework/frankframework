/*
   Copyright 2013 Nationale-Nederlanden, 2017-2024 WeAreFrank!

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.lifecycle.events.MessageEvent;
import org.frankframework.logging.IbisMaskingLayout;

/**
 * A message for the MessageKeeper. <br/>
 * Although this could be an inner class of the MessageKeeper,
 * it's made "standalone" to provide the use of iterators and
 * enumerators with the MessageKeeper.
 * @author Johan Verrips IOS
 */
public class MessageKeeperMessage {
	private static final DateTimeFormatter MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a");

	private final Instant messageDate;
	private final String messageText;
	private final MessageKeeper.MessageKeeperLevel messageLevel;

	public static MessageKeeperMessage fromEvent(MessageEvent<?> event) {
		MessageKeeper.MessageKeeperLevel level = EnumUtils.parse(MessageKeeper.MessageKeeperLevel.class, event.getLevel().name());
		return new MessageKeeperMessage(event.getMessage(), Instant.ofEpochMilli(event.getTimestamp()), level);
	}

	/**
	* Set the message-text of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, MessageKeeper.MessageKeeperLevel level){
		this.messageText = maskMessage(message);
		this.messageDate = TimeProvider.now();
		this.messageLevel=level;
	}

	/**
	* Set the message-text and -date of this message. The text will be xml-encoded.
	*/
	public MessageKeeperMessage(String message, Instant date, MessageKeeper.MessageKeeperLevel level) {
		this.messageText = maskMessage(message);
		this.messageDate=date;
		this.messageLevel=level;
	}

	private String maskMessage(String message) {
		if (StringUtils.isNotEmpty(message)) {
			return IbisMaskingLayout.maskSensitiveInfo(message);
		}
		return message;
	}

	public Instant getMessageDate() {
		return messageDate;
	}
	public String getMessageText() {
		return messageText;
	}
	public String getMessageLevel() {
		return messageLevel!=null ? messageLevel.name() : null;
	}

	@Override
	public String toString() {
		// Ideally we'd use the end-user timezone, but we can't look across the browser. ;-)
		String date = MESSAGE_DATE_FORMATTER.format(messageDate.atZone(ZoneId.systemDefault()));
		return "%S: %s - %s".formatted(messageLevel, date, messageText);
	}
}
