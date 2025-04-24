package org.frankframework.core;

import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.receivers.DummySender;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.UUIDUtil;

public class AdapterHideRegexTest {

	public static final String MAIN_ADAPTER_SECRET = "A1";
	public static final String MAIN_RECEIVER_SECRET = "R1";
	public static final String SUB_ADAPTER_SECRET = "A2";
	public static final String SUB_RECEIVER_SECRET = "R2";

	private TestConfiguration configuration;
	private PipeLineSession pipeLineSession;

	private TestAppender getAppender() {
		return TestAppender.newBuilder().useIbisPatternLayout("[%level] - [%class] - %m").build();
	}

	@BeforeEach
	public void setUp() {
		configuration = TransactionManagerType.DATASOURCE.create(false);
		pipeLineSession = new PipeLineSession();
	}

	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(pipeLineSession, configuration);
	}

	public JavaListener<String> setupJavaListener(String name) {
		JavaListener<String> listener = configuration.createBean();
		listener.setName(name);
		return listener;
	}

	private  <M> Receiver<M> setupReceiver(IListener<M> listener, String hideRegex) {
		Receiver<M> receiver = configuration.createBean();
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		receiver.setHideRegex(hideRegex);
		DummySender sender = configuration.createBean();
		receiver.setSender(sender);
		return receiver;
	}

	private <M> Adapter setupAdapter(Receiver<M> receiver, PipeLine.ExitState exitState, String name, IPipe... pipes) throws ConfigurationException {
		assertNotNull(pipes);
		assertTrue(pipes.length > 0, "Should add at least 1 pipe");

		Adapter adapter = configuration.createBean();
		adapter.setName(name);

		CorePipeLineProcessor plp = configuration.createBean();
		CorePipeProcessor pp = configuration.createBean();
		plp.setPipeProcessor(pp);
		PipeLine pl = SpringUtils.createBean(adapter);
		pl.setPipeLineProcessor(plp);
		pl.setFirstPipe(pipes[0].getName());
		for (IPipe pipe : pipes) {
			SpringUtils.autowireByName(adapter, pipe);
			pl.addPipe(pipe);
		}

		PipeLineExit ple = new PipeLineExit();
		ple.setName(exitState.name());
		ple.setState(exitState);
		pl.addPipeLineExit(ple);
		adapter.setPipeLine(pl);

		SpringUtils.autowireByName(adapter, receiver);
		adapter.addReceiver(receiver);
		configuration.addAdapter(adapter);
		return adapter;
	}

	private static @Nonnull IPipe createLoggingPipe(String name, String hideRegex, boolean doThrowException) {
		IPipe pipe = new FixedForwardPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
				// Should be hidden in context of main adapter, may be visible when logged from receiver logs
				message.getContext().put("secret1", MAIN_ADAPTER_SECRET);

				// Should be hidden only in context of sub adapter, should be visible when from receiver or main adapter logs
				message.getContext().put("secret2", SUB_ADAPTER_SECRET);

				// Log something containing current adapter name and all the things we're trying to hide
				log.info("[{}] I expect logging to hide the text in brackets: Main Adapter - [{}]-[{}]; Sub Adapter - [{}]-[{}]", getAdapter().getName(), MAIN_ADAPTER_SECRET, MAIN_RECEIVER_SECRET, SUB_ADAPTER_SECRET, SUB_RECEIVER_SECRET);

				if (doThrowException) { // Exception Messages should not contain strings you want hidden
					throw new PipeRunException(this, "[%s] Gotta Throw what we Gotta Throw" // + " [%s]-[%s] [%s]-[%s]"
							.formatted(getAdapter().getName())); //, MAIN_ADAPTER_SECRET, MAIN_RECEIVER_SECRET, SUB_ADAPTER_SECRET, SUB_RECEIVER_SECRET));
				}
				// Return a result
				return new PipeRunResult(getSuccessForward(), message);
			}
		};
		pipe.setName(name);
		pipe.setHideRegex(hideRegex);
		return pipe;
	}

	private IPipe createSubAdapterCallPipe(String subAdapterName, String hideRegex, boolean subAdapterInSeparateThread) {
		IbisLocalSender localSender = configuration.createBean();
		localSender.setName(subAdapterName);
		localSender.setJavaListener(subAdapterName);
		localSender.setCheckDependency(true);
		localSender.setIsolated(subAdapterInSeparateThread);
		localSender.setSynchronous(true);

		SenderPipe senderPipe = configuration.createBean();
		senderPipe.setName(subAdapterName);
		senderPipe.setHideRegex(hideRegex);
		senderPipe.setSender(localSender);

		return senderPipe;
	}

	private <M> JavaListener<String> createMainAdapter(JavaListener<M> subAdapterListener, String name, boolean subAdapterInSeparateThread) throws ConfigurationException {
		IPipe pipe1 = createLoggingPipe("echo1", null, false);
		IPipe pipe2 = createSubAdapterCallPipe(subAdapterListener.getName(), MAIN_ADAPTER_SECRET, subAdapterInSeparateThread);
		IPipe pipe3 = createLoggingPipe("echo2", null, false);

		JavaListener<String> listener = setupJavaListener(name);
		Receiver<String> receiver = setupReceiver(listener, MAIN_RECEIVER_SECRET);
		setupAdapter(receiver, PipeLine.ExitState.SUCCESS, name, pipe1, pipe2, pipe3);

		return listener;
	}

	private JavaListener<String> createSubAdapter(String name, PipeLine.ExitState exitState, boolean doThrowException) throws ConfigurationException {
		IPipe pipe = createLoggingPipe("echo", SUB_ADAPTER_SECRET, doThrowException);
		JavaListener<String> listener = setupJavaListener(name);
		Receiver<String> receiver = setupReceiver(listener, SUB_RECEIVER_SECRET);
		setupAdapter(receiver, exitState, name, pipe);

		return listener;
	}

	private @Nonnull Adapter setupBasicAdapter(PipeLine.ExitState exitState, boolean doThrowException) throws ConfigurationException {
		IPipe pipe = createLoggingPipe("echo", MAIN_ADAPTER_SECRET, doThrowException);
		Receiver<?> receiver = mock(Receiver.class);
		when(receiver.getRunState()).thenReturn(RunState.STOPPED);
		Adapter adapter = setupAdapter(receiver, exitState, "main-adapter", pipe);

		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED, RunState.STARTING);
		assertEquals(RunState.STARTED, adapter.getRunState());
		return adapter;
	}

	private void verifyLoglinesBasicScenario(TestAppender appender) {
		List<String> logLines = appender.getLogLines();
		assertThat(logLines, not(hasItem(containsString(MAIN_ADAPTER_SECRET))));
	}

	@Test
	public void testHideRegexesProcessDirectSuccess() throws ConfigurationException, IOException {
		// Arrange
		Adapter adapter = setupBasicAdapter(PipeLine.ExitState.SUCCESS, false);
		Message inputMessage = new Message("Message to hide: [" + MAIN_ADAPTER_SECRET + "]");

		// Start capturing logs
		try (TestAppender appender = getAppender()) {

			// Act
			PipeLineResult result = adapter.processMessageDirect(UUIDUtil.createRandomUUID(), inputMessage, pipeLineSession);

			// Assert
			assertEquals(inputMessage.asString(), result.getResult().asString());

			// Stop capturing logs
			verifyLoglinesBasicScenario(appender);
		}
	}

	@Test
	public void testHideRegexesProcessDirectWithError() throws ConfigurationException, IOException {
		// Arrange
		Adapter adapter = setupBasicAdapter(PipeLine.ExitState.ERROR, false);
		Message inputMessage = new Message("Message to hide: [" + MAIN_ADAPTER_SECRET + "]");

		// Start capturing logs
		try (TestAppender appender = getAppender()) {

			// Act
			PipeLineResult result = adapter.processMessageDirect(UUIDUtil.createRandomUUID(), inputMessage, pipeLineSession);

			// Assert
			assertEquals(inputMessage.asString(), result.getResult().asString());

			// Stop capturing logs
			verifyLoglinesBasicScenario(appender);
		}
	}

	@Test
	public void testHideRegexesProcessDirectWithException() throws ConfigurationException, IOException {
		// Arrange
		Adapter adapter = setupBasicAdapter(PipeLine.ExitState.ERROR, true);
		Message inputMessage = new Message("Message to hide: [" + MAIN_ADAPTER_SECRET + "]");

		// Start capturing logs
		try (TestAppender appender = getAppender()) {
			// Act
			PipeLineResult result = adapter.processMessageDirect(UUIDUtil.createRandomUUID(), inputMessage, pipeLineSession);

			// Assert
			assertThat(result.getResult().asString(), containsString("error during pipeline processing"));

			// Stop capturing logs
			verifyLoglinesBasicScenario(appender);
		}
	}

	private JavaListener<String> setupNestedAdapters(PipeLine.ExitState exitState, boolean doThrowException, boolean subAdapterInSeparateThread) throws ConfigurationException {
		JavaListener<String> subAdapterListener = createSubAdapter("sub-adapter", exitState, doThrowException);
		JavaListener<String> mainAdapterListener = createMainAdapter(subAdapterListener, "main-adapter", subAdapterInSeparateThread);

		configuration.configure();
		configuration.start();

		// Wait until all adapters are started
		for (Adapter adapter : configuration.getRegisteredAdapters()) {
			waitWhileInState(adapter, RunState.STOPPED, RunState.STARTING);
			assertEquals(RunState.STARTED, adapter.getRunState());
		}
		return mainAdapterListener;
	}

	private static void assertThreadLocalReplaceIsEmpty() {
		Collection<Pattern> threadLocalReplace = IbisMaskingLayout.getThreadLocalReplace();
		assertTrue(threadLocalReplace == null || threadLocalReplace.isEmpty(), "ThreadLocalReplace of IbisMaskingLayout should have been empty");
	}

	private static void verifyLoglinesNestedAdapterScenario(List<String> logLines) {
		assertThat(logLines, not(hasItem(containsString(MAIN_RECEIVER_SECRET))));
		assertThat(logLines, not(hasItem(containsString(MAIN_ADAPTER_SECRET))));

		// This can be logged from the main adapter
		assertThat(logLines, hasItem(allOf(containsString("[main-adapter]"), containsString(SUB_RECEIVER_SECRET))));
		assertThat(logLines, hasItem(allOf(containsString("[main-adapter]"), containsString(SUB_ADAPTER_SECRET))));

		// But should not be logged from the sub adapter
		assertThat(logLines, not(hasItem(allOf(containsString("[sub-adapter]"), containsString(SUB_RECEIVER_SECRET)))));
		assertThat(logLines, not(hasItem(allOf(containsString("[sub-adapter]"), containsString(SUB_ADAPTER_SECRET)))));

		// Verify that things are actually hidden
		assertThat(logLines, hasItem(allOf(containsString("[main-adapter]"), containsString("Main Adapter - [**]-[**]; Sub Adapter - [%s]-[%s]".formatted(SUB_ADAPTER_SECRET, SUB_RECEIVER_SECRET)))));
		assertThat(logLines, hasItem(allOf(containsString("[sub-adapter]"), containsString("Main Adapter - [**]-[**]; Sub Adapter - [**]-[**]"))));

	}

	@Test
	public void testNestedHideRegexesProcessViaReceiverSuccess() throws ConfigurationException, ListenerException {
		// Arrange
		JavaListener<String> mainAdapterListener = setupNestedAdapters(PipeLine.ExitState.SUCCESS, false, false);

		String inputMessage = "Message to hide: [%s]-[%s]".formatted(MAIN_RECEIVER_SECRET, MAIN_ADAPTER_SECRET);

		// Start capturing logs
		try (TestAppender appender = getAppender()) {
			assertThreadLocalReplaceIsEmpty();

			// Act
			String result = mainAdapterListener.processRequest(UUIDUtil.createRandomUUID(), inputMessage, new HashMap<>());

			// Assert
			assertThreadLocalReplaceIsEmpty();
			assertEquals(inputMessage, result);

			// Stop capturing logs
			List<String> logLines = appender.getLogLines();
			verifyLoglinesNestedAdapterScenario(logLines);
		}
	}

	@Test
	public void testNestedHideRegexesProcessViaReceiverWithError() throws ConfigurationException, ListenerException {
		// Arrange
		JavaListener<String> mainAdapterListener = setupNestedAdapters(PipeLine.ExitState.ERROR, false, false);

		String inputMessage = "Message to hide: [%s]-[%s]".formatted(MAIN_RECEIVER_SECRET, MAIN_ADAPTER_SECRET);

		// Start capturing logs
		try (TestAppender appender = getAppender()) {
			assertThreadLocalReplaceIsEmpty();

			// Act
			String result = mainAdapterListener.processRequest(UUIDUtil.createRandomUUID(), inputMessage, new HashMap<>());

			// Assert
			assertThreadLocalReplaceIsEmpty();
			assertEquals(inputMessage, result);

			// Stop capturing logs
			List<String> logLines = appender.getLogLines();
			verifyLoglinesNestedAdapterScenario(logLines);
		}
	}

	@Test
	public void testNestedHideRegexesProcessViaReceiverWithException() throws ConfigurationException {
		// Arrange
		JavaListener<String> mainAdapterListener = setupNestedAdapters(PipeLine.ExitState.ERROR, true, false);

		String inputMessage = "Message to hide: [%s]-[%s]".formatted(MAIN_RECEIVER_SECRET, MAIN_ADAPTER_SECRET);

		// Start capturing logs
		try (TestAppender appender = getAppender()) {
			assertThreadLocalReplaceIsEmpty();

			// Act
			assertThrows(ListenerException.class, () -> mainAdapterListener.processRequest(UUIDUtil.createRandomUUID(), inputMessage, new HashMap<>()));

			// Assert
			assertThreadLocalReplaceIsEmpty();

			// Stop capturing logs
			List<String> logLines = appender.getLogLines();
			verifyLoglinesNestedAdapterScenario(logLines);
		}
	}

	@Test
	public void testNestedHideRegexesProcessWithIsolatedThreadCaller() throws ConfigurationException, ListenerException {
		// Arrange
		JavaListener<String> mainAdapterListener = setupNestedAdapters(PipeLine.ExitState.SUCCESS, false, true);

		String inputMessage = "Message to hide: [%s]-[%s]".formatted(MAIN_RECEIVER_SECRET, MAIN_ADAPTER_SECRET);

		// Start capturing logs
		try (TestAppender appender = getAppender()) {
			assertThreadLocalReplaceIsEmpty();

			// Act
			String result = mainAdapterListener.processRequest(UUIDUtil.createRandomUUID(), inputMessage, new HashMap<>());

			// Assert
			assertThreadLocalReplaceIsEmpty();
			assertEquals(inputMessage, result);

			// Stop capturing logs
			List<String> logLines = appender.getLogLines();
			verifyLoglinesNestedAdapterScenario(logLines);
		}
	}
}
