package org.frankframework.console.controllers.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.management.bus.BusTopic;

public class MessageCacheStoreTest {

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testGet(boolean isNull) {
		UUID uuid = isNull ? null : UUID.randomUUID();

		MessageCacheStore cache = new MessageCacheStore();
		String topic = BusTopic.ADAPTER.toString();
		cache.put(uuid, topic, "dummy");
		assertEquals("dummy", cache.get(uuid, topic));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testgetAndUpdate(boolean isNull) {
		UUID uuid = isNull ? null : UUID.randomUUID();

		MessageCacheStore cache = new MessageCacheStore();
		String topic = BusTopic.ADAPTER.toString();
		cache.put(uuid, topic, "old");
		assertEquals("old", cache.getAndUpdate(uuid, topic, "new"));
		assertEquals("new", cache.get(uuid, topic));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testGetNullValue(boolean isNull) {
		UUID uuid = isNull ? null : UUID.randomUUID();

		MessageCacheStore cache = new MessageCacheStore();
		assertNull(cache.get(uuid, BusTopic.ADAPTER.toString()));
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testUpdateNullValue(boolean isNull) {
		UUID uuid = isNull ? null : UUID.randomUUID();

		MessageCacheStore cache = new MessageCacheStore();
		assertEquals("{}", cache.getAndUpdate(uuid, BusTopic.ADAPTER.toString(), "new"));
	}
}
