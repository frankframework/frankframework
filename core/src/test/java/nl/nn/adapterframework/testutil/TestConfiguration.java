package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.webcontrol.api.MockIbisManager;

/**
 * Test Configuration utility
 */
public class TestConfiguration extends Configuration {
	public final static String TEST_CONFIGURATION_NAME = "test"; //See DeploymentSpecifics.properties
	private ClassPathXmlApplicationContext ac;

	//Configures a standalone configuration.
	public TestConfiguration() {
		ac = new ClassPathXmlApplicationContext();

		ac.setConfigLocation("testConfigurationContext.xml");
		setName(TEST_CONFIGURATION_NAME);
		ac.refresh();

		if(!TEST_CONFIGURATION_NAME.equals(AppConstants.getInstance().getProperty("instance.name"))) {
			fail("instance.name has been altered");
		}
	}

	public void autowireByType(Object bean) {
		autowire(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
	}

	public void autowireByName(Object bean) {
		autowire(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
	}

	private void autowire(Object bean, int autowireMode) {
		ac.getAutowireCapableBeanFactory().autowireBeanProperties(bean, autowireMode, false);
		ac.getAutowireCapableBeanFactory().initializeBean(bean, bean.getClass().getCanonicalName());
	}

	@SuppressWarnings("unchecked")
	public <T> T createBean(Class<T> beanClass) {
		return (T) ac.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	@Override
	public IbisManager getIbisManager() {
		if(super.getIbisManager() == null) {
			IbisManager ibisManager = new MockIbisManager();
			ibisManager.addConfiguration(this);
			IbisContext ibisContext = spy(new IbisContext());
			ibisContext.setApplicationContext(ac);
			ibisManager.setIbisContext(ibisContext);
			setIbisManager(ibisManager);
		}
		return super.getIbisManager();
	}
}
