package nl.nn.adapterframework.jta.btm;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.util.AppConstants;


public class BtmDataSourceFactoryTest {

	@AfterAll
	public static void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	public void testSetup() {
		// Arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("transactionmanager.btm.jdbc.connection.minPoolSize", "1");
		appConstants.setProperty("transactionmanager.btm.jdbc.connection.maxPoolSize", "2");
		appConstants.setProperty("transactionmanager.btm.jdbc.connection.maxIdleTime", "3");
		appConstants.setProperty("transactionmanager.btm.jdbc.connection.maxLifeTime", "4");

		// Act
		BtmDataSourceFactory factory = new BtmDataSourceFactory();

		// Assert
		assertEquals(1, factory.getMinPoolSize());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdleTime());
		assertEquals(4, factory.getMaxLifeTime());
	}
}
