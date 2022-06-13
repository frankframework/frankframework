package nl.nn.adapterframework.core;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public abstract class ConfiguredTestBase {
	protected Logger log = LogUtil.getLogger(this);

	protected PipeLineSession session = new PipeLineSession();

	protected PipeLine pipeline;
	protected Adapter adapter;
	private static  TestConfiguration configuration;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	protected TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}



	@Before
	public void setUp() throws Exception {
		pipeline = getConfiguration().createBean(PipeLine.class);
		adapter = getConfiguration().createBean(Adapter.class);
		adapter.setName("TestAdapter of "+getClass().getSimpleName());
		adapter.setPipeLine(pipeline);
	}

	@After
	public void tearDown() throws Exception {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipeline = null;
		adapter = null;
	}

	protected void autowireByType(Object bean) {
		getConfiguration().autowireByType(bean);
	}

	protected void autowireByName(Object bean) {
		getConfiguration().autowireByName(bean);
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
