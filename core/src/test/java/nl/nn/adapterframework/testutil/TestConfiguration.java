package nl.nn.adapterframework.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
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

		if(!TEST_CONFIGURATION_NAME.equals(AppConstants.getInstance().getProperty("instance.name"))) {
			fail("instance.name has been altered");
		}
	}

	@Override
	protected DefaultListableBeanFactory createBeanFactory() {
		return Mockito.spy(new DefaultListableBeanFactory());
	}

	public <T> void mockCreateBean(Class<T> originalBean, Class<? extends T> mockedBean) {
		T mock = getBean(mockedBean);
		assertNotNull("mock ["+mockedBean+"] not found", mock);
		mockCreateBean(originalBean, mock);
	}
	public <T> void mockCreateBean(Class<T> originalBean, T mock) {
		assertNotNull("mock may not be null", mock);
		AutowireCapableBeanFactory beanFactory = getAutowireCapableBeanFactory();
		Mockito.doReturn(mock).when(beanFactory).createBean(Mockito.eq(originalBean), Mockito.anyInt(), Mockito.anyBoolean());

		T t = SpringUtils.createBean(this, originalBean); //Test the mock
		assertNotNull(t);
		if(t.getClass().isInstance(originalBean)) {
			fail("Unable to mock bean ["+originalBean+"] got ["+t.getClass().getName()+"] instead");
		}
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
	}

	public void autowireByName(Object bean) {
		SpringUtils.autowireByName(this, bean);
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
			IbisManager ibisManager = new MockIbisManager();
			ibisManager.addConfiguration(this);
			getBeanFactory().registerSingleton("ibisManager", ibisManager);
			setIbisManager(ibisManager);

			assertTrue("bean IbisManager not found", containsBean("ibisManager"));
		}
		return super.getIbisManager();
	}
}
