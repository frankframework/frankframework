package org.frankframework.receivers;

import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import nl.nn.adapterframework.dispatcher.DispatcherException;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IPipe;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineExits;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.jdbc.MessageStoreListener;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.RunState;

@Log4j2
public class ReceiverSubAdapterTest {

	public static final String SERVICE_NAME = "FAILING_TEST_SERVICE";

	private TestConfiguration configuration;

	@BeforeEach
	void setup() {
		// Create a TestConfiguration and stop it so that we can configure new adapters in it
		configuration = new TestConfiguration(false);
	}

	@AfterEach
	void tearDown() {
		log.info("*>>> Test done, wrapping up and destroying");
		// In case JavaListener didn't close after end of test, deregister the service.
		ServiceDispatcher.getInstance().unregisterServiceClient(SERVICE_NAME);
		try {
			DispatcherManagerFactory.getDispatcherManager().unregister(SERVICE_NAME);
		} catch (DispatcherException e) {
			// Ignore
		}
	}

	private FailurePipe createFailurePipe() throws ConfigurationException {
		return new FailurePipe();
	}

	private PipeLine createPipeLine(TestConfiguration configuration, IPipe testPipe) throws ConfigurationException {
		PipeLine pl = configuration.createBean(PipeLine.class);
		pl.setFirstPipe(testPipe.getName());
		pl.addPipe(testPipe);
		PipeLineExit success = new PipeLineExit();
		success.setName(PipeForward.SUCCESS_FORWARD_NAME);
		success.setState(PipeLine.ExitState.SUCCESS);

		PipeLineExit failure = new PipeLineExit();
		failure.setName(PipeForward.EXCEPTION_FORWARD_NAME);
		failure.setState(PipeLine.ExitState.ERROR);

		PipeLineExits exits = new PipeLineExits();
		exits.addPipeLineExit(success);
		exits.addPipeLineExit(failure);
		pl.setPipeLineExits(exits);

		CorePipeLineProcessor plp = new CorePipeLineProcessor();
		plp.setPipeProcessor(new CorePipeProcessor());
		pl.setPipeLineProcessor(plp);
		return pl;
	}

	private JavaListener<Serializable> createJavaListener(TestConfiguration configuration, Receiver<Serializable> receiver) {
		@SuppressWarnings("unchecked")
		JavaListener<Serializable> listener = configuration.createBean(JavaListener.class);
		listener.setName(receiver.getName());
		listener.setHandler(receiver);

		receiver.setListener(listener);
		receiver.setTxManager(configuration.createBean(NarayanaJtaTransactionManager.class));

		listener.start();
		return listener;
	}

	private Receiver<Serializable> createReceiver(TestConfiguration configuration, PipeLine pipeline, String name, NarayanaJtaTransactionManager txManager) throws Exception {
		Adapter adapter = configuration.createBean(Adapter.class);
		@SuppressWarnings("unchecked")
		Receiver<Serializable> receiver = configuration.createBean(Receiver.class);
		receiver.setName(name);
		adapter.setName(name);

		configuration.addAdapter(adapter);

		adapter.addReceiver(receiver);

		receiver.setApplicationContext(configuration);
		receiver.setAdapter(adapter);
		receiver.setTxManager(txManager);
		adapter.setPipeLine(pipeline);
		return receiver;
	}

	private MessageSendingPipe createMessageSendingPipe(TestConfiguration configuration, JavaListener<?> javaListener) {
		IbisLocalSender sender = new IbisLocalSender();
		sender.setIsolated(false);
		sender.setSynchronous(true);
		sender.setJavaListener(javaListener.getName());
		sender.setName("send a message");
		sender.setApplicationContext(configuration);

		// Calls to sub-adapter will get a randomly generated message-id, but here we
		// hardcode a parameter with same name as the session uses to store the message-id.
		// Therefore each call to the sub-adapter will use the same message id, and the Frank!Framework
		// keeps track of the failure of each attempt internally until max-retries is reached.
		Parameter idParam = new Parameter(PipeLineSession.MESSAGE_ID_KEY, "my-id");
		sender.addParameter(idParam);

		SenderPipe pipe = new SenderPipe();
		pipe.setSender(sender);
		pipe.setName("send a message");
		return pipe;
	}

	private IListener<Serializable> createMockListener(Receiver<Serializable> receiver) throws ListenerException {
		MessageStoreListener listener = mock();
		receiver.setListener(listener);

		when(listener.extractMessage(any(), any())).thenCallRealMethod();
		when(listener.getName()).thenReturn(receiver.getName());
		when(listener.getApplicationContext()).thenReturn(receiver.getApplicationContext());
		when(listener.getConfigurationClassLoader()).thenReturn(receiver.getConfigurationClassLoader());

		return listener;
	}

	private ITransactionalStorage<Serializable> createMockErrorStorage(Receiver<?> receiver) throws Exception {
		ITransactionalStorage<Serializable> errorStorage = mock();

		Map<String, Serializable> storedMessages = new HashMap<>();
		when(errorStorage.storeMessage(any(), any(), any(), any(), any(), any())).thenAnswer(params -> {
			String msgId = params.getArgument(0);
			Serializable message = params.getArgument(5);

			storedMessages.put(msgId, message);
			return msgId;
		});
		when(errorStorage.getMessage(any())).thenAnswer(params -> {
			String msgId = params.getArgument(0);
			Serializable message = storedMessages.get(msgId);
			if (message instanceof MessageWrapper<?> messageWrapper) {
				return messageWrapper;
			}
			return new RawMessageWrapper<>(message);
		});

		receiver.setErrorStorage(errorStorage);
		return errorStorage;
	}

	@Test
	void testManualRetryWithSubAdapter() throws Exception {
		// Arrange
		NarayanaJtaTransactionManager txManager = configuration.createBean(NarayanaJtaTransactionManager.class);

		FailurePipe failurePipe = createFailurePipe();
		PipeLine subAdapterPipeLine = createPipeLine(configuration, failurePipe);
		Receiver<Serializable> subAdapterReceiver = createReceiver(configuration, subAdapterPipeLine, "TEST-FAIL", txManager);
		JavaListener<Serializable> javaListener = createJavaListener(configuration, subAdapterReceiver);
		subAdapterReceiver.setMaxRetries(1);

		MessageSendingPipe messageSendingPipe = createMessageSendingPipe(configuration, javaListener);
		PipeLine mainAdapterPipeLine = createPipeLine(configuration, messageSendingPipe);
		Receiver<Serializable> mainAdapterReceiver = createReceiver(configuration, mainAdapterPipeLine, "TEST", txManager);

		ITransactionalStorage<Serializable> mockErrorStorage = createMockErrorStorage(mainAdapterReceiver);
		IListener<Serializable> mockListener = createMockListener(mainAdapterReceiver);

		log.info("*>>> Starting Configuration");
		configuration.configure();
		configuration.start();

		waitForState(mainAdapterReceiver, RunState.STARTED);
		waitForState(subAdapterReceiver, RunState.STARTED);

		RawMessageWrapper<Serializable> rawMessage = new RawMessageWrapper<>("TEST MESSAGE", "msg-id", "cid");

		// Act 1 -- initial message

		log.info("*>>> Starting Tests, Run 1");
		try (PipeLineSession session = new PipeLineSession()) {
			mainAdapterReceiver.processRawMessage(mockListener, rawMessage, session, false);
		}
		// Assert
		verify(mockErrorStorage, times(1)).storeMessage(any(), any(), any(), any(), any(), any());

		// Act 2 -- 1st manual retry
		log.info("*>>> Retrying message; Run 2");
		mainAdapterReceiver.retryMessage("msg-id");

		// Act 3 -- another manual retry
		// B/c of maxRetries=1 on the sub-adapter this would be rejected without fix for #5699.
		log.info("*>>> Retrying message; Run 3");
		failurePipe.doFail = false;
		mainAdapterReceiver.retryMessage("msg-id");

		// Assert
		assertAll(
				() -> assertEquals(3, failurePipe.runs.get()),
				() -> assertEquals(2, failurePipe.fails.get()),
				() -> assertEquals(1, failurePipe.succeeds.get())
		);

		// Afterwards
		log.info("*>>> Test done, success");
	}

	private static class FailurePipe extends FixedForwardPipe {
		private final PipeForward failureForward = new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME, PipeForward.EXCEPTION_FORWARD_NAME);
		private final PipeForward successForward = new PipeForward(PipeForward.SUCCESS_FORWARD_NAME, PipeForward.SUCCESS_FORWARD_NAME);

		boolean doFail = true;

		AtomicInteger runs = new AtomicInteger();
		AtomicInteger fails = new AtomicInteger();
		AtomicInteger succeeds = new AtomicInteger();

		FailurePipe() throws ConfigurationException {
			addForward(failureForward);
			addForward(successForward);
			setName("fail");
		}

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			runs.incrementAndGet();
			int result = doFail ? fails.incrementAndGet() : succeeds.incrementAndGet();
			return new PipeRunResult(doFail ? failureForward : successForward, result);
		}
	}
}
