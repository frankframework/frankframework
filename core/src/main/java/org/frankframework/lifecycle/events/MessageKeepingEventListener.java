/*
   Copyright 2025 WeAreFrank!

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

import org.springframework.context.ApplicationListener;

import lombok.Getter;

import org.frankframework.util.MessageKeeperMessage;
import org.frankframework.util.SizeLimitedVector;
public class MessageKeepingEventListener implements ApplicationListener<AdapterMessageEvent> {
	private static final int DEFAULT_MESSAGEKEEPER_SIZE = 25;

	private final @Getter SizeLimitedVector<MessageKeeperMessage> messages;

	public MessageKeepingEventListener() {
		this(DEFAULT_MESSAGEKEEPER_SIZE);
	}

	public MessageKeepingEventListener(int messageKeeperSize) {
		messages = new SizeLimitedVector<>(messageKeeperSize * 2);
	}

	@Override
	public void onApplicationEvent(AdapterMessageEvent event) {
		MessageKeeperMessage messageKeeperMessage = MessageKeeperMessage.fromEvent(event);
		messages.add(messageKeeperMessage);
	}
}
