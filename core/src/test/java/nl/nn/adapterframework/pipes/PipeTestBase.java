package nl.nn.adapterframework.pipes;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;

public abstract class PipeTestBase<P extends IPipe> {
	protected Logger log = LogUtil.getLogger(this);

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

	@Rule
	public ExpectedException exception = ExpectedException.none();

	public abstract P createPipe() throws ConfigurationException;

	@Before
	public void setup() throws Exception {
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
	 * Configure the pipe
	 */
	protected void configurePipe() throws ConfigurationException {
		pipeline.configure();
	}

	/**
	 * Configure and start the pipe
	 */
	protected void configureAndStartPipe() throws ConfigurationException, PipeStartException {
		configurePipe();
		pipe.start();
	}

	/**
	 * Configure the pipe adapter, pipeline and pipe(s)
	 */
	protected void configureAdapter() throws ConfigurationException {
		adapter.configure();
	}

	/*
	 * use these methods to execute pipe, instead of calling pipe.doPipe directly. This allows for 
	 * integrated testing of streaming.
	 */
	protected PipeRunResult doPipe(String input) throws PipeRunException {
		return doPipe(pipe, new Message(input), session);
	}
	protected PipeRunResult doPipe(Message input) throws PipeRunException {
		return doPipe(pipe, input, session);
	}
	protected PipeRunResult doPipe(P pipe, Message input, PipeLineSession session) throws PipeRunException {
		return pipe.doPipe(input, session);
	}
	
	protected PipeRunResult doPipe(P pipe, Object input, PipeLineSession session) throws PipeRunException {
		return doPipe(pipe, Message.asMessage(input), session);
	}

}
