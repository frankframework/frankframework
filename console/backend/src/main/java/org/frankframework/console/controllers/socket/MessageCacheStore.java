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
package org.frankframework.console.controllers.socket;

import java.util.EnumMap;
import java.util.UUID;
import java.util.WeakHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.management.bus.BusTopic;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MessageCacheStore {

	private final WeakHashMap<String, EnumMap<BusTopic, String>> memberCache = new WeakHashMap<>();
//	private final EnumMap<BusTopic, String> topicCache = new EnumMap<>(BusTopic.class);

	public void put(@Nullable UUID uuid, @Nonnull BusTopic topic, @Nonnull String message) {
		String key = uuid == null ? "local" : uuid.toString();
		EnumMap<BusTopic, String> topicCache = memberCache.computeIfAbsent(key, t -> new EnumMap<>(BusTopic.class));
		topicCache.put(topic, message);
	}

	@Nullable
	public String get(@Nullable UUID uuid, @Nonnull BusTopic topic) {
		String key = uuid == null ? "local" : uuid.toString();
		EnumMap<BusTopic, String> topicCache = memberCache.computeIfAbsent(key, t -> new EnumMap<>(BusTopic.class));
		return topicCache.get(topic);
	}

	@Nonnull
	public String getAndUpdate(@Nullable UUID uuid, @Nonnull BusTopic topic, @Nonnull String latestJsonMessage) {
		String key = uuid == null ? "local" : uuid.toString();
		EnumMap<BusTopic, String> topicCache = memberCache.computeIfAbsent(key, t -> new EnumMap<>(BusTopic.class));
		String cachedMessage = topicCache.put(topic, latestJsonMessage);
		if(cachedMessage == null) {
			return "{}";
		}
		return cachedMessage;
	}
}
