package nl.nn.adapterframework.pipes;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

public abstract class PipeTestBase<P extends IPipe> {
	protected Logger log = LogUtil.getLogger(this);

	protected IPipeLineSession session = new PipeLineSessionBase();

	protected P pipe;
	protected PipeLine pipeline;
	protected Adapter adapter;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	public abstract P createPipe();
	
	@Before
	public void setup() throws ConfigurationException {
		pipe = createPipe();
		pipe.registerForward(new PipeForward("success",null));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline = new PipeLine();
		pipeline.addPipe(pipe);
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState("success");
		pipeline.registerPipeLineExit(exit);
		adapter = new Adapter();
		adapter.registerPipeLine(pipeline);
	}

	/**
	 * Configure the pipe
	 */
	protected void configurePipe() throws ConfigurationException, PipeStartException {
		if (pipe instanceof IExtendedPipe) {
			((IExtendedPipe) pipe).configure(pipeline);
		} else {
			pipe.configure();
		}
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
	 * use this method to execute pipe, instead of calling pipe.doPipe directly. This allows for 
	 * integrated testing of streaming.
	 */
	protected PipeRunResult doPipe(Message input) throws PipeRunException {
		return doPipe(pipe, input, session);
	}
	protected PipeRunResult doPipe(P pipe, Message input, IPipeLineSession session) throws PipeRunException {
		return pipe.doPipe(input, session);
	}
	
	protected PipeRunResult doPipe(P pipe, Object input, IPipeLineSession session) throws PipeRunException {
		return doPipe(pipe, Message.asMessage(input), session);
	}

}
