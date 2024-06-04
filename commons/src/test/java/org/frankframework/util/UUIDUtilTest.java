package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class UUIDUtilTest {

	/**
	 * Method: createSimpleUUID()
	 */
	@Test
	public void testCreateSimpleUUID() {
		String uuid = UUIDUtil.createSimpleUUID();
		assertEquals("-", uuid.substring(8, 9));
		assertFalse(uuid.isEmpty());
	}

	/**
	 * Method: createRandomUUID(boolean removeDashes)
	 */
	@Test
	public void testCreateRandomUUIDRemoveDashes() {
		String uuid = UUIDUtil.createRandomUUID(true);
		assertNotEquals("-", uuid.substring(8, 9)); // assert that dashes are removed
		assertEquals(32, uuid.length());
	}

	@Test
	public void testCreateRandomUUID() {
		String uuid = UUIDUtil.createRandomUUID();
		assertFalse(uuid.isEmpty());
	}

	/**
	 * Method: asHex(byte[] buf)
	 */
	@Test
	public void testAsHex() {
		String test = "test";
		String hex = UUIDUtil.asHex(test.getBytes());
		assertEquals("74657374", hex);
	}

	/**
	 * Method: createNumericUUID()
	 */
	@Test
	public void testCreateNumericUUID() {
		String uuid = UUIDUtil.createNumericUUID();
		assertEquals(31, uuid.length()); // Unique string is <ipaddress with length 4*3><currentTime with length
		// 13><hashcode with length 6>
	}

	/**
	 * Method: unsignedByteToInt(byte b)
	 */
	@Test
	public void testUnsignedByteToInt() {
		assertEquals(244, UUIDUtil.unsignedByteToInt(Byte.valueOf("-12")));
		assertEquals(12, UUIDUtil.unsignedByteToInt(Byte.valueOf("12")));

	}
}
