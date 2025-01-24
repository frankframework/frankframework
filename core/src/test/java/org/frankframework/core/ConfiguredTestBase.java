package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

public abstract class ConfiguredTestBase {
	protected Logger log = LogUtil.getLogger(this);

	public static final String testMessageId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
	public static final String testCorrelationId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";

	protected PipeLineSession session;

	protected PipeLine pipeline;
	protected Adapter adapter;
	private static TestConfiguration configuration;

	protected TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}

	@BeforeEach
	public void setUp() throws Exception {
		adapter = getConfiguration().createBean(Adapter.class);
		pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		adapter.setName("TestAdapter of "+getClass().getSimpleName());
		adapter.setPipeLine(pipeline);
		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown() {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		CloseUtils.closeSilently(adapter, session);
		pipeline = null;
		adapter = null;
		session = null;
	}

	protected void autowireByType(Object bean) {
		getConfiguration().autowireByType(bean);
	}

	/**
	 * Performs full initialization of the bean, including all applicable BeanPostProcessors. This is effectively a superset of what autowire provides, adding initializeBean behavior.
	 */
	public <T> T createBeanInAdapter(Class<T> beanClass) {
		assertNotNull(adapter, "Adapter does not exist");
		return SpringUtils.createBean(adapter, beanClass);
	}

	protected ConfigurationWarnings getConfigurationWarnings() {
		return getConfiguration().getConfigurationWarnings();
	}

	protected void configurePipeline() throws ConfigurationException {
		pipeline.configure();
	}

	/**
	 * Configure the pipe adapter, pipeline and pipe(s)
	 */
	protected void configureAdapter() throws ConfigurationException {
		adapter.configure();
	}

}
