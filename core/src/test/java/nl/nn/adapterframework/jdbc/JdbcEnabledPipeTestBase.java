package nl.nn.adapterframework.jdbc;

import org.junit.After;
import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;

public abstract class JdbcEnabledPipeTestBase<P extends IPipe> extends JdbcTestBase {

	protected PipeLineSession session = new PipeLineSession();

	protected P pipe;
	protected PipeLine pipeline;
	protected Adapter adapter;
	private static TestConfiguration configuration;

	private TestConfiguration getConfiguration() {
		if(configuration == null) {
			configuration = new TestConfiguration();
		}
		return configuration;
	}

	public abstract P createPipe();

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		pipe = createPipe();
		autowireByType(pipe);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline = getConfiguration().createBean(PipeLine.class);
		pipeline.addPipe(pipe);
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState("success");
		pipeline.registerPipeLineExit(exit);
		adapter = getConfiguration().createBean(Adapter.class);
		adapter.setName("TestAdapter-for-".concat(pipe.getClass().getSimpleName()));
		adapter.setPipeLine(pipeline);
	}

	@After
	public void tearDown() throws Exception {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipe = null;
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

	/**
	 * Configure the pipe, don't forget to call {@link IPipe#start()} after configuring!
	 */
	protected void configurePipe() throws ConfigurationException {
		pipeline.configure();
	}

	protected PipeRunResult doPipe(String input) throws PipeRunException {
		return doPipe(new Message(input));
	}
	protected PipeRunResult doPipe(Message input) throws PipeRunException {
		return pipe.doPipe(input, session);
	}
}
