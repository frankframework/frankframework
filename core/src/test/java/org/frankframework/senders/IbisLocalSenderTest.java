package org.frankframework.senders;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import nl.nn.adapterframework.dispatcher.DispatcherException;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.ForEachChildElementPipe;
import org.frankframework.receivers.JavaListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.VirtualInputStream;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

@Log4j2
class IbisLocalSenderTest {
	public static final String SERVICE_NAME = "TEST-SERVICE";

	public static final long EXPECTED_BYTE_COUNT = 1_000L;

	private TestConfiguration configuration;

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(configuration);
		// In case JavaListener didn't close after end of test, deregister the service.
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		try {
			DispatcherManagerFactory.getDispatcherManager().unregister(SERVICE_NAME);
		} catch (DispatcherException e) {
			// Ignore
		}
	}

	private static IbisLocalSender setupIbisLocalSender(Adapter adapter, JavaListener<?> listener, boolean callByServiceName, boolean callIsolated, boolean callSynchronous) throws ConfigurationException {
		IsolatedServiceCaller serviceCaller = SpringUtils.createBean(adapter);
		IbisLocalSender ibisLocalSender = SpringUtils.createBean(adapter);
		ibisLocalSender.setCheckDependency(true);
		ibisLocalSender.setIsolatedServiceCaller(serviceCaller);
		ibisLocalSender.setIsolated(callIsolated);
		// If not synchronous then isolated is always true, so actual value we set should depend on callIsolated
		ibisLocalSender.setSynchronous(!callIsolated || callSynchronous);

		if (callByServiceName) {
			//noinspection removal
			ibisLocalSender.setServiceName(listener.getServiceName());
		} else {
			ibisLocalSender.setJavaListener(listener.getName());
		}

		ibisLocalSender.setApplicationContext(adapter);
		ibisLocalSender.setName("IbisLocalSender");
		ibisLocalSender.configure();
		return ibisLocalSender;
	}

	private static void registerWithServiceDispatcher(JavaListener<?> listener) {
		ServiceDispatcher.getInstance().registerServiceClient(listener.getServiceName(), listener);
	}

	private static Message createVirtualInputStream(long streamSize) throws IOException {
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
		configuration = TransactionManagerType.DATASOURCE.create(false);
		Adapter adapter = createAdapter(configuration);
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		TestPipe testPipe = createTestPipe(asyncCounterResult, asyncCompletionSemaphore);
		PipeLine pipeline = createPipeLine(testPipe, adapter);
		JavaListener<?> listener = setupJavaListener(configuration, adapter, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(adapter, listener, callByServiceName, callIsolated, callSynchronous);

		startConfiguration(listener);
		ibisLocalSender.start();

		// Act
		PipeLineSession session = new PipeLineSession();
		String originalMessageId = "m-id";
		String originalCorrelationId = "c-id";
		PipeLineSession.updateListenerParameters(session, originalMessageId, originalCorrelationId);

		log.info("**>>> Calling Local Sender");
		Message message = createVirtualInputStream(EXPECTED_BYTE_COUNT);
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
			() -> assertNotEquals(originalMessageId, testPipe.recordedMessageId, msgPrefix + "Original Message ID should not be passed to nested session"),
			() -> assertEquals(originalCorrelationId, testPipe.recordedCorrelationId, msgPrefix + "Correlation ID should be passed to nested session")
		);
	}

	@ParameterizedTest(name = "Call via Dispatcher: {0}")
	@ValueSource(booleans = {true, false})
	@DisplayName("Test IbisLocalSender.sendMessage() Async")
	void sendMessageAsync(boolean callByServiceName) throws Exception {
		// Arrange
		configuration = TransactionManagerType.DATASOURCE.create(false);
		Adapter adapter = createAdapter(configuration);
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		TestPipe testPipe = createTestPipe(asyncCounterResult, asyncCompletionSemaphore);
		PipeLine pipeline = createPipeLine(testPipe, adapter);
		JavaListener<?> listener = setupJavaListener(configuration, adapter, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(adapter, listener, callByServiceName, true, false);

		startConfiguration(listener);
		ibisLocalSender.start();

		// Act
		PipeLineSession session = new PipeLineSession();
		String originalMessageId = "m-id";
		String originalCorrelationId = "c-id";
		PipeLineSession.updateListenerParameters(session, originalMessageId, originalCorrelationId, null, null);

		log.info("**>>> Calling Local Sender");
		Message message = createVirtualInputStream(EXPECTED_BYTE_COUNT);
		ibisLocalSender.sendMessage(message, session);

		session.close();

		log.info("***>>> Done reading result message");
		boolean completedSuccess = asyncCompletionSemaphore.tryAcquire(10, TimeUnit.SECONDS);

		// Assert
		String msgPrefix = callByServiceName ? "Call via Dispatcher: " : "Call via JavaListener: ";
		assertAll(
			() -> assertTrue(completedSuccess, msgPrefix + "Async local sender should complete w/o error within at most 10 seconds"),
			() -> assertEquals(EXPECTED_BYTE_COUNT, asyncCounterResult.get(), msgPrefix + "Async reader of message-stream should read " + EXPECTED_BYTE_COUNT + " bytes."),
			() -> assertNotEquals(originalMessageId, testPipe.recordedMessageId, msgPrefix + "Original Message ID should not be passed to nested session"),
			() -> assertEquals(originalCorrelationId, testPipe.recordedCorrelationId, msgPrefix + "Correlation ID should be passed to nested session")
		);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testSendMessageWithParamValuesAndReturnSessionKeys(boolean isolated) throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(isolated);

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("my-parameter1")) {

			session.put("my-parameter1", "parameter1-value");
			session.put("my-parameter2", new Message("parameter2-value"));
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
				() -> assertTrue(session.containsKey("key-to-copy"), "After request the pipeline-session should contain key [key-to-copy]"),
				() -> assertEquals("dummy-value", session.getString("key-to-copy"), "After request the value of [key-to-copy] in the pipelinesession should be [dummy-value]"),
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
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		sender.setReturnedSessionKeys(null);

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("my-parameter1")) {

			session.put("my-parameter1", "parameter1-value");

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
	public void testSendMessageWithParamsAndReturnSessionKeys() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		sender.setReturnedSessionKeys("*");

		Parameter parameter = new Parameter();
		parameter.setName("*");
		parameter.setSessionKey("*");
		parameter.configure();
		sender.addParameter(parameter);
		sender.configure();

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("mid")) {

			String originalMid = "original-main-adapter-mid";
			session.put(PipeLineSession.MESSAGE_ID_KEY, originalMid);
			session.put("my-parameter1", "parameter1-value");

			// Act
			// NB: The dummy service client will return the session-key from the sub-adapter-session that is in the incoming message. Here we retrieve session-key "mid"
			SenderResult result = sender.sendMessage(message, session);

			// Assert
			assertAll(
				() -> assertNotEquals(originalMid, result.getResult().asString()),
				() -> assertEquals(originalMid, session.get(PipeLineSession.MESSAGE_ID_KEY)),
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
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("my-parameter1")) {

			session.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, PipeLine.ExitState.ERROR);
			session.put(PipeLineSession.EXIT_CODE_CONTEXT_KEY, "400");

			// Act / Assert
			SenderResult result = sender.sendMessage(message, session);

			assertFalse(result.isSuccess(), "Expected SenderResult to have error");
			assertEquals("400", result.getForwardName());
		}
	}

	@Test
	public void testSendMessageWithException() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("MESSAGE")) {

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageIsolatedWithException() throws Exception {
		// Arrange
		configuration = new TestConfiguration();
		IsolatedServiceCaller serviceCaller = configuration.createBean();

		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		sender.setIsolatedServiceCaller(serviceCaller);
		sender.setIsolated(true);
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("MESSAGE")) {

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageWithInvalidServiceName() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		sender.setThrowJavaListenerNotFoundException(true);
		//noinspection removal
		sender.setServiceName("invalid");

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("MESSAGE")) {

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessage(message, session));
		}
	}

	@Test
	public void testSendMessageWithInvalidServiceNameNoException() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient(false);
		sender.setThrowJavaListenerNotFoundException(false);
		//noinspection removal
		sender.setServiceName("invalid");

		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("MESSAGE")) {

			// Act / Assert
			SenderResult result = sender.sendMessage(message, session);

			assertNull(result.getForwardName(), "ForwardName for this error expected to be null");
			assertFalse(result.isSuccess(), "Result not expected to be success");
			assertEquals("<error>No service with name [invalid] has been registered</error>", result.getResult().asString());
		}
	}

	@Test
	public void testIteratorPipeWithLocalSenderExitSuccess() throws Exception {
		// Arrange
		configuration = TransactionManagerType.DATASOURCE.create(false);
		Adapter adapter = createAdapter(configuration);
		EchoPipe pipe = configuration.createBean();
		pipe.setName("put-in-session-pipe");
		pipe.setStoreResultInSessionKey("my-session-key");
		PipeLine pipeline = createPipeLine(pipe, adapter);
		JavaListener<?> listener = setupJavaListener(configuration, adapter, pipeline, false);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(adapter, listener, false, false, true);
		ibisLocalSender.setReturnedSessionKeys("my-session-key,no-such-key");


		ForEachChildElementPipe iteratorPipe = SpringUtils.createBean(adapter);
		iteratorPipe.setName("iterator");
		iteratorPipe.setSender(ibisLocalSender);

		startConfiguration(listener);
		ibisLocalSender.start();
		iteratorPipe.start();

		// Act
		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("""
					<root>
					  <sub>
					  	<data/>
					  </sub>
					</root>
					""")) {
			session.put("my-session-key", null);

			PipeRunResult pipeRunResult = iteratorPipe.doPipe(message, session);

			// Assert

			String expectedResult = """
					<results>
					  <result item="1">
					    <sub>
					  	  <data/>
					    </sub>
					  </result>
					</results>
					""";
			String expectedSessionKeyValue = """
					<sub>
					  <data/>
					</sub>
					""";

			assertAll(
					() -> assertXmlEquals(expectedSessionKeyValue, session.getString("my-session-key")),
					() -> assertTrue(session.containsKey("no-such-key"), "After request the pipeline-session should contain key [no-such-key]"),
					() -> assertNull(session.getString("no-such-key")),
					() -> assertXmlEquals(expectedResult, pipeRunResult.getResult().asString())
			);
		}
	}

	@Test
	public void testIteratorPipeWithLocalSenderExitError() throws Exception {
		// Arrange
		configuration = TransactionManagerType.DATASOURCE.create(false);
		Adapter adapter = createAdapter(configuration);
		EchoPipe pipe = configuration.createBean();
		pipe.setName("put-in-session-pipe");
		pipe.setStoreResultInSessionKey("my-session-key");
		pipe.addForward(new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, "error"));
		pipe.addForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME, "error"));
		PipeLine pipeline = createPipeLine(pipe, adapter);
		JavaListener<?> listener = setupJavaListener(configuration, adapter, pipeline, false);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(adapter, listener, false, false, true);
		ibisLocalSender.setReturnedSessionKeys("my-session-key,no-such-key");


		ForEachChildElementPipe iteratorPipe = SpringUtils.createBean(adapter);
		iteratorPipe.setName("iterator");
		iteratorPipe.setSender(ibisLocalSender);

		startConfiguration(listener);
		ibisLocalSender.start();
		iteratorPipe.start();

		// Act
		try (PipeLineSession session = new PipeLineSession();
			 Message message = new Message("""
					<root>
					  <sub>
					  	<data/>
					  </sub>
					</root>
					""")) {
			session.put("my-session-key", null);

			PipeRunException pre = assertThrows(PipeRunException.class, () -> iteratorPipe.doPipe(message, session));

			// Assert

			String expectedSessionKeyValue = """
					<sub>
					  <data/>
					</sub>
					""";

			assertAll(
					() -> assertXmlEquals(expectedSessionKeyValue, session.getString("my-session-key")),
					() -> assertTrue(session.containsKey("no-such-key"), "After request the pipeline-session should contain key [no-such-key]"),
					() -> assertNull(session.getString("no-such-key")),
					() -> assertThat(pre.getMessage(), containsString("exitState=ERROR"))
			);
		}
	}

	private void startConfiguration(JavaListener<?> listener) throws ConfigurationException {
		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState((Receiver<?>) listener.getHandler(), RunState.STARTED);
	}

	private static Adapter createAdapter(TestConfiguration configuration) {
		Adapter adapter = configuration.createBean();
		adapter.setName("TEST");
		configuration.addAdapter(adapter);
		return adapter;
	}

	private static JavaListener<?> setupJavaListener(TestConfiguration configuration, Adapter adapter, PipeLine pipeline, boolean callByServiceName) {
		Receiver<String> receiver = SpringUtils.createBean(adapter);
		JavaListener<String> listener = SpringUtils.createBean(adapter);
		listener.setName("TEST");
		listener.setServiceName(SERVICE_NAME);
		receiver.setName("TEST");

		if (callByServiceName) {
			registerWithServiceDispatcher(listener);
		}

		adapter.addReceiver(receiver);
		receiver.setListener(listener);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		listener.setHandler(receiver);

		adapter.setPipeLine(pipeline);

		listener.start();
		return listener;
	}

	private static PipeLine createPipeLine(IPipe testPipe, Adapter adapter) throws ConfigurationException {
		PipeLine pl = SpringUtils.createBean(adapter);
		pl.setFirstPipe(testPipe.getName());
		pl.addPipe(testPipe);
		PipeLineExit success = new PipeLineExit();
		success.setName("success");
		success.setState(PipeLine.ExitState.SUCCESS);
		pl.addPipeLineExit(success);
		PipeLineExit error = new PipeLineExit();
		error.setName("error");
		error.setState(PipeLine.ExitState.ERROR);
		pl.addPipeLineExit(error);
		return pl;
	}

	private static TestPipe createTestPipe(final AtomicLong asyncCounterResult, final Semaphore asyncCompletionSemaphore) {
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

	private IbisLocalSender createIbisLocalSenderWithDummyServiceClient(boolean isolated) throws ConfigurationException {
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((message, session) -> {
			session.put("key-not-configured-for-copy", "dummy-value");
			session.put("key-to-copy", "dummy-value");
			session.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);
			try {
				return session.getMessage(message.asString());
			} catch (IOException e) {
				throw new ListenerException(e);
			}
		}));
		IbisLocalSender sender = new IbisLocalSender();
		//noinspection removal
		sender.setServiceName(SERVICE_NAME);
		sender.setSynchronous(true);
		sender.setIsolated(isolated);
		sender.setReturnedSessionKeys("my-parameter1,key-to-copy,this-doesnt-exist");
		if (isolated) {
			configuration = new TestConfiguration(false);
			IsolatedServiceCaller isolatedServiceCaller = configuration.createBean();
			sender.setIsolatedServiceCaller(isolatedServiceCaller);
		}

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

	private static class TestPipe extends EchoPipe {
		private final AtomicLong asyncCounterResult;
		private final Semaphore asyncCompletionSemaphore;

		String recordedMessageId;
		String recordedCorrelationId;

		public TestPipe(final AtomicLong asyncCounterResult, final Semaphore asyncCompletionSemaphore) {
			this.asyncCounterResult = asyncCounterResult;
			this.asyncCompletionSemaphore = asyncCompletionSemaphore;
		}

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			try {
				log.info("{}: start reading virtual stream", Thread.currentThread().getName());
				// Often this assert is done in PipeLineProcessor but they're not part of the test-spring-configuration, and it is important to make this assertion
				message.assertNotClosed();
				recordedMessageId = session.getMessageId();
				recordedCorrelationId = session.getCorrelationId();
				long counter = countStreamSize(message);
				asyncCounterResult.set(counter);
				// Return a stream from message which will be read by caller, testing that stream is not closed.
				return new PipeRunResult(getSuccessForward(), createVirtualInputStream(counter));
			} catch (IOException e) {
				throw new PipeRunException(this, "Cannot read stream", e);
			} finally {
				asyncCompletionSemaphore.release();
				log.info("{}: pipe done and semaphore released", Thread.currentThread().getName());
			}
		}
	}
}
