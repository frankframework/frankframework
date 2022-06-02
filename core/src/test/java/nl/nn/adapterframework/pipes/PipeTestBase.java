package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;

public abstract class PipeTestBase<P extends IPipe> extends ConfiguredTestBase {

	protected P pipe;

	public abstract P createPipe() throws ConfigurationException;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		pipe = createPipe();
		autowireByType(pipe);
		pipe.registerForward(new PipeForward("success", "READY"));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline.addPipe(pipe);
	}

	@Override
	public void tearDown() throws Exception {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipe = null;
		super.tearDown();
	}

	/**
	 * Configure the pipe
	 */
	protected void configurePipe() throws ConfigurationException {
		configurePipeline();
	}

	/**
	 * Configure and start the pipe
	 */
	protected void configureAndStartPipe() throws ConfigurationException, PipeStartException {
		configurePipeline();
		pipe.start();
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
