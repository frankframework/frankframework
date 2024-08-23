package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.ResultSet;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.lifecycle.MessageEventListener;
import org.frankframework.testutil.mock.MockIbisManager;
import org.frankframework.util.AppConstants;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.SpringUtils;
import org.springframework.beans.BeansException;

/**
 * Test Configuration utility
 *
 * @author Niels Meijer
 */
public class TestConfiguration extends Configuration {
	public static final String TEST_CONFIGURATION_NAME = "TestConfiguration";
	public static final String TEST_CONFIGURATION_FILE = "testConfigurationContext.xml";
	public static final String TEST_DATABASE_ENABLED_CONFIGURATION_FILE = "testDatabaseEnabledConfigurationContext.xml";
	private final QuerySenderPostProcessor qsPostProcessor = new QuerySenderPostProcessor();
	private final boolean autoConfigure;

	//Configures a standalone configuration.
	public TestConfiguration() {
		this(true);
	}

	public TestConfiguration(boolean autoConfigure) {
		this(autoConfigure, TEST_CONFIGURATION_FILE);
		refresh();
	}

	public TestConfiguration(String... configurationFiles) {
		this(true, configurationFiles);
	}

	public TestConfiguration(boolean autoConfigure, String... configurationFiles) {
		super();
		setAutoStart(false);
		this.autoConfigure = autoConfigure;

		ClassLoader classLoader = new JunitTestClassLoaderWrapper(); //Add ability to retrieve classes from src/test/resources
		setClassLoader(classLoader); //Add the test classpath
		setConfigLocations(configurationFiles);
		setName(TEST_CONFIGURATION_NAME);
	}

	@Override
	public void refresh() throws BeansException, IllegalStateException {
		super.refresh();

		//Add Custom Pre-Instantiation Processor to mock statically created FixedQuerySenders.
		qsPostProcessor.setApplicationContext(this);
		getBeanFactory().addBeanPostProcessor(qsPostProcessor);

		if (autoConfigure) {
			try {
				configure();
			} catch (ConfigurationException e) {
				throw new IllegalStateException("unable to configure configuration", e);
			}
		}

		if(!TEST_CONFIGURATION_NAME.equals(AppConstants.getInstance().getProperty("instance.name"))) {
			fail("instance.name has been altered");
		}
	}

	public String getConfigWarning(int index) {
		return getConfigurationWarnings().getWarnings().get(index);
	}

	/**
	 * Add the ability to mock FixedQuerySender ResultSets. Enter the initial query and a mocked
	 * ResultSet using a {@link org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder ResultSetBuilder}.
	 */
	public void mockQuery(String query, ResultSet resultSet) {
		qsPostProcessor.addFixedQuerySenderMock(query, resultSet);
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
	public synchronized IbisManager getIbisManager() {
		if(super.getIbisManager() == null) {
			IbisManager ibisManager = new MockIbisManager();
			ibisManager.addConfiguration(this);
			getBeanFactory().registerSingleton("ibisManager", ibisManager);
			setIbisManager(ibisManager);

			assertTrue(containsBean("ibisManager"), "bean IbisManager not found");
		}
		return super.getIbisManager();
	}

	public MessageKeeper getMessageKeeper() {
		MessageEventListener mel = getBean("MessageEventListener", MessageEventListener.class);
		return mel.getMessageKeeper(getName());
	}

	public MessageKeeper getGlobalMessageKeeper() {
		MessageEventListener mel = getBean("MessageEventListener", MessageEventListener.class);
		return mel.getMessageKeeper();
	}
}
