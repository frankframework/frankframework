package org.frankframework.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.util.SpringUtils;

public abstract class JdbcEnabledPipeTestBase<P extends IPipe> {

	protected PipeLineSession session = new PipeLineSession();
	protected DatabaseTestEnvironment env;

	protected P pipe;
	protected PipeLine pipeline;
	protected Adapter adapter;

	public abstract P createPipe();

	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		this.env = env;
		adapter = env.createBean(Adapter.class);
		pipe = createPipe();
		env.autowire(pipe);
		pipe.addForward(new PipeForward("success", "exit"));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		pipeline.addPipe(pipe);
		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		adapter.setName("TestAdapter-for-".concat(pipe.getClass().getSimpleName()));
		adapter.setPipeLine(pipeline);
	}

	protected final String getDataSourceName() {
		return env.getDataSourceName();
	}

	@AfterEach
	public void tearDown() throws Exception {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipe = null;
		pipeline = null;
		adapter = null;
	}

	protected ConfigurationWarnings getConfigurationWarnings() {
		return env.getConfiguration().getConfigurationWarnings();
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
