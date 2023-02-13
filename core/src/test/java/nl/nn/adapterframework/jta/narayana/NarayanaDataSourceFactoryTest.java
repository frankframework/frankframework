package nl.nn.adapterframework.jta.narayana;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.util.AppConstants;

public class NarayanaDataSourceFactoryTest {

	@Test
	public void testSetup() {
		//arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("jdbc.connection.minPoolSize", "1");
		appConstants.setProperty("jdbc.connection.maxPoolSize", "2");
		appConstants.setProperty("jdbc.connection.maxIdleTime", "3");
		appConstants.setProperty("jdbc.connection.maxLifeTime", "4");

		// act
		NarayanaDataSourceFactory factory = new NarayanaDataSourceFactory();

		// assert
		assertEquals(2, factory.getMaxPoolSize());

	}
}
