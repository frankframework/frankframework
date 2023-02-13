package nl.nn.adapterframework.jta.btm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.util.AppConstants;

public class BtmConnectionFactoryFactoryTest {

	@Test
	public void testSetup() {
		//arrange
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty("jms.connection.minPoolSize", "1");
		appConstants.setProperty("jms.connection.maxPoolSize", "2");
		appConstants.setProperty("jms.connection.maxIdleTime", "3");
		appConstants.setProperty("jms.connection.maxLifeTime", "4");

		// act
		BtmConnectionFactoryFactory factory = new BtmConnectionFactoryFactory();

		// assert
		assertEquals(1, factory.getMinPoolSize());
		assertEquals(2, factory.getMaxPoolSize());
		assertEquals(3, factory.getMaxIdleTime());
		assertEquals(4, factory.getMaxLifeTime());

	}
}
