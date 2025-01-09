package org.frankframework.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.annotation.Nonnull;

import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

@SuppressWarnings("deprecation") // Part of the tests!
public class PipeLineTest {
	private int pipeNr = 0;

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
	public void testDuplicateExits() {
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter, PipeLine.class);
		PipeLineExit exit = SpringUtils.createBean(adapter, PipeLineExit.class);
		exit.setName("success");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.addPipeLineExit(exit);
		adapter.setPipeLine(pipeline);

		List<String> warnings = configuration.getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		String lastWarning = warnings.get(warnings.size()-1);
		assertThat(lastWarning, StringEndsWith.endsWith("PipeLine exit named [success] already exists"));
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
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
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
		errorExit.setName("error");
		errorExit.setState(ExitState.ERROR);
		pipeline.addPipeLineExit(errorExit);
		PipeLineExit successExit = new PipeLineExit();
		successExit.setName("exit");
		successExit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(successExit);
		PipeLineExit successExit2 = new PipeLineExit();
		successExit2.setName("exit2");
		successExit2.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(successExit2);

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
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.addForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
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
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe.addForward(new PipeForward("success", "exit"));
		pipe.addForward(new PipeForward("success", "exit"));
		pipe.addForward(new PipeForward("success", "exit"));
		pipe.addForward(new PipeForward("success", "exit")); // Surprisingly this doesn't cause any warnings
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.configure();

		assertEquals(1, configuration.getConfigurationWarnings().getWarnings().size(), "pipes should cause a configuration warning");
		assertThat(configuration.getConfigWarning(0), StringEndsWith.endsWith("] the forward [success] is already registered on this pipe"));
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
		pipe.addForward(new PipeForward("success", "the next pipe"));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean(EchoPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("special exit name");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
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
		public PipeRunResult doPipe(Message message, PipeLineSession session) {
			return new PipeRunResult(findForward(PipeForward.SUCCESS_FORWARD_NAME), message);
		}
	}

	@Test
	public void testNonFixedForwardPipesWithNoForwardToNextPipe() throws ConfigurationException {
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		String pipeForwardName = "EchoPipe next pipe";

		IPipe pipe = configuration.createBean(NonFixedForwardPipe.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		IPipe pipe2 = configuration.createBean(NonFixedForwardPipe.class);
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");

		assertTrue(pipe.getForwards().isEmpty(), "pipe1 should have no forwards");
		assertTrue(pipe2.getForwards().isEmpty(), "pipe2 should not have a pipe-forward");
	}

	@Test
	public void testAdapterExpectedSessionKeysAllPresent() throws ConfigurationException {
		// Arrange
		Adapter adapter = buildTestAdapter();

		PipeLineSession session = new PipeLineSession();
		session.put("k1", "v1");
		session.put("k2", "v2");
		session.put("k3", "v3");

		// Act // Assert
		try (Message message = Message.nullMessage()) {
			assertDoesNotThrow(() -> adapter.processMessageWithExceptions("m1", message, session));
			assertFalse(message.isClosed());
		}
	}

	@Test
	public void testAdapterExpectedSessionKeysMissingKey() throws ConfigurationException {
		// Arrange
		Adapter adapter = buildTestAdapter();

		PipeLineSession session = new PipeLineSession();
		session.put("k1", "v1");

		// Act // Assert
		try (Message message = Message.nullMessage()) {
			ListenerException e = assertThrows(ListenerException.class, () -> adapter.processMessageWithExceptions("m1", message, session));

			// Assert
			assertEquals("Adapter [Adapter] called without expected session keys [k2, k3]", e.getMessage());
			assertFalse(message.isClosed());
		}
	}

	private @Nonnull Adapter buildTestAdapter() throws ConfigurationException {
		Adapter adapter = new Adapter() {
			@Override
			public RunState getRunState() {
				return RunState.STARTED;
			}
		};
		adapter.setName("Adapter");
		buildDummyPipeLine(adapter);
		adapter.setConfiguration(configuration);
		adapter.setApplicationContext(configuration);
		adapter.setConfigurationMetrics(configuration.getBean(MetricsInitializer.class));
		adapter.refresh();
		adapter.configure();
		return adapter;
	}

	private void buildDummyPipeLine(Adapter adapter) throws ConfigurationException {
		PipeLine pipeLine = new PipeLine();
		pipeLine.setApplicationContext(configuration);
		pipeLine.setConfigurationMetrics(configuration.getBean(MetricsInitializer.class));
		CorePipeLineProcessor pipeLineProcessor = configuration.createBean(CorePipeLineProcessor.class);
		pipeLineProcessor.setPipeProcessor(configuration.createBean(CorePipeProcessor.class));
		pipeLine.setPipeLineProcessor(pipeLineProcessor);
		EchoPipe pipe = buildTestPipe(pipeLine);
		pipeLine.setFirstPipe(pipe.getName());
		pipeLine.setExpectsSessionKeys("k1, k2,k3");
		adapter.setPipeLine(pipeLine);
	}

	private @Nonnull EchoPipe buildTestPipe(@Nonnull PipeLine pipeLine) throws ConfigurationException {
		EchoPipe pipe = new EchoPipe();
		pipe.setName("Pipe" + ++pipeNr);
		pipeLine.addPipe(pipe);
		return pipe;
	}
}
