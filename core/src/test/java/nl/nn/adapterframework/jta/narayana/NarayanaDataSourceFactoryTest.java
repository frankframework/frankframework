package nl.nn.adapterframework.jta.narayana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactoryTest {

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	public void testSetup() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("transactionmanager.narayana.jdbc.connection.maxPoolSize", "2");

		// Act
		NarayanaDataSourceFactory factory = new NarayanaDataSourceFactory();

		// Assert
		assertEquals(2, factory.getMaxPoolSize());
	}
}
