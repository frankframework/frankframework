package nl.nn.adapterframework.senders;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IManagable;
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
import nl.nn.adapterframework.receivers.ServiceClient;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.RunState;

public class IbisLocalSenderTest {
	public static final String SERVICE_NAME = "TEST-SERVICE";
	private Logger log = LogManager.getLogger(this);

	public static final long EXPECTED_BYTE_COUNT = 1_000L;

	@After
	public void tearDown() {
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
	}

	private static IbisLocalSender setupIbisLocalSender(TestConfiguration configuration, JavaListener listener, boolean callByServiceName, boolean callIsolated, boolean callSynchronous) throws ConfigurationException {
		IsolatedServiceCaller serviceCaller = configuration.createBean(IsolatedServiceCaller.class);
		IbisLocalSender ibisLocalSender = configuration.createBean(IbisLocalSender.class);
		ibisLocalSender.setCheckDependency(true);
		ibisLocalSender.setIsolatedServiceCaller(serviceCaller);
		ibisLocalSender.setIsolated(callIsolated);
		// If not synchronous then isolated is always true, so actual value we set should depend on callIsolated
		ibisLocalSender.setSynchronous(!callIsolated || callSynchronous);

		if (callByServiceName) {
			ibisLocalSender.setServiceName(listener.getServiceName());
		} else {
			ibisLocalSender.setJavaListener(listener.getName());
		}

		ibisLocalSender.setApplicationContext(configuration);
		ibisLocalSender.configure();
		return ibisLocalSender;
	}

	private static void registerWithServiceDispatcher(JavaListener listener) throws ListenerException {
		ServiceClient serviceClient = listener::processRequest;
		ServiceDispatcher.getInstance().registerServiceClient(listener.getServiceName(), serviceClient);
	}

	private Message createVirtualInputStream(long streamSize) {
		InputStream virtualInputStream = new InputStream() {
			LongAdder bytesRead = new LongAdder();

			@Override
			public int read() throws IOException {
				if (bytesRead.longValue() >= streamSize) {
					log.info("{}: VirtualInputStream EOF after {} bytes", Thread.currentThread().getName(), bytesRead.longValue());
					return -1;
				}
				bytesRead.increment();
				Thread.yield();
				return 1;
			}
		};

		return new Message(virtualInputStream);
	}

	@Test
	public void sendMessageAsyncByServiceName() throws Exception {
		sendMessage(true, true, false);
	}
	@Test
	public void sendMessageAsync() throws Exception {
		sendMessage(false, true, false);
	}

	@Test
	public void sendMessageSynchronousByServiceName() throws Exception {
		sendMessage(true, true, true);
	}

	@Test
	public void sendMessageSynchronous() throws Exception {
		sendMessage(false, true, true);
	}

	@Test
	public void sendMessageNotIsolated() throws Exception {
		sendMessage(false, false, false);
	}

	@Test
	public void sendMessageNotIsolatedByServiceName() throws Exception {
		sendMessage(true, false, false);
	}

	private void sendMessage(boolean callByServiceName, boolean callIsolated, boolean callSynchronous) throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration();
		configuration.stop();
		configuration.getAdapterManager().close();
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		PipeLine pipeline = createPipeLine(configuration, asyncCounterResult, asyncCompletionSemaphore);
		JavaListener listener = setupJavaListener(configuration, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(configuration, listener, callByServiceName, callIsolated, callSynchronous);

		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState((Receiver<?>)listener.getHandler(), RunState.STARTED);
		ibisLocalSender.open();

		// Act
		PipeLineSession session = new PipeLineSession();
		log.info("**>>> Calling Local Sender");
		Message result = ibisLocalSender.sendMessage(createVirtualInputStream(EXPECTED_BYTE_COUNT), session);

		long localCounterResult = countStreamSize(result);
		log.info("***>>> Done reading result message");
		boolean completedSuccess = asyncCompletionSemaphore.tryAcquire(10, TimeUnit.SECONDS);

		// Assert
		assertTrue("Async local sender should complete w/o error within at most 10 seconds", completedSuccess);
		assertEquals(EXPECTED_BYTE_COUNT, localCounterResult);
		assertEquals(EXPECTED_BYTE_COUNT, asyncCounterResult.get());
	}

	@Test
	public void testSendMessageWithParamValuesAndReturnSessionKeys() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();

		try (PipeLineSession session = new PipeLineSession()) {
			session.put("my-parameter", "parameter-value");
			Message message = new Message("my-parameter");

			// Act
			SenderResult result = sender.sendMessageAndProvideForwardName(message, session);

			// Assert
			assertEquals("parameter-value", result.getResult().asString());
			assertTrue("After request the pipeline-session should contain key [my-parameter]", session.containsKey("my-parameter"));
			assertEquals("parameter-value", session.get("my-parameter"));
			assertTrue("After request the pipeline-session should not contain key [this-doesnt-exist]", session.containsKey("this-doesnt-exist"));
			assertNull("Key not in return from service should have value [NULL]", session.get("this-doesnt-exist"));
		}
	}

	@Test
	public void testSendMessageWithExitStateError() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();

		try (PipeLineSession session = new PipeLineSession()) {
			session.put(PipeLineSession.EXIT_STATE_CONTEXT_KEY, PipeLine.ExitState.ERROR);
			Message message = new Message("my-parameter");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessageAndProvideForwardName(message, session));
		}
	}

	@Test
	public void testSendMessageWithException() throws Exception {
		// Arrange
		IbisLocalSender sender = createIbisLocalSenderWithDummyServiceClient();
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((correlationId, message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessageAndProvideForwardName(message, session));
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
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((correlationId, message, session) -> {
			throw new ListenerException("TEST");
		}));

		try (PipeLineSession session = new PipeLineSession()) {
			Message message = new Message("MESSAGE");

			// Act / Assert
			assertThrows(SenderException.class, () -> sender.sendMessageAndProvideForwardName(message, session));
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
			assertThrows(SenderException.class, () -> sender.sendMessageAndProvideForwardName(message, session));
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
			SenderResult result = sender.sendMessageAndProvideForwardName(message, session);

			assertEquals("error", result.getForwardName());
			assertEquals("<error>No service with name [invalid] has been registered</error>", result.getResult().asString());
		}
	}

	public void waitForState(IManagable object, RunState state) {
		while(object.getRunState()!=state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
	}

	private JavaListener setupJavaListener(TestConfiguration configuration, PipeLine pipeline, boolean callByServiceName) throws Exception {
		Adapter adapter = configuration.createBean(Adapter.class);
		Receiver<String> receiver = new Receiver<>();
		JavaListener listener = configuration.createBean(JavaListener.class);
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

	private PipeLine createPipeLine(TestConfiguration configuration, AtomicLong asyncCounterResult, Semaphore asyncCompletionSemaphore) throws ConfigurationException {
		IPipe testPipe = new EchoPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) {
				try {
					log.info("{}: start reading virtual stream", Thread.currentThread().getName());
					long counter = countStreamSize(message);
					asyncCounterResult.set(counter);
					// Return a stream from message which will be read by caller, testing that stream is not closed.
					return new PipeRunResult(getSuccessForward(), createVirtualInputStream(counter));
				} finally {
					asyncCompletionSemaphore.release();
					log.info("{}: pipe done and semaphore released", Thread.currentThread().getName());
				}
			}
		};
		testPipe.setName("read-stream");
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
		ServiceDispatcher.getInstance().registerServiceClient(SERVICE_NAME, ((correlationId, message, session) -> session.getMessage(message.asObject().toString())));
		IbisLocalSender sender = new IbisLocalSender();
		sender.setServiceName(SERVICE_NAME);
		sender.setSynchronous(true);
		sender.setIsolated(false);
		sender.setReturnedSessionKeys("my-parameter,this-doesnt-exist");

		Parameter parameter = new Parameter("my-parameter", null);
		parameter.setSessionKey("my-parameter");
		parameter.configure();
		sender.addParameter(parameter);

		Parameter exitStateParameter = new Parameter(PipeLineSession.EXIT_STATE_CONTEXT_KEY, null);
		exitStateParameter.setSessionKey(PipeLineSession.EXIT_STATE_CONTEXT_KEY);
		exitStateParameter.configure();
		sender.addParameter(exitStateParameter);

		return sender;
	}
}
