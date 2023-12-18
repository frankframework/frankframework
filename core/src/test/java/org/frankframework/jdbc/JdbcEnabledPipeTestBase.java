package org.frankframework.jdbc;

import org.junit.After;
import org.junit.Before;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

public abstract class JdbcEnabledPipeTestBase<P extends IPipe> extends JdbcTestBase {

	protected PipeLineSession session = new PipeLineSession();

	protected P pipe;
	protected PipeLine pipeline;
	protected Adapter adapter;

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
		exit.setState(ExitState.SUCCESS);
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
