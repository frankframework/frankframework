package org.frankframework.filesystem.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AmazonEncodingUtilsTest {

	@ParameterizedTest
	@CsvSource({
			"value1, value1",
			"nön-äscïï, =?UTF-8?B?bsO2bi3DpHNjw6/Drw==?=",
	})
	void testEncode(String value, String expected) {
		String result = AmazonEncodingUtils.encode(value);
		assertEquals(expected, result);
	}

	@ParameterizedTest
	@CsvSource({
			"value1, value1",
			"=?UTF-8?B?dsOkbMO8w6kyLeKCrC1ub24tYXNjaWk?=, välüé2-€-non-ascii",
			"=?UTF-8?B?bsO2bi3DpHNjw6/Drw==?=, nön-äscïï",
			"=?UTF-8?Q?v=C3=A4lue3-non-ascii?=, välue3-non-ascii",
	})
	void testDecode(String value, String expected) {
		String result = AmazonEncodingUtils.decode(value);
		assertEquals(expected, result);
	}
}
