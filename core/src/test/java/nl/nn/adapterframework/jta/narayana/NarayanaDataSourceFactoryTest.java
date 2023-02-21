package nl.nn.adapterframework.jta.narayana;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Test;

import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactoryTest {

	@AfterClass
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
