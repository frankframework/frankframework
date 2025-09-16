package org.frankframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class NarayanaDataSourceFactoryTest {

	@AfterAll
	public static void tearDown() {
		System.clearProperty("transactionmanager.jdbc.connection.minIdle");
		System.clearProperty("transactionmanager.jdbc.connection.maxPoolSize");
		System.clearProperty("transactionmanager.jdbc.connection.maxIdle");
		System.clearProperty("transactionmanager.jdbc.connection.maxLifeTime");
		System.clearProperty("transactionmanager.narayana.jdbc.connection.minPoolSize");
		System.clearProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize");
		System.clearProperty("transactionmanager.narayana.jdbc.connection.maxIdle");
		System.clearProperty("transactionmanager.narayana.jdbc.connection.maxLifeTime");
	}

	@Test
	public void testSetup() {
		// Arrange
		System.setProperty("transactionmanager.jdbc.connection.minIdle", "10");
		System.setProperty("transactionmanager.jdbc.connection.maxPoolSize", "20");
		System.setProperty("transactionmanager.jdbc.connection.maxIdle", "30");
		System.setProperty("transactionmanager.jdbc.connection.maxLifeTime", "40");
		System.setProperty("transactionmanager.narayana.jdbc.connection.minPoolSize", "1");
		System.setProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize", "2");
		System.setProperty("transactionmanager.narayana.jdbc.connection.maxIdle", "3");
		System.setProperty("transactionmanager.narayana.jdbc.connection.maxLifeTime", "4");

		// Act
		NarayanaDataSourceFactory factory = new NarayanaDataSourceFactory();

		// Assert
		assertEquals(1, factory.getMinIdle());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdle());
		assertEquals(4, factory.getMaxLifeTime());
	}
}
