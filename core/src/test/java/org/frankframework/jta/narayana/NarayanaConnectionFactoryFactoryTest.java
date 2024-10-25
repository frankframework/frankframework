package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.frankframework.util.AppConstants;

public class NarayanaConnectionFactoryFactoryTest {

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	public void testSetup() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("transactionmanager.narayana.jms.connection.maxPoolSize", "1");
		appConstants.setProperty("transactionmanager.narayana.jms.connection.maxIdleTime", "2");
		appConstants.setProperty("transactionmanager.narayana.jms.connection.checkInterval", "3");
		appConstants.setProperty("transactionmanager.narayana.jms.connection.maxSessions", "4");

		// Act
		NarayanaConnectionFactoryFactory factory = new NarayanaConnectionFactoryFactory();

		// Assert
		assertEquals(1, factory.getMaxPoolSize());
		assertEquals(2, factory.getMaxIdleTime());
		assertEquals(3, factory.getConnectionCheckInterval());
		assertEquals(4, factory.getMaxSessions());

	}
}
