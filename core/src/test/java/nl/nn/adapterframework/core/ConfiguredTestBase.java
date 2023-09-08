package nl.nn.adapterframework.core;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public abstract class ConfiguredTestBase {
	protected Logger log = LogUtil.getLogger(this);

	public static final String testMessageId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
	public static final String testCorrelationId = "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";

	protected PipeLineSession session;

	protected PipeLine pipeline;
	protected Adapter adapter;
	private static  TestConfiguration configuration;

	protected TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}

	@Before
	@BeforeEach
	public void setUp() throws Exception {
		pipeline = getConfiguration().createBean(PipeLine.class);
		adapter = getConfiguration().createBean(Adapter.class);
		adapter.setName("TestAdapter of "+getClass().getSimpleName());
		adapter.setPipeLine(pipeline);
		session = new PipeLineSession();
	}

	@After
	@AfterEach
	public void tearDown() throws Exception {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipeline = null;
		adapter = null;
		if(session != null) {
			session.close();
		}
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
