package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.frankframework.util.AppConstants;

public class NarayanaDataSourceFactoryTest {

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	public void testSetup() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("transactionmanager.jdbc.connection.minIdle", "10");
		appConstants.setProperty("transactionmanager.jdbc.connection.maxPoolSize", "20");
		appConstants.setProperty("transactionmanager.jdbc.connection.maxIdle", "30");
		appConstants.setProperty("transactionmanager.jdbc.connection.maxLifeTime", "40");
		appConstants.setProperty("transactionmanager.narayana.jdbc.connection.minPoolSize", "1");
		appConstants.setProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize", "2");
		appConstants.setProperty("transactionmanager.narayana.jdbc.connection.maxIdle", "3");
		appConstants.setProperty("transactionmanager.narayana.jdbc.connection.maxLifeTime", "4");

		// Act
		NarayanaDataSourceFactory factory = new NarayanaDataSourceFactory();

		// Assert
		assertEquals(1, factory.getMinIdle());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdle());
		assertEquals(4, factory.getMaxLifeTime());
	}
}
