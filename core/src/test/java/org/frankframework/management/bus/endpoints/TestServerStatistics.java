package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.management.bus.BusTestBase;

public class TestServerStatistics extends BusTestBase {
	/**
	 * Method: getFileSystemTotalSpace()
	 */
	@Test
	public void testGetFileSystemTotalSpace() {
		assertNotNull(ServerStatistics.getFileSystemTotalSpace());
	}

	/**
	 * Method: getFileSystemFreeSpace()
	 */
	@Test
	public void testGetFileSystemFreeSpace() {
		assertNotNull(ServerStatistics.getFileSystemFreeSpace());
	}
}
