package nl.nn.adapterframework.pipes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;

public abstract class PipeTestBase<P extends IPipe> {
	protected Log log = LogFactory.getLog(this.getClass());
	
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
		adapter = new Adapter();
		adapter.registerPipeLine(pipeline);
	}

	protected void configurePipe() throws ConfigurationException {
		if (pipe instanceof AbstractPipe) {
			((AbstractPipe) pipe).configure(pipeline);
		} else {
			pipe.configure();
		}
	}

	
	@Test
	public void notConfigured() throws ConfigurationException {
		pipe = createPipe();
		exception.expect(ConfigurationException.class);
		pipe.configure();
	}

	@Test
	public void basicNoAdditionalConfig() throws ConfigurationException {
		adapter.configure();
	}

}
