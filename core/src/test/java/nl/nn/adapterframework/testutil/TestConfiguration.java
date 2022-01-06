package nl.nn.adapterframework.testutil;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.lifecycle.MessageEventListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Test Configuration utility
 * 
 * @author Niels Meijer
 */
public class TestConfiguration extends Configuration {
	public final static String TEST_CONFIGURATION_NAME = "TestConfiguration";
	private QuerySenderPostProcessor qsPostProcessor = new QuerySenderPostProcessor();

	//Configures a standalone configuration.
	public TestConfiguration() {
		this(new JunitTestClassLoaderWrapper());
	}

	/**
	 * When the beanfactory is mocked it holds bean references!
	 */
	public TestConfiguration(ClassLoader classLoader) {
		super();

		setClassLoader(classLoader); //Add the test classpath
		setConfigLocation("testConfigurationContext.xml");
		setName(TEST_CONFIGURATION_NAME);

		refresh();

		//Add Custom Pre-Instantiation Processor to mock statically created FixedQuerySenders.
		qsPostProcessor.setApplicationContext(this);
		getBeanFactory().addBeanPostProcessor(qsPostProcessor);

		try {
			configure();
		} catch (ConfigurationException e) {
			throw new IllegalStateException("unable to configure configuration", e);
		}

		if(!TEST_CONFIGURATION_NAME.equals(AppConstants.getInstance().getProperty("instance.name"))) {
			fail("instance.name has been altered");
		}
	}

	@Override
	protected void runMigrator() {
		//Suppress migrator instantiation during test phase
	}

	public String getConfigWarning(int index) {
		return getConfigurationWarnings().getWarnings().get(index);
	}

	/**
	 * Add the ability to mock FixedQuerySender ResultSets. Enter the initial query and a mocked 
	 * ResultSet using a {@link FixedQuerySenderMock.ResultSetBuilder ResultSetBuilder}.
	 */
	public void mockQuery(String query, ResultSet resultSet) {
		qsPostProcessor.addMock(query, resultSet);
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

	public MessageKeeper getMessageKeeper() {
		MessageEventListener mel = getBean("MessageEventListener", MessageEventListener.class);
		return mel.getMessageKeeper(TestConfiguration.TEST_CONFIGURATION_NAME);
	}

	public MessageKeeper getGlobalMessageKeeper() {
		MessageEventListener mel = getBean("MessageEventListener", MessageEventListener.class);
		return mel.getMessageKeeper();
	}
}
