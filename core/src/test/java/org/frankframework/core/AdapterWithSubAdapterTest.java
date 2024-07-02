package org.frankframework.core;

import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.receivers.DummySender;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.RunState;
import org.frankframework.util.UUIDUtil;
import org.jetbrains.annotations.NotNull;

@Log4j2
public class AdapterWithSubAdapterTest {

	public static final String MAIN_ADAPTER_SECRET = "A1";
	public static final String MAIN_RECEIVER_SECRET = "R1";
	public static final String SUB_ADAPTER_SECRET = "A2";
	public static final String SUB_RECEIVER_SECRET = "R2";

	private TestConfiguration configuration;
	private TestAppender appender;

	@BeforeEach
	public void setUp() {
		configuration = TransactionManagerType.DATASOURCE.create(false);
		appender = TestAppender.newBuilder().useIbisPatternLayout("[%level] - [%class] - %m").build();

	}

	@AfterEach
	public void tearDown() {
		configuration.close();
		TestAppender.removeAppender(appender);
	}

	public JavaListener<String> setupJavaListener(String name) {
		@SuppressWarnings("unchecked")
		JavaListener<String> listener = configuration.createBean(JavaListener.class);
		configuration.autowireByName(listener);
		listener.setName(name);
		return listener;
	}

	private  <M> Receiver<M> setupReceiver(IListener<M> listener, String hideRegex) {
		@SuppressWarnings("unchecked")
		Receiver<M> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		receiver.setHideRegex(hideRegex);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	private  <M> Adapter setupAdapter(Receiver<M> receiver, PipeLine.ExitState exitState, String name, IPipe... pipes) throws ConfigurationException {
		assertNotNull(pipes);
		assertTrue(pipes.length > 0, "Should add at least 1 pipe");

		Adapter adapter = configuration.createBean(Adapter.class);
		configuration.autowireByName(adapter);
		adapter.setName(name);

		CorePipeLineProcessor plp = configuration.createBean(CorePipeLineProcessor.class);
		PipeProcessor pp = configuration.createBean(CorePipeProcessor.class);
		plp.setPipeProcessor(pp);
		PipeLine pl = configuration.createBean(PipeLine.class);
		configuration.autowireByName(pl);
		pl.setPipeLineProcessor(plp);
		pl.setFirstPipe(pipes[0].getName());
		for (IPipe pipe : pipes) {
			pl.addPipe(pipe);
		}

		PipeLineExit ple = new PipeLineExit();
		ple.setName(exitState.name());
		ple.setState(exitState);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.registerReceiver(receiver);
		configuration.registerAdapter(adapter);
		return adapter;
	}

	private static @NotNull IPipe createLoggingPipe(String name, String hideRegex) {
		IPipe pipe = new FixedForwardPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) {
				// Should be hidden in context of main adapter, may be visible when logged from receiver logs
				message.getContext().put("secret1", MAIN_ADAPTER_SECRET);

				// Should be hidden only in context of sub adapter, should be visible when from receiver or main adapter logs
				message.getContext().put("secret2", SUB_ADAPTER_SECRET);

				// Log something containing current adapter name and all the things we're trying to hide
				log.info("[{}] I expect logging to hide the text in brackets: Main Adapter - [{}]-[{}]; Sub Adapter - [{}]-[{}]", getAdapter().getName(), MAIN_ADAPTER_SECRET, MAIN_RECEIVER_SECRET, SUB_ADAPTER_SECRET, SUB_RECEIVER_SECRET);

				// Hand off to super
				return new PipeRunResult(getSuccessForward(), message);
			}
		};
		pipe.setName(name);
		pipe.setHideRegex(hideRegex);
		return pipe;
	}

	private IPipe createSubAdapterCallPipe(String subAdapterName, String hideRegex) {
		IbisLocalSender localSender = configuration.createBean(IbisLocalSender.class);
		configuration.autowireByName(localSender);
		localSender.setName(subAdapterName);
		localSender.setJavaListener(subAdapterName);
		localSender.setCheckDependency(true);
		localSender.setIsolated(false);
		localSender.setSynchronous(true);

		SenderPipe senderPipe = configuration.createBean(SenderPipe.class);
		configuration.autowireByName(senderPipe);
		senderPipe.setName(subAdapterName);
		senderPipe.setHideRegex(hideRegex);
		senderPipe.setSender(localSender);

		return senderPipe;
	}

	private <M> JavaListener<String> createParentAdapter(JavaListener<M> subAdapterListener, String name) throws ConfigurationException {
		IPipe pipe1 = createLoggingPipe("echo1", null);
		IPipe pipe2 = createSubAdapterCallPipe(subAdapterListener.getName(), MAIN_ADAPTER_SECRET);
		IPipe pipe3 = createLoggingPipe("echo2", null);

		JavaListener<String> listener = setupJavaListener(name);
		Receiver<String> receiver = setupReceiver(listener, MAIN_RECEIVER_SECRET);
		setupAdapter(receiver, PipeLine.ExitState.SUCCESS, name, pipe1, pipe2, pipe3);

		return listener;
	}

	private JavaListener<String> createSubAdapter(String name) throws ConfigurationException {
		IPipe pipe = createLoggingPipe("echo", SUB_ADAPTER_SECRET);
		JavaListener<String> listener = setupJavaListener(name);
		Receiver<String> receiver = setupReceiver(listener, SUB_RECEIVER_SECRET);
		setupAdapter(receiver, PipeLine.ExitState.SUCCESS, name, pipe);

		return listener;
	}

	@Test
	public void testNestedHideRegexesProcessSuccess() throws ConfigurationException, ListenerException {
		// Arrange
		JavaListener<String> subAdapterListener = createSubAdapter("sub-adapter");
		JavaListener<String> mainAdapterListener = createParentAdapter(subAdapterListener, "main-adapter");

		configuration.configure();
		configuration.start();

		// Wait until all adapters are started
		for (Adapter adapter : configuration.getRegisteredAdapters()) {
			waitWhileInState(adapter, RunState.STOPPED, RunState.STARTING);

			assertEquals(RunState.STARTED, adapter.getRunState());
		}

		String inputMessage = "Message to hide: [" + MAIN_RECEIVER_SECRET + "]";
		TestAppender.addToRootLogger(appender);

		// Act
		String result = mainAdapterListener.processRequest(UUIDUtil.createRandomUUID(), inputMessage, new HashMap<>());

		// Assert
		assertEquals(inputMessage, result);

		List<String> logLines = appender.getLogLines();
//		System.err.println("--- <<BEGIN All Captured Loglines>> ---");
//		System.err.println(logLines);
//		System.err.println("--- <<END All Captured Loglines>> ---");
		assertThat(logLines, not(hasItem(containsString(MAIN_RECEIVER_SECRET))));
		assertThat(logLines, not(hasItem(allOf(containsString("Adapter"), containsString(MAIN_ADAPTER_SECRET)))));
		assertThat(logLines, not(hasItem(allOf(containsString("[main-adapter]"), containsString(MAIN_ADAPTER_SECRET)))));

		// These can appear
		assertThat(logLines, hasItem(allOf(containsString("[main-adapter]"), containsString(SUB_RECEIVER_SECRET))));
		assertThat(logLines, hasItem(allOf(containsString("[main-adapter]"), containsString(SUB_ADAPTER_SECRET))));

		assertThat(logLines, not(hasItem(allOf(containsString("[sub-adapter]"), containsString(SUB_RECEIVER_SECRET)))));
		assertThat(logLines, not(hasItem(allOf(containsString("[sub-adapter]"), containsString(SUB_ADAPTER_SECRET)))));
	}
}
