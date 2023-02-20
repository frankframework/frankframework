package nl.nn.adapterframework.jdbc.dbms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class MariaDbDbmsSupportTest {

	@Test
	public void testHasSkipLockedLowerVersion() {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport("9.9");
		assertFalse(d.hasSkipLockedFunctionality());
	}

	@Test
	public void testHasSkipLockedEqualVersion() {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport("10.6.0");
		assertTrue(d.hasSkipLockedFunctionality());
	}

	@Test
	public void testHasSkipLockedHigherVersion() {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport("10.6.1");
		assertTrue(d.hasSkipLockedFunctionality());
	}

	@Test
	public void testHasSkipLockedServerVersion() {
		MariaDbDbmsSupport d = new MariaDbDbmsSupport("5.5.5-10.6.5-MariaDB-1:10.6.5+maria~focal");
		assertTrue(d.hasSkipLockedFunctionality());
	}
}
