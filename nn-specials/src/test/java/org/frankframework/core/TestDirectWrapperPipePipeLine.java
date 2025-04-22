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
		PipeLine pipeline = configuration.createBean();

		PipeForward pf = configuration.createBean();
		pf.setName("success");
		pf.setPath("nextPipe");

		PipeForward toExit = configuration.createBean();
		toExit.setName("success");
		toExit.setPath("EXIT");

		DirectWrapperPipe pipe = configuration.createBean();
		pipe.setName("DirectWrapperPipe");
		pipe.addForward(pf);
		pipeline.addPipe(pipe);

		EchoPipe echoPipe = configuration.createBean();
		echoPipe.setName("nextPipe");
		echoPipe.setPipeLine(pipeline);
		echoPipe.addForward(toExit);
		pipeline.addPipe(echoPipe);

		PipeLineExit exit = configuration.createBean();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);

		pipeline.configure();

		CorePipeProcessor cpp = configuration.createBean();
		PipeLineSession ps = configuration.createBean();

		PipeRunResult pipeRunResult=cpp.processPipe(pipeline, pipe, new Message("<dummy/>"), ps);

		PipeForward pipeForward=pipeRunResult.getPipeForward();

		IForwardTarget target = pipeline.resolveForward(pipe, pipeForward);

		assertNotNull(target);
	}
}
