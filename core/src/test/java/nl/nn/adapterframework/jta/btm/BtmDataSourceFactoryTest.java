package nl.nn.adapterframework.jta.btm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.util.AppConstants;


public class BtmDataSourceFactoryTest {

	@Test
	public void testSetup() {
		//arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("jdbc.connection.minPoolSize", "1");
		appConstants.setProperty("jdbc.connection.maxPoolSize", "2");
		appConstants.setProperty("jdbc.connection.maxIdleTime", "3");
		appConstants.setProperty("jdbc.connection.maxLifeTime", "4");

		// act
		BtmDataSourceFactory factory = new BtmDataSourceFactory();

		// assert
		assertEquals(1, factory.getMinPoolSize());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdleTime());
		assertEquals(4, factory.getMaxLifeTime());

	}
}
