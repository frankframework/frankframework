package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.frankframework.management.bus.message.JsonMessage;

public class BusMessageUtilsTest {
	private static JsonMessage TEST_MESSAGE;

	@BeforeAll
	public static void setup() {
		JsonMessage response = new JsonMessage("payload");
		response.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, "one");
		response.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, "two");
		response.setHeader("emptyHeader", "");
		response.setHeader("nullHeader", null);
		response.setHeader("intHeader", "123");
		response.setHeader("booleanHeader1", "true");
		response.setHeader("booleanHeader2", "false");
		response.setHeader("enumTopicHeader", "aDaPtEr");
		response.setHeader("enumActionHeader", "get");
		TEST_MESSAGE = response;
	}

	@Test
	public void containsHeaderTest() {
		assertAll(
				() -> assertTrue(BusMessageUtils.containsHeader(TEST_MESSAGE, "adapter")),
				() -> assertTrue(BusMessageUtils.containsHeader(TEST_MESSAGE, "configuration")),
				() -> assertTrue(BusMessageUtils.containsHeader(TEST_MESSAGE, "emptyHeader")),
				() -> assertFalse(BusMessageUtils.containsHeader(TEST_MESSAGE, "nullHeader"))
		);
	}

	@Test
	public void getHeaderTest() {
		assertAll(
				() -> assertEquals("one", BusMessageUtils.getHeader(TEST_MESSAGE, "adapter")),
				() -> assertEquals("two", BusMessageUtils.getHeader(TEST_MESSAGE, "configuration")),
				() -> assertEquals("", BusMessageUtils.getHeader(TEST_MESSAGE, "emptyHeader")),
				() -> assertNull(BusMessageUtils.getHeader(TEST_MESSAGE, "nullHeader")),
				() -> assertNull(BusMessageUtils.getHeader(TEST_MESSAGE, "doesNotExist"))
		);
	}

	@Test
	public void getHeaderTestWithDefault() {
		assertAll(
				() -> assertEquals("one", BusMessageUtils.getHeader(TEST_MESSAGE, "adapter", "dFault")),
				() -> assertEquals("two", BusMessageUtils.getHeader(TEST_MESSAGE, "configuration", "dFault")),
				() -> assertEquals("dFault", BusMessageUtils.getHeader(TEST_MESSAGE, "emptyHeader", "dFault")),
				() -> assertEquals("dFault", BusMessageUtils.getHeader(TEST_MESSAGE, "nullHeader", "dFault")),
				() -> assertEquals("dFault", BusMessageUtils.getHeader(TEST_MESSAGE, "doesNotExist", "dFault"))
		);
	}

	@Test
	public void getIntegerHeaderTest() {
		assertAll(
				() -> assertEquals(123, BusMessageUtils.getIntHeader(TEST_MESSAGE, "intHeader", 456)),
				() -> assertEquals(456, BusMessageUtils.getIntHeader(TEST_MESSAGE, "emptyHeader", 456)),
				() -> assertEquals(456, BusMessageUtils.getIntHeader(TEST_MESSAGE, "nullHeader", 456)),
				() -> assertEquals(456, BusMessageUtils.getIntHeader(TEST_MESSAGE, "doesNotExist", 456)),
				() -> assertNull(BusMessageUtils.getIntHeader(TEST_MESSAGE, "doesNotExist", null))
		);
	}

	@Test
	public void getBooleanHeaderTest() {
		assertAll(
				() -> assertTrue(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "booleanHeader1", true)),
				() -> assertFalse(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "booleanHeader2", true)),
				() -> assertFalse(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "emptyHeader", false)),
				() -> assertTrue(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "emptyHeader", true)), // contains header, can't parse -> uses default
				() -> assertTrue(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "nullHeader", true)),
				() -> assertNull(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "doesNotExist", null)),
				() -> assertTrue(BusMessageUtils.getBooleanHeader(TEST_MESSAGE, "doesNotExist", true))
		);
	}

	@Test
	public void getEnumHeaderTest() {
		assertAll(
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "enumTopicHeader", BusTopic.class)),
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "enumTopicHeader", BusTopic.class, BusTopic.APPLICATION)),
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "emptyHeader", BusTopic.class, BusTopic.ADAPTER)),
				() -> assertEquals(BusAction.DELETE, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "emptyHeader", BusAction.class, BusAction.DELETE)),
				() -> assertNull(BusMessageUtils.getEnumHeader(TEST_MESSAGE, "nullHeader", BusAction.class)),
				() -> assertEquals(BusAction.DELETE, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "nullHeader", BusAction.class, BusAction.DELETE)),
				() -> assertNull(BusMessageUtils.getEnumHeader(TEST_MESSAGE, "doesNotExist", BusAction.class)),
				() -> assertEquals(BusTopic.APPLICATION, BusMessageUtils.getEnumHeader(TEST_MESSAGE, "doesNotExist", BusTopic.class, BusTopic.APPLICATION))
		);
	}
}
