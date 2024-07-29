/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.web.socket;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MessageCacheStore {

	private final Map<String, MessageCache> messages = new HashMap<>();

	public Message<?> getCachedMessage(String name) {
		MessageCache cache = messages.get(name);
		if (cache != null) {
			return cache.message();
		}

		return null;
	}

	public void putMessage(String name, Message<?> message) {
		this.messages.put(name, new MessageCache(name, message));
	}

	protected record MessageCache(String name, Message<?> message) {}

}
