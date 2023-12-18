package org.frankframework.jta.btm;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.frankframework.jta.btm.BtmConnectionFactoryFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.frankframework.util.AppConstants;

public class BtmConnectionFactoryFactoryTest {

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	void testSetup() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("transactionmanager.btm.jms.connection.minPoolSize", "1");
		appConstants.setProperty("transactionmanager.btm.jms.connection.maxPoolSize", "2");
		appConstants.setProperty("transactionmanager.btm.jms.connection.maxIdleTime", "3");
		appConstants.setProperty("transactionmanager.btm.jms.connection.maxLifeTime", "4");

		// Act
		BtmConnectionFactoryFactory factory = new BtmConnectionFactoryFactory();

		// Assert
		assertEquals(1, factory.getMinPoolSize());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdleTime());
		assertEquals(4, factory.getMaxLifeTime());
	}
}
