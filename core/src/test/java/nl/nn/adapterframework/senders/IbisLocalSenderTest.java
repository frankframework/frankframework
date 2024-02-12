package nl.nn.adapterframework.senders;

import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitForState;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.ThrowingAfterCloseInputStream;
import nl.nn.adapterframework.testutil.VirtualInputStream;
import nl.nn.adapterframework.util.RunState;

class IbisLocalSenderTest {
	public static final String SERVICE_NAME = "TEST-SERVICE";
	private final Logger log = LogManager.getLogger(this);

	public static final long EXPECTED_BYTE_COUNT = 1_000L;

	@AfterEach
	void tearDown() {
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
	}

	private static IbisLocalSender setupIbisLocalSender(TestConfiguration configuration, JavaListener<?> listener, boolean callByServiceName, boolean callIsolated, boolean callSynchronous) throws ConfigurationException {
		IsolatedServiceCaller serviceCaller = configuration.createBean(IsolatedServiceCaller.class);
		IbisLocalSender ibisLocalSender = configuration.createBean(IbisLocalSender.class);
		ibisLocalSender.setCheckDependency(true);
		ibisLocalSender.setIsolatedServiceCaller(serviceCaller);
		ibisLocalSender.setIsolated(callIsolated);
		// If not synchronous then isolated is always true, so actual value we set should depend on callIsolated
		ibisLocalSender.setSynchronous(!callIsolated || callSynchronous);

		if (callByServiceName) {
			//noinspection deprecation
			ibisLocalSender.setServiceName(listener.getServiceName());
		} else {
			ibisLocalSender.setJavaListener(listener.getName());
		}

		ibisLocalSender.setApplicationContext(configuration);
		ibisLocalSender.configure();
		return ibisLocalSender;
	}

	private static void registerWithServiceDispatcher(JavaListener<?> listener) throws ListenerException {
		ServiceDispatcher.getInstance().registerServiceClient(listener.getServiceName(), listener);
	}

	private Message createVirtualInputStream(long streamSize) {
		InputStream virtualInputStream = new VirtualInputStream(streamSize);
		return new Message(new ThrowingAfterCloseInputStream(virtualInputStream));
	}

	@ParameterizedTest(name = "Call via Dispatcher: {0}, Isolated: {1}, Synchronous: {2}")
	@CsvSource({
			"true, true, false",
			"false, true, false",
			"true, true, true",
			"false, true, true",
			"false, false, false",
			"true, false, false"
	})
	@DisplayName("Test IbisLocalSender.sendMessage()")
	void sendMessage(boolean callByServiceName, boolean callIsolated, boolean callSynchronous) throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration();
		configuration.stop();
		configuration.getAdapterManager().close();
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		TestPipe testPipe = createTestPipe(asyncCounterResult, asyncCompletionSemaphore);
		PipeLine pipeline = createPipeLine(testPipe, configuration);
		JavaListener<?> listener = setupJavaListener(configuration, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(configuration, listener, callByServiceName, callIsolated, callSynchronous);

		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState((Receiver<?>)listener.getHandler(), RunState.STARTED);
		ibisLocalSender.open();

		// Act
		PipeLineSession session = new PipeLineSession();
		PipeLineSession.updateListenerParameters(session, "m-id", "c-id", null, null);

		log.info("**>>> Calling Local Sender");
		Message message = createVirtualInputStream(EXPECTED_BYTE_COUNT);
		message.closeOnCloseOf(session, ibisLocalSender);
		SenderResult result = ibisLocalSender.sendMessage(message, session);

		long localCounterResult = countStreamSize(result.getResult());
		log.info("***>>> Done reading result message");
		boolean completedSuccess = asyncCompletionSemaphore.tryAcquire(10, TimeUnit.SECONDS);

		// Assert
		String msgPrefix = callByServiceName ? "Call via Dispatcher: " : "Call via JavaListener: ";
		assertAll(
			() -> assertTrue(completedSuccess, msgPrefix + "Async local sender should complete w/o error within at most 10 seconds"),
			() -> assertEquals(EXPECTED_BYTE_COUNT, localCounterResult, msgPrefix + "Local reader of message-stream should read " + EXPECTED_BYTE_COUNT + " bytes."),
			() -> assertEquals(EXPECTED_BYTE_COUNT, asyncCounterResult.get(), msgPrefix + "Async reader of message-stream should read " + EXPECTED_BYTE_COUNT + " bytes."),
			() -> assertNull(testPipe.recordedMessageId, msgPrefix + "Message ID should not be passed to nested session"),
			() -> assertEquals("c-id", testPipe.recordedCorrelationId, msgPrefix + "Correlation ID should be passed to nested session")
		);
	}

	@ParameterizedTest(name = "Call via Dispatcher: {0}")
	@CsvSource({
			"true",
			"false",
	})
	@DisplayName("Test IbisLocalSender.sendMessage()")
	void sendMessageAsync(boolean callByServiceName) throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration();
		configuration.stop();
		configuration.getAdapterManager().close();
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		TestPipe testPipe = createTestPipe(asyncCounterResult, asyncCompletionSemaphore);
		PipeLine pipeline = createPipeLine(testPipe, configuration);
		JavaListener<?> listener = setupJavaListener(configuration, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(configuration, listener, callByServiceName, true, false);

		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState((Receiver<?>)listener.getHandler(), RunState.STARTED);
		ibisLocalSender.open();

		// Act
		PipeLineSession session = new PipeLineSession();
		PipeLineSession.updateListenerParameters(session, "m-id", "c-id", null, null);

		log.info("**>>> Calling Local Sender");
		Message message = createVirtualInputStream(EXPECTED_BYTE_COUNT);
		message.closeOnCloseOf(session, ibisLocalSender);
		ibisLocalSender.sendMessage(message, session);

		session.close();

		log.info("***>>> Done reading result message");
		boolean completedSuccess = asyncCompletionSemaphore.tryAcquire(10, TimeUnit.SECONDS);

		// Assert
		String msgPrefix = callByServiceName ? "Call via Dispatcher: " : "Call via JavaListener: ";
		assertAll(
			() -> assertTrue(completedSuccess, msgPrefix + "Async local sender should complete w/o error within at most 10 seconds"),
			() -> assertEquals(EXPECTED_BYTE_COUNT, asyncCounterResult.get(), msgPrefix + "Async reader of message-stream should read " + EXPECTED_BYTE_COUNT + " bytes."),
			() -> assertNull(testPipe.recordedMessageId, msgPrefix + "Message ID should not be passed to nested session"),
			() -> assertEquals("c-id", testPipe.recordedCorrelationId, msgPrefix + "Correlation ID should be passed to nested session")
		);
	}

	@Test
	public void testSendMessageWithParamValuesAndReturnSessionKeys() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();

		try (PipeLineSession session = new PipeLineSession()) {
			session.put("my-parameter1", "parameter1-value");
			session.put("my-parameter2", Message.asMessage("parameter2-value"));
			Message message = new Message("my-parameter1");
			session.put("session-message", message);

			// Act
			SenderResult result = sender.sendMessage(message, session);

			// Assert
			assertAll(
				() -> assertEquals("parameter1-value", result.getResult().asString()),
				() -> assertTrue(session.containsKey("my-parameter1"), "After request the pipeline-session should contain key [my-parameter1]"),
				() -> assertEquals("parameter1-value", session.getString("my-parameter1")),
				() -> assertTrue(session.containsKey("my-parameter2"), "After request the pipeline-session should contain key [my-parameter2]"),
				() -> assertEquals("parameter2-value", session.getString("my-parameter2")),
				() -> assertTrue(session.containsKey("this-doesnt-exist"), "After request the pipeline-session should contain key [this-doesnt-exist]"),
				() -> assertNull(session.get("this-doesnt-exist"), "Key not in return from service should have value [NULL]"),
				() -> assertFalse(session.containsKey("key-not-configured-for-copy"), "Session should not contain key 'key-not-configured-for-copy'"),
				() -> assertEquals("my-parameter1", message.asString())
			);
		}
	}

	@Test
	public void testSendMessageReturnSessionKeysWhenNoneConfigured() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		sender.setReturnedSessionKeys(null);

		try (PipeLineSession session = new PipeLineSession()) {
			session.put("my-parameter1", "parameter1-value");
			Message message = new Message("my-parameter1");

			// Act
			SenderResult result = sender.sendMessage(message, session);

			// Assert
			assertAll(
				() -> assertEquals("parameter1-value", result.getResult().asString()),
				() -> assertTrue(session.containsKey("my-parameter1"), "After request the pipeline-session should contain key [my-parameter1]"),
				() -> assertEquals("parameter1-value", session.get("my-parameter1")),
				() -> assertFalse(session.containsKey("this-doesnt-exist"), "After request the pipeline-session should not contain key [this-doesnt-exist]"),
				() -> assertTrue(session.containsKey("key-not-configured-for-copy"), "Session should contain key 'key-not-configured-for-copy' b/c all keys should be copied")
			);
		}
	}

	@Test
	public void testSendMessageWithExitStateError() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();

		try (PipeLineSession session = new PipeLineSession()) {
			session.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, PipeLine.ExitState.ERROR);
			session.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, "400");
			Message message = new Message("my-parameter1");

			// Act / Assert
			SenderResult result = sender.sendMessage(message, session);

			assertFalse(result.isSuccess(), "Expected SenderResult to have error");
			assertEquals("400", result.getForwardName());
		}
	}

	@Test
	public void testSendMessageWithException() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageIsolatedWithException() throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration();
		IsolatedServiceCaller serviceCaller = configuration.createBean(IsolatedServiceCaller.class);

		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		sender.setIsolatedServiceCaller(serviceCaller);
		sender.setIsolated(true);
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageWithInvalidServiceName() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		sender.setThrowJavaListenerNotFoundException(true);
		sender.setServiceName("invalid");

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageWithInvalidServiceNameNoException() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		sender.setThrowJavaListenerNotFoundException(false);
		sender.setServiceName("invalid");

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			SenderResult result = sender.sendMessage(message, session);

			assertNull(result.getForwardName(), "ForwardName for this error expected to be null");
			assertFalse(result.isSuccess(), "Result not expected to be success");
			assertEquals("<error>No service with name [invalid] has been registered</error>", result.getResult().asString());
		}
	}

	private JavaListener<?> setupJavaListener(TestConfiguration configuration, PipeLine pipeline, boolean callByServiceName) throws Exception {
		Adapter adapter = configuration.createBean(Adapter.class);
		Receiver<String> receiver = new Receiver<>();
		JavaListener<String> listener = configuration.createBean(JavaListener.class);
		listener.setName("TEST");
		listener.setServiceName(SERVICE_NAME);
		receiver.setName("TEST");
		adapter.setName("TEST");

		if (callByServiceName) {
			registerWithServiceDispatcher(listener);
		}

		configuration.registerAdapter(adapter);

		adapter.registerReceiver(receiver);
		receiver.setListener(listener);
		receiver.setAdapter(adapter);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		listener.setHandler(receiver);

		adapter.setPipeLine(pipeline);

		listener.open();
		return listener;
	}

	private PipeLine createPipeLine(IPipe testPipe, TestConfiguration configuration) throws ConfigurationException {
		PipeLine pl = configuration.createBean(PipeLine.class);
		pl.setFirstPipe("read-stream");
		pl.addPipe(testPipe);
		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(PipeLine.ExitState.SUCCESS);
		pl.registerPipeLineExit(ple);
		CorePipeLineProcessor plp = new CorePipeLineProcessor();
		plp.setAdapterManager(configuration.getAdapterManager());
		plp.setPipeProcessor(new CorePipeProcessor());
		pl.setPipeLineProcessor(plp);
		return pl;
	}

	private TestPipe createTestPipe(final AtomicLong asyncCounterResult, final Semaphore asyncCompletionSemaphore) {
		TestPipe testPipe = new TestPipe(asyncCounterResult, asyncCompletionSemaphore);
		testPipe.setName("read-stream");
		return testPipe;
	}

	private static long countStreamSize(Message message) {
		long counter = 0;
		try(InputStream in = message.asInputStream()) {
			while (in.read() >= 0) {
				++counter;
				// Do a bit of sleep, to give other thread chance to
				// read a bit of stream too.
				Thread.yield();
			}
		} catch (Exception e) {
			throw new RuntimeException("Exception reading from message as stream", e);
		}
		return counter;
	}

	private static IbisLocalSender createIbisLocalSenderWithDummyServiceClient() throws ListenerException, ConfigurationException {
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			session.put("key-not-configured-for-copy", "dummy-value");
			session.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);
			session.scheduleCloseOnSessionExit(Message.asMessage(session.get("my-parameter1")), "param1");
			session.scheduleCloseOnSessionExit(Message.asMessage(session.get("my-parameter2")), "param2");
			return session.getMessage(message.asObject().toString());
		}));
		IbisLocalSender sender = new IbisLocalSender();
		sender.setServiceName(SERVICE_NAME);
		sender.setSynchronous(true);
		sender.setIsolated(false);
		sender.setReturnedSessionKeys("my-parameter1,this-doesnt-exist");

		addParameter("my-parameter1", sender);
		addParameter("my-parameter2", sender);
		addParameter(PipeLineSession.EXIT_STATE_CONTEXT_KEY, sender);
		addParameter(PipeLineSession.EXIT_CODE_CONTEXT_KEY, sender);

		return sender;
	}

	private static void addParameter(String name, IbisLocalSender sender) throws ConfigurationException {
		Parameter parameter = new Parameter(name, null);
		parameter.setSessionKey(name);
		parameter.configure();
		sender.addParameter(parameter);
	}

	private class TestPipe extends EchoPipe {
		private final AtomicLong asyncCounterResult;
		private final Semaphore asyncCompletionSemaphore;

		String recordedMessageId;
		String recordedCorrelationId;

		public TestPipe(final AtomicLong asyncCounterResult, final Semaphore asyncCompletionSemaphore) {
			this.asyncCounterResult = asyncCounterResult;
			this.asyncCompletionSemaphore = asyncCompletionSemaphore;
		}

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) {
			try {
				log.info("{}: start reading virtual stream", Thread.currentThread().getName());
				recordedMessageId = session.getMessageId();
				recordedCorrelationId = session.getCorrelationId();
				long counter = countStreamSize(message);
				asyncCounterResult.set(counter);
				// Return a stream from message which will be read by caller, testing that stream is not closed.
				return new PipeRunResult(getSuccessForward(), createVirtualInputStream(counter));
			} finally {
				asyncCompletionSemaphore.release();
				log.info("{}: pipe done and semaphore released", Thread.currentThread().getName());
			}
		}
	}
}
