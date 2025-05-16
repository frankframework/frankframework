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

import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MessageCacheStore {
	private static final UUID LOCAL_UUID = UUID.randomUUID();

	private final WeakHashMap<UUID, HashMap<String, String>> memberCache = new WeakHashMap<>();

	public void put(@Nullable UUID uuid, @Nonnull String topic, @Nonnull String message) {
		getCache(uuid).put(topic, message);
	}

	@Nullable
	public String get(@Nullable UUID uuid, @Nonnull String topic) {
		return getCache(uuid).get(topic);
	}

	@Nonnull
	public String getAndUpdate(@Nullable UUID uuid, @Nonnull String topic, @Nonnull String latestJsonMessage) {
		String cachedMessage = getCache(uuid).put(topic, latestJsonMessage);
		if(cachedMessage == null) {
			return "{}";
		}
		return cachedMessage;
	}

	/* Used to reset the cache for testing */
	public void empty() {
		memberCache.clear();
	}

	@Nonnull
	private HashMap<String, String> getCache(@Nullable UUID uuid) {
		UUID key = uuid == null ? LOCAL_UUID : uuid;
		return memberCache.computeIfAbsent(key, t -> new HashMap<>());
	}
}
