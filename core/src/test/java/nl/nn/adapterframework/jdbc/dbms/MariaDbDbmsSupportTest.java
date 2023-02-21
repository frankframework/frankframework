package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MariaDbDbmsSupportTest {

	@ParameterizedTest
	@CsvSource({"9.9", "5.5.5-10.4.26-MariaDB-log"})
	public void testHasNoSkipLocked(String version) {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport(version);
		assertFalse(d.hasSkipLockedFunctionality());
	}

	@ParameterizedTest
	@CsvSource({"10.6.0", "10.6.1", "5.5.5-10.6.5-MariaDB-1:10.6.5+maria~focal", "5.5.5-10.6.26-MariaDB-log"})
	public void testHasSkipLocked(String version) {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport(version);
		assertTrue(d.hasSkipLockedFunctionality());
	}
}
