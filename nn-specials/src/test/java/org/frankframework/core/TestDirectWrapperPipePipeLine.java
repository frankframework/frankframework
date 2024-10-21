package org.frankframework.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.extensions.esb.DirectWrapperPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;

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
		pipe.addForward(pf);
		pipeline.addPipe(pipe);

		EchoPipe echoPipe = configuration.createBean(EchoPipe.class);
		echoPipe.setName("nextPipe");
		echoPipe.setPipeLine(pipeline);
		echoPipe.addForward(toExit);
		pipeline.addPipe(echoPipe);

		PipeLineExit exit = configuration.createBean(PipeLineExit.class);
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);

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
