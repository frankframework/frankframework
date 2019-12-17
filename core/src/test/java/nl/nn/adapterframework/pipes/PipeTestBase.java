package nl.nn.adapterframework.pipes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public abstract class PipeTestBase<P extends IPipe> {
	protected Log log = LogFactory.getLog(this.getClass());

	@Mock
	protected IPipeLineSession session;

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
	protected PipeRunResult doPipe(P pipe, Object input, IPipeLineSession session) throws Exception {
		return pipe.doPipe(input, session);
	}
	

}
