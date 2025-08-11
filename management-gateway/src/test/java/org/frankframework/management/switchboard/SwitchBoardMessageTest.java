package org.frankframework.management.switchboard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("SwitchBoardMessage: construction, headers, immutability, and Jackson behavior")
class SwitchBoardMessageTest {

	@Test
	@DisplayName("Returns provided payload and wraps headers with Spring system keys")
	void payloadAndHeadersIncludeProvidedEntriesAndSystemHeaders() {
		Map<String, Object> headers = Map.of("k1", "v1", "num", 42);
		SwitchBoardMessage<String> msg = new SwitchBoardMessage<>("hello", headers);

		assertEquals("hello", msg.getPayload(), "Payload should match");

		MessageHeaders mh = msg.getHeaders();
		assertEquals("v1", mh.get("k1"));
		assertEquals(42, mh.get("num"));
		assertTrue(mh.containsKey(MessageHeaders.ID), "Should contain MessageHeaders.ID");
		assertTrue(mh.containsKey(MessageHeaders.TIMESTAMP), "Should contain MessageHeaders.TIMESTAMP");
	}

	@Test
	@DisplayName("Null headers still yield system headers and correct payload")
	void nullHeadersStillYieldSystemHeadersOnly() {
		SwitchBoardMessage<byte[]> msg = new SwitchBoardMessage<>(new byte[]{ 1, 2, 3 }, null);
		MessageHeaders mh = msg.getHeaders();

		assertNotNull(mh);
		assertTrue(mh.containsKey(MessageHeaders.ID));
		assertTrue(mh.containsKey(MessageHeaders.TIMESTAMP));
		assertArrayEquals(new byte[]{ 1, 2, 3 }, msg.getPayload());
	}

	@Test
	@DisplayName("Headers are immutable")
	void headersAreUnmodifiable() {
		SwitchBoardMessage<String> msg = new SwitchBoardMessage<>("x", Map.of("a", "b"));
		MessageHeaders mh = msg.getHeaders();

		assertThrows(UnsupportedOperationException.class, () -> mh.put("c", "d"));
		assertThrows(UnsupportedOperationException.class, () -> mh.remove("a"));
	}

	@Test
	@DisplayName("Mutating the original map after construction does not affect stored headers")
	void mutatingOriginalMapAfterConstructionDoesNotAffectStoredHeaders() {
		Map<String, Object> original = new HashMap<>();
		original.put("k", "v");

		SwitchBoardMessage<String> msg = new SwitchBoardMessage<>("p", original);
		MessageHeaders snapshot = msg.getHeaders();

		original.put("k2", "v2");

		assertEquals("v", snapshot.get("k"));
		assertNull(snapshot.get("k2"));
	}

	@Test
	@DisplayName("Jackson round-trip (String payload) preserves payload and custom headers")
	void jacksonRoundTripWithStringPayload() throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		SwitchBoardMessage<String> original =
				new SwitchBoardMessage<>("payload", Map.of("h", "v"));

		String json = mapper.writeValueAsString(original);

		// Quick shape check
		JsonNode root = mapper.readTree(json);
		assertTrue(root.has("payload"));
		assertTrue(root.has("headers"));
		assertEquals("payload", root.get("payload").asText());
		assertEquals("v", root.get("headers").get("h").asText());
		assertTrue(root.get("headers").has(MessageHeaders.ID));
		assertTrue(root.get("headers").has(MessageHeaders.TIMESTAMP));

		// Round-trip
		SwitchBoardMessage<String> copy = mapper.readValue(
				json, new TypeReference<SwitchBoardMessage<String>>() {
				}
		);

		assertEquals("payload", copy.getPayload());
		assertEquals("v", copy.getHeaders().get("h"));
		assertTrue(copy.getHeaders().containsKey(MessageHeaders.ID));
		assertTrue(copy.getHeaders().containsKey(MessageHeaders.TIMESTAMP));
	}
}
