package nl.nn.adapterframework.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.webcontrol.api.MockIbisManager;

/**
 * Test Configuration utility
 */
public class TestConfiguration extends Configuration {
	public final static String TEST_CONFIGURATION_NAME = "TestConfiguration";

	//Configures a standalone configuration.
	public TestConfiguration() {
		super();
		setConfigLocation("testConfigurationContext.xml");
		setName(TEST_CONFIGURATION_NAME);
		refresh();
		configure();
		start();
	}

	@Test
	public void testTestConfiguration() {
		Configuration config = new TestConfiguration(); //Validate it can create/init
		assertTrue(config.isActive());
		assertEquals(TEST_CONFIGURATION_NAME, config.getId());
		config.close();
		assertFalse(config.isActive());
	}

	public void autowireByType(Object bean) {
		SpringUtils.autowireByType(this, bean);
		this.getAutowireCapableBeanFactory().initializeBean(bean, bean.getClass().getSimpleName());
	}

	public <T> T createBean(Class<T> beanClass) {
		return SpringUtils.createBean(this, beanClass);
	}

	/**
	 * Create and register the IbisManger with the Configuration
	 */
	@Override
	public IbisManager getIbisManager() {
		if(super.getIbisManager() == null) {
			MockIbisManager ibisManager = new MockIbisManager();
			ibisManager.addConfiguration(this);
			setIbisManager(ibisManager);
		}
		return super.getIbisManager();
	}
}
