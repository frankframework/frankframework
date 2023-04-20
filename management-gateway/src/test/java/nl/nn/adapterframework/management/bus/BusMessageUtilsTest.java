package nl.nn.adapterframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BusMessageUtilsTest {
	public JsonResponseMessage createMessage() {
		JsonResponseMessage response = new JsonResponseMessage("payload");
		response.setHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, "one");
		response.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, "two");
		response.setHeader("emptyHeader", "");
		response.setHeader("nullHeader", null);
		response.setHeader("intHeader", "123");
		response.setHeader("booleanHeader1", "true");
		response.setHeader("booleanHeader2", "false");
		response.setHeader("enumTopicHeader", "aDaPtEr");
		response.setHeader("enumActionHeader", "get");
		return response;
	}

	@Test
	public void containsHeaderTest() {
		assertAll(
			() -> assertTrue(BusMessageUtils.containsHeader(createMessage(), "adapter")),
			() -> assertTrue(BusMessageUtils.containsHeader(createMessage(), "configuration")),
			() -> assertTrue(BusMessageUtils.containsHeader(createMessage(), "emptyHeader")),
			() -> assertFalse(BusMessageUtils.containsHeader(createMessage(), "nullHeader"))
		);
	}

	@Test
	public void getHeaderTest() {
		assertAll(
			() -> assertEquals("one", BusMessageUtils.getHeader(createMessage(), "adapter")),
			() -> assertEquals("two", BusMessageUtils.getHeader(createMessage(), "configuration")),
			() -> assertEquals("", BusMessageUtils.getHeader(createMessage(), "emptyHeader")),
			() -> assertNull(BusMessageUtils.getHeader(createMessage(), "nullHeader")),
			() -> assertNull(BusMessageUtils.getHeader(createMessage(), "doesNotExist"))
		);
	}

	@Test
	public void getHeaderTestWithDefault() {
		assertAll(
			() -> assertEquals("one", BusMessageUtils.getHeader(createMessage(), "adapter", "dFault")),
			() -> assertEquals("two", BusMessageUtils.getHeader(createMessage(), "configuration", "dFault")),
			() -> assertEquals("dFault", BusMessageUtils.getHeader(createMessage(), "emptyHeader", "dFault")),
			() -> assertEquals("dFault", BusMessageUtils.getHeader(createMessage(), "nullHeader", "dFault")),
			() -> assertEquals("dFault", BusMessageUtils.getHeader(createMessage(), "doesNotExist", "dFault"))
		);
	}

	@Test
	public void getIntegerHeaderTest() {
		assertAll(
			() -> assertEquals(123, BusMessageUtils.getIntHeader(createMessage(), "intHeader", 456)),
			() -> assertThrows(NumberFormatException.class, ()->BusMessageUtils.getIntHeader(createMessage(), "emptyHeader", 456)),
			() -> assertNull(BusMessageUtils.getIntHeader(createMessage(), "nullHeader", 456)),
			() -> assertEquals(456, BusMessageUtils.getIntHeader(createMessage(), "doesNotExist", 456)),
			() -> assertNull(BusMessageUtils.getIntHeader(createMessage(), "doesNotExist", null))
		);
	}

	@Test
	public void getBooleanHeaderTest() {
		assertAll(
				() -> assertTrue(BusMessageUtils.getBooleanHeader(createMessage(), "booleanHeader1", true)),
				() -> assertFalse(BusMessageUtils.getBooleanHeader(createMessage(), "booleanHeader2", true)),
				() -> assertFalse(BusMessageUtils.getBooleanHeader(createMessage(), "emptyHeader", false)),
				() -> assertFalse(BusMessageUtils.getBooleanHeader(createMessage(), "emptyHeader", true)), // contains header, can't parse -> false
				() -> assertNull(BusMessageUtils.getBooleanHeader(createMessage(), "nullHeader", true)),
				() -> assertNull(BusMessageUtils.getBooleanHeader(createMessage(), "doesNotExist", null)),
				() -> assertTrue(BusMessageUtils.getBooleanHeader(createMessage(), "doesNotExist", true))
		);
	}

	@Test
	public void getEnumHeaderTest() {
		assertAll(
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(createMessage(), "enumTopicHeader", BusTopic.class)),
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(createMessage(), "enumTopicHeader", BusTopic.class, BusTopic.APPLICATION)),
				() -> assertEquals(BusTopic.ADAPTER, BusMessageUtils.getEnumHeader(createMessage(), "emptyHeader", BusTopic.class, BusTopic.ADAPTER)),
				() -> assertEquals(BusAction.DELETE, BusMessageUtils.getEnumHeader(createMessage(), "emptyHeader", BusAction.class, BusAction.DELETE)),
				() -> assertNull(BusMessageUtils.getEnumHeader(createMessage(), "nullHeader", BusAction.class)),
				() -> assertEquals(BusAction.DELETE, BusMessageUtils.getEnumHeader(createMessage(), "nullHeader", BusAction.class, BusAction.DELETE)),
				() -> assertNull(BusMessageUtils.getEnumHeader(createMessage(), "doesNotExist", BusAction.class)),
				() -> assertEquals(BusTopic.APPLICATION, BusMessageUtils.getEnumHeader(createMessage(), "doesNotExist", BusTopic.class, BusTopic.APPLICATION))
		);
	}
}
