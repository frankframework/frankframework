package nl.nn.adapterframework.senders;


import static org.junit.Assert.assertEquals;
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
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.IsolatedServiceCaller;
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

	private static IbisLocalSender setupIbisLocalSender(TestConfiguration configuration, JavaListener listener, boolean callByServiceName, boolean callSynchronous) {
		IsolatedServiceCaller serviceCaller = configuration.createBean(IsolatedServiceCaller.class);
		IbisLocalSender ibisLocalSender = configuration.createBean(IbisLocalSender.class);
		ibisLocalSender.setIsolatedServiceCaller(serviceCaller);
		ibisLocalSender.setIsolated(true);
		ibisLocalSender.setSynchronous(callSynchronous);

		if (callByServiceName) {
			ibisLocalSender.setServiceName(listener.getServiceName());
		} else {
			ibisLocalSender.setJavaListener(listener.getName());
		}
		return ibisLocalSender;
	}

	private static void registerWithServiceDispatcher(JavaListener listener) throws ListenerException {
		ServiceClient serviceClient = (correlationId, message, session) -> {
			try {
				return new Message(listener.processRequest(correlationId, message.asString(), session));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
		ServiceDispatcher.getInstance().registerServiceClient(listener.getServiceName(), serviceClient);
	}

	private Message createVirtualInputStream() {
		InputStream virtualInputStream = new InputStream() {
			LongAdder bytesRead = new LongAdder();

			@Override
			public int read() throws IOException {
				if (bytesRead.longValue() >= EXPECTED_BYTE_COUNT) {
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
		sendMessageAsync(true);
	}
	@Test
	public void sendMessageAsync() throws Exception {
		sendMessageAsync(false);
	}

	private void sendMessageAsync(boolean callByServiceName) throws Exception {
		// Arrange
		TestConfiguration configuration = new TestConfiguration();
		configuration.stop();
		configuration.getAdapterManager().close();
		AtomicLong asyncCounterResult = new AtomicLong();
		Semaphore asyncCompletionSemaphore = new Semaphore(0);

		PipeLine pipeline = createPipeLine(configuration, asyncCounterResult, asyncCompletionSemaphore);
		JavaListener listener = setupJavaListener(configuration, pipeline, callByServiceName);
		IbisLocalSender ibisLocalSender = setupIbisLocalSender(configuration, listener, callByServiceName, false);

		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState((Receiver<?>)listener.getHandler(), RunState.STARTED);

		// Act
		PipeLineSession session = new PipeLineSession();
		log.info("**>>> Calling Local Sender");
		Message result = ibisLocalSender.sendMessage(createVirtualInputStream(), session);

		long localCounterResult = countStreamSize(result);
		log.info("***>>> Done reading result message");
		boolean completedSuccess = asyncCompletionSemaphore.tryAcquire(10, TimeUnit.SECONDS);

		// Assert
		assertTrue("Async local sender should complete w/o error within at most 10 seconds", completedSuccess);
		assertEquals(EXPECTED_BYTE_COUNT, localCounterResult);
		assertEquals(EXPECTED_BYTE_COUNT, asyncCounterResult.get());
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
					return new PipeRunResult(getSuccessForward(), counter);
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
			throw new RuntimeException("Exception running Pipe", e);
		}
		return counter;
	}
}
