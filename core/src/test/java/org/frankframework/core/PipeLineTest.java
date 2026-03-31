package org.frankframework.core;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hamcrest.core.StringEndsWith;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.BeanCreationException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.lifecycle.events.ConfigurationMessageEvent;
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
	public void testNoParent() {
		try (PipeLine pipeline = new PipeLine()) {
			assertThrows(IllegalArgumentException.class, pipeline::afterPropertiesSet);
		}
	}

	@Test
	public void testWrongParent() {
		assertThrows(BeanCreationException.class, () -> configuration.createBean(PipeLine.class));
	}

	@Test
	public void testClassLoaders() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		configuration.configure();

		ClassLoader classLoader = configuration.getClassLoader();
		assertEquals(classLoader, configuration.getConfigurationClassLoader());

		assertEquals(classLoader, adapter.getClassLoader());
		// I'd like to test the Adapter's ConfigurationClassLoader here but it's based on the ContextClassloader.

		assertEquals(classLoader, pipeline.getClassLoader());
		assertEquals(classLoader, pipeline.getConfigurationClassLoader());
	}

	@Test
	public void testNoPipes() {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		ConfigurationException ex = assertThrows(ConfigurationException.class, configuration::configure);
		assertEquals("no Pipes in Pipeline", ex.getMessage());
	}

	@Test
	public void testNoPipeName() {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		ConfigurationException ex = assertThrows(ConfigurationException.class, () -> pipeline.addPipe(new EchoPipe()));
		assertEquals("unable to add pipe [EchoPipe] without name", ex.getMessage());
	}

	@Test
	public void testDuplicatePipeName() throws Exception {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		EchoPipe pipe1 = SpringUtils.createBean(adapter);
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = SpringUtils.createBean(adapter);
		pipe2.setName("one");

		ConfigurationException ex = assertThrows(ConfigurationException.class, () -> pipeline.addPipe(pipe2));
		assertEquals("unable to add pipe with duplicate name [one]", ex.getMessage());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "one")
	public void testGetFirstPipe(String firstPipe) throws Exception {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);
		pipeline.setFirstPipe(firstPipe);

		EchoPipe pipe1 = SpringUtils.createBean(adapter);
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = SpringUtils.createBean(adapter);
		pipe2.setName("two");
		pipeline.addPipe(pipe2);

		configuration.configure();

		assertEquals("one", pipeline.getFirstPipe());
	}

	@Test
	public void testGetFirstPipe() throws Exception {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);
		pipeline.setFirstPipe("two");

		EchoPipe pipe1 = SpringUtils.createBean(adapter);
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		EchoPipe pipe2 = SpringUtils.createBean(adapter);
		pipe2.setName("two");
		pipeline.addPipe(pipe2);

		configuration.configure();

		assertEquals("two", pipeline.getFirstPipe());
	}

	@Test
	public void firstPipeDoesNotExist() throws Exception {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);
		pipeline.setFirstPipe("two");

		EchoPipe pipe1 = SpringUtils.createBean(adapter);
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		ConfigurationException ex = assertThrows(ConfigurationException.class, configuration::configure);
		assertEquals("no pipe found for firstPipe [two]", ex.getMessage());
	}

	@Test
	public void testDuplicateExits() {
		Adapter adapter = configuration.createBean();
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		PipeLineExit exit = SpringUtils.createBean(adapter);
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
	public void pipesCannotConfigure() throws Exception {
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		EchoPipe pipe1 = spy(SpringUtils.createBean(adapter, EchoPipe.class));
		doThrow(new ConfigurationException("pipe1")).when(pipe1).configure();
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		ConfigurationException ex = assertThrows(ConfigurationException.class, configuration::configure);
		assertEquals("Exception configuring EchoPipe [one]: pipe1", ex.getMessage());

		List<String> messages = configuration.getEvents(ConfigurationMessageEvent.class);

		assertThat(messages.getFirst(), Matchers.containsString("Configuration [TestConfiguration] configured in "));
		assertEquals("Configuration [TestConfiguration] aborted starting; Exception configuring EchoPipe [one]: pipe1", messages.get(1));

	}

	@Test
	public void testLifecycle() throws Exception {
		configuration.clearEvents();
		Adapter adapter = configuration.createBean();
		configuration.addAdapter(adapter);
		adapter.setName("testAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeline);

		EchoPipe pipe1 = spy(SpringUtils.createBean(adapter, EchoPipe.class));
		pipe1.setName("one");
		pipeline.addPipe(pipe1);

		configuration.configure();
		configuration.start();

		// Start is done asynchronous. Wait till it's ready.
		await().pollInterval(3, TimeUnit.SECONDS)
			.atMost(Duration.ofSeconds(30))
			.until(adapter::isRunning);

		verify(pipe1).configure();
		verify(pipe1).start();

		List<String> configEvents = configuration.getEvents(ConfigurationMessageEvent.class);
		assertEquals(1, configEvents.size());
		assertThat(configEvents.getFirst(), Matchers.containsString("Configuration [TestConfiguration] configured in "));

		configuration.stop();
		verify(pipe1).stop();
	}

	@Test
	public void testFixedForwardPipesWithNoForwardShouldDefaultToNextPipe() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean();
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getRegisteredForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, getPipeRegisteredForwardsMap(pipe).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getRegisteredForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", getPipeRegisteredForwardsMap(pipe2).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	void testFixedForwardAsLastPipeForwardsToFirstSuccessfulExit() throws ConfigurationException {
		// Arrange
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean();
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
		assertEquals(1, pipe.getRegisteredForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, getPipeRegisteredForwardsMap(pipe).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getRegisteredForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", getPipeRegisteredForwardsMap(pipe2).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to successful pipeline-exit");
	}

	@Test
	public void testPipesWithNormalForwardToNextPipe() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean();
		pipe2.setName(pipeForwardName);
		pipe2.addForward(new PipeForward("success", "exit"));
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");
		assertEquals(1, pipe.getRegisteredForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, getPipeRegisteredForwardsMap(pipe).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getRegisteredForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", getPipeRegisteredForwardsMap(pipe2).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	public void giveWarningWhenForwardIsAlreadyDefined() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.addForward(new PipeForward("success", pipeForwardName));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean();
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
		assertEquals(1, pipe.getRegisteredForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals(pipeForwardName, getPipeRegisteredForwardsMap(pipe).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should default to next pipe");

		assertEquals(1, pipe2.getRegisteredForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("exit", getPipeRegisteredForwardsMap(pipe2).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	@Test
	public void giveWarningWhenForwardToNonExistingPipe() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.addForward(new PipeForward("success", "the next pipe"));
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		EchoPipe pipe2 = configuration.createBean();
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
		assertEquals(1, pipe.getRegisteredForwards().size(), "pipe1 should only have 1 pipe-forward");
		assertEquals("the next pipe", getPipeRegisteredForwardsMap(pipe).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe1 forward should exist even though next pipe doesn't exist");

		assertEquals(1, pipe2.getRegisteredForwards().size(), "pipe2 should only have 1 pipe-forward");
		assertEquals("special exit name", getPipeRegisteredForwardsMap(pipe2).get(PipeForward.SUCCESS_FORWARD_NAME).getPath(), "pipe2 forward should default to pipeline-exit");
	}

	private static Map<String, PipeForward> getPipeRegisteredForwardsMap(IPipe pipe) {
		return pipe.getRegisteredForwards()
				.stream()
				.collect(Collectors.toMap(PipeForward::getName, p -> p));
	}

	private static class NonFixedForwardPipe extends AbstractPipe {
		@NonNull
		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) {
			return new PipeRunResult(findForward(PipeForward.SUCCESS_FORWARD_NAME), message);
		}
	}

	@Test
	public void testNonFixedForwardPipesWithNoForwardToNextPipe() throws ConfigurationException {
		Adapter adapter = configuration.createBean();
		PipeLine pipeline = SpringUtils.createBean(adapter);
		String pipeForwardName = "EchoPipe next pipe";

		NonFixedForwardPipe pipe = configuration.createBean();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipe.setPipeLine(pipeline);
		pipeline.addPipe(pipe);

		NonFixedForwardPipe pipe2 = configuration.createBean();
		pipe2.setName(pipeForwardName);
		pipe2.setPipeLine(pipeline);
		pipeline.addPipe(pipe2);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);
		pipeline.configure();

		assertTrue(configuration.getConfigurationWarnings().getWarnings().isEmpty(), "pipe should not cause any configuration warnings");

		assertTrue(pipe.getRegisteredForwards().isEmpty(), "pipe1 should have no forwards");
		assertTrue(pipe2.getRegisteredForwards().isEmpty(), "pipe2 should not have a pipe-forward");
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
		assertDoesNotThrow(() -> adapter.processMessageWithExceptions("m1", Message.nullMessage(), session));
	}

	@Test
	public void testAdapterExpectedSessionKeysMissingKey() throws ConfigurationException {
		// Arrange
		Adapter adapter = buildTestAdapter();

		PipeLineSession session = new PipeLineSession();
		session.put("k1", "v1");

		// Act // Assert
		ListenerException e = assertThrows(ListenerException.class, () -> adapter.processMessageWithExceptions("m1", Message.nullMessage(), session));

		// Assert
		assertEquals("Adapter [Adapter] called without expected session keys [k2, k3]", e.getMessage());
	}

	private @NonNull Adapter buildTestAdapter() throws ConfigurationException {
		Adapter adapter = new Adapter() {
			@NonNull
			@Override
			public RunState getRunState() {
				return RunState.STARTED;
			}
		};
		configuration.autowireByName(adapter);
		adapter.setName("Adapter");
		buildDummyPipeLine(adapter);
		adapter.setApplicationContext(configuration);
		adapter.setConfigurationMetrics(configuration.getBean(MetricsInitializer.class));
		adapter.configure();
		return adapter;
	}

	private void buildDummyPipeLine(Adapter adapter) throws ConfigurationException {
		PipeLine pipeLine = SpringUtils.createBean(adapter);
		pipeLine.setConfigurationMetrics(configuration.getBean(MetricsInitializer.class));
		CorePipeLineProcessor pipeLineProcessor = configuration.createBean();
		pipeLineProcessor.setPipeProcessor(configuration.createBean(CorePipeProcessor.class));
		pipeLine.setPipeLineProcessor(pipeLineProcessor);
		EchoPipe pipe = buildTestPipe(pipeLine);
		pipeLine.setFirstPipe(pipe.getName());
		pipeLine.setExpectsSessionKeys("k1, k2,k3");
		adapter.setPipeLine(pipeLine);
	}

	private @NonNull EchoPipe buildTestPipe(@NonNull PipeLine pipeLine) throws ConfigurationException {
		EchoPipe pipe = new EchoPipe();
		pipe.setName("Pipe" + ++pipeNr);
		pipeLine.addPipe(pipe);
		return pipe;
	}
}
