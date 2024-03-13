package org.frankframework.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") //Part of the tests!
public class PipeLineTest {

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
	public void testDuplicateExits() throws ConfigurationException {
		Adapter adapter = new Adapter();
		PipeLine pipeline = new PipeLine();
		pipeline.setApplicationContext(configuration);
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("success");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.registerPipeLineExit(exit);
		adapter.setPipeLine(pipeline);

		List<String> warnings = configuration.getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		String lastWarning = warnings.get(warnings.size()-1);
		assertThat(lastWarning,StringEndsWith.endsWith("PipeLine exit named [success] already exists"));
	}

	@Test
	public void testFixedForwardPipesWithNoForwardShouldDefaultToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	void testFixedForwardAsLastPipeForwardsToFirstSuccessfulExit() throws ConfigurationException {
		// Arrange
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit errorExit = new PipeLineExit();
		errorExit.setPath("error");
		errorExit.setState(ExitState.ERROR);
		pipeline.registerPipeLineExit(errorExit);
		PipeLineExit successExit = new PipeLineExit();
		successExit.setPath("exit");
		successExit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(successExit);
		PipeLineExit successExit2 = new PipeLineExit();
		successExit2.setPath("exit2");
		successExit2.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(successExit2);

		// Act
		pipeline.configure();

		// Assert
		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to successful pipeline-exit");
	}

	@Test
	public void testPipesWithNormalForwardToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	public void giveWarningWhenForwardIsAlreadyDefined() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe.registerForward(new PipeForward("success", "exit"));//Surprisingly this doesn't cause any warnings
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertEquals(1, configuration.getConfigurationWarnings().getWarnings().size(), "pipes should cause a configuration warning");
		assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] forward [success] is already registered"));
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	public void giveWarningWhenForwardToNonExistingPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean(EchoPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", "the next pipe"));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("special exit name");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertEquals(1, configuration.getConfigurationWarnings().getWarnings().size(), "pipes should cause a configuration warning");
		assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] has a forward of which the pipe to execute [the next pipe] is not defined"));
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals("the next pipe", pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should exist even though next pipe doesn't exist");

		assertEquals(1, pipe2.getForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("special exit name", pipe2.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	private static class NonFixedForwardPipe extends AbstractPipe {
		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			return new PipeRunResult(findForward(PipeForward.SUCCESS_FORWARD_NAME), message);
		}
	}

	@Test
	public void testNonFixedForwardPipesWithNoForwardToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IPipe pipe = configuration.createBean(NonFixedForwardPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.registerForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IPipe pipe2 = configuration.createBean(NonFixedForwardPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.registerForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, pipe.getForwards().get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertTrue(pipe2.getForwards().isEmpty(), "pipe2 should not have a pipe-forward");
	}
}
