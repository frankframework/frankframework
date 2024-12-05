package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class HttpUtilsTest {

	@Test
	public void testDecodeBase64() {
		assertAll(
				() -> assertEquals("/adapter/dummy adapter/receiver/receiver!", HttpUtils.decodeBase64("L2FkYXB0ZXIvZHVtbXkgYWRhcHRlci9yZWNlaXZlci9yZWNlaXZlciE=")),
				() -> assertEquals("/adapter/dummy ædåpter", HttpUtils.decodeBase64("L2FkYXB0ZXIvZHVtbXkgw6Zkw6VwdGVy")),
				() -> assertThrows(IllegalArgumentException.class, () -> HttpUtils.decodeBase64("??"))
		);
	}
}
