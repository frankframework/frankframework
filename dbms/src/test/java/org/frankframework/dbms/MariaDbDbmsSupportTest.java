package org.frankframework.dbms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MariaDbDbmsSupportTest {

	@ParameterizedTest
	@CsvSource({"9.9", "5.5.5-10.4.26-MariaDB-log"})
	void testHasNoSkipLocked(String version) {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport(version);
		assertFalse(d.hasSkipLockedFunctionality());
	}

	@ParameterizedTest
	@CsvSource({"11.3.2", "11.3.2-MariaDB-1:11.3.2+maria~ubu2204", "10.6.0", "10.6.1", "5.5.5-10.6.5-MariaDB-1:10.6.5+maria~focal", "5.5.5-10.6.26-MariaDB-log"})
	void testHasSkipLocked(String version) {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport(version);
		assertTrue(d.hasSkipLockedFunctionality());
	}
}
