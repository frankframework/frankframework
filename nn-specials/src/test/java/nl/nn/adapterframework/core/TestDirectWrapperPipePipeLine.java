package nl.nn.adapterframework.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.extensions.esb.DirectWrapperPipe;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;

@SuppressWarnings("deprecation") //Part of the tests!
public class TestDirectWrapperPipePipeLine {

	TestConfiguration configuration;

	@BeforeEach
	void setUp() {
		configuration = new TestConfiguration();
	}

	@AfterEach
	void tearDown() {
		configuration.close();
	}

	@Test
	public void testDirectWrapperPipeSuccessForward() throws ConfigurationException, PipeRunException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);

		PipeForward pf = configuration.createBean(PipeForward.class);
		pf.setName("success");
		pf.setPath("nextPipe");

		PipeForward toExit = configuration.createBean(PipeForward.class);
		toExit.setName("success");
		toExit.setPath("EXIT");

		DirectWrapperPipe pipe = configuration.createBean(DirectWrapperPipe.class);
		pipe.setName("DirectWrapperPipe");
		pipe.registerForward(pf);
		pipeline.addPipe(pipe);

		EchoPipe echoPipe = configuration.createBean(EchoPipe.class);
		echoPipe.setName("nextPipe");
		echoPipe.setPipeLine(pipeline);
		echoPipe.registerForward(toExit);
		pipeline.addPipe(echoPipe);

		PipeLineExit exit = configuration.createBean(PipeLineExit.class);
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);

		pipeline.setOwner(pipe);
		pipeline.configure();

		CorePipeProcessor cpp = configuration.createBean(CorePipeProcessor.class);
		PipeLineSession ps = configuration.createBean(PipeLineSession.class);

		PipeRunResult pipeRunResult=cpp.processPipe(pipeline, pipe, new Message("<dummy/>"), ps);

		PipeForward pipeForward=pipeRunResult.getPipeForward();

		IForwardTarget target = pipeline.resolveForward(pipe, pipeForward);

		assertNotNull(target);
	}
}
