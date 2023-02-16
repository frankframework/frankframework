package nl.nn.adapterframework.receivers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jms.Destination;
import javax.jms.TextMessage;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TransactionAttribute;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.jta.btm.BtmJtaTransactionManager;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunState;

public class ReceiverTest {
	public static final DefaultTransactionDefinition TRANSACTION_DEFINITION = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	protected Logger log = LogUtil.getLogger(this);
	private TestConfiguration configuration = new TestConfiguration();

	@BeforeEach
	public void setUp() throws Exception {
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
	}

	public SlowListenerBase setupSlowStartPullingListener(int startupDelay) {
		return createSlowListener(SlowPullingListener.class, startupDelay, 0);
	}

	public SlowListenerBase setupSlowStartPushingListener(int startupDelay) {
		return createSlowListener(SlowPushingListener.class, startupDelay, 0);
	}

	public SlowListenerBase setupSlowStopPullingListener(int shutdownDelay) {
		return createSlowListener(SlowPullingListener.class, 0, shutdownDelay);
	}

	public SlowListenerBase setupSlowStopPushingListener(int shutdownDelay) {
		return createSlowListener(SlowPushingListener.class, 0, shutdownDelay);
	}

	protected <T extends SlowListenerBase> T createSlowListener(Class<T> cls, int startupDelay, int shutdownDelay) {
		T listener = configuration.createBean(cls);
		listener.setStartupDelay(startupDelay);
		listener.setShutdownDelay(shutdownDelay);
		return listener;
	}

	public Receiver<javax.jms.Message> setupReceiver(IListener<javax.jms.Message> listener) throws Exception {
		@SuppressWarnings("unchecked")
		Receiver<javax.jms.Message> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	public Adapter setupAdapter(Receiver<javax.jms.Message> receiver) throws Exception {

		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("ReceiverTestAdapterName");

		PipeLine pl = spy(new PipeLine());
		doAnswer(p -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setState(ExitState.SUCCESS);
			plr.setResult(p.getArgument(1));
			return plr;
		}).when(pl).process(anyString(), any(nl.nn.adapterframework.stream.Message.class), any(PipeLineSession.class));
		pl.setFirstPipe("dummy");

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(ExitState.SUCCESS);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.registerReceiver(receiver);
		configuration.registerAdapter(adapter);
		return adapter;
	}

	public void waitWhileInState(IManagable object, RunState state) {
		while(object.getRunState()==state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
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

	public static Stream<Arguments> testJmsMessageWithHighDeliveryCount() {
		return Stream.of(
			Arguments.of(NarayanaJtaTransactionManager.class),
			Arguments.of(BtmJtaTransactionManager.class)
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testJmsMessageWithHighDeliveryCount(Class<? extends JtaTransactionManager> txManagerClass) throws Exception {
		// Arrange
		EsbJmsListener listener = spy(configuration.createBean(EsbJmsListener.class));
		doReturn(mock(Destination.class)).when(listener).getDestination();
		doNothing().when(listener).open();

		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);

		Field messagingSourceField = JMSFacade.class.getDeclaredField("messagingSource");
		messagingSourceField.setAccessible(true);
		MessagingSource messagingSource = mock(MessagingSource.class);
		messagingSourceField.set(listener, messagingSource);

		@SuppressWarnings("unchecked")
		IListenerConnector<javax.jms.Message> jmsConnectorMock = mock(IListenerConnector.class);
		listener.setJmsConnector(jmsConnectorMock);
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);

		final JtaTransactionManager txManager = configuration.createBean(txManagerClass);
		txManager.setDefaultTimeout(1);

		receiver.setTxManager(txManager); // MockTXManager here to check if no new TX was started for processing?
		receiver.setTransactionAttribute(TransactionAttribute.REQUIRED);

		// assume there was no connectivity, the message was not able to be stored in the database, retryInterval keeps increasing.
		final Field retryIntervalField = Receiver.class.getDeclaredField("retryInterval");
		retryIntervalField.setAccessible(true);
		retryIntervalField.set(receiver, 2);

		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		TextMessage jmsMessage = mock(TextMessage.class);
		doReturn("dummy-message-id").when(jmsMessage).getJMSMessageID();
		doReturn(receiver.getMaxDeliveries() + 1).when(jmsMessage).getIntProperty("JMSXDeliveryCount");
		doReturn(Collections.emptyEnumeration()).when(jmsMessage).getPropertyNames();
		doReturn("message").when(jmsMessage).getText();


		final int NR_TIMES_MESSAGE_OFFERED = 5;
		final AtomicInteger rolledBackTXCounter = new AtomicInteger();
		final AtomicInteger txRollbackOnlyInErrorStorage = new AtomicInteger();
		final AtomicInteger movedToErrorStorage = new AtomicInteger();
		final AtomicInteger exceptionsFromReceiver = new AtomicInteger();
		final AtomicInteger processedNoException = new AtomicInteger();
		final AtomicInteger txNotCompletedAfterReceiverEnds = new AtomicInteger();

		final Semaphore semaphore = new Semaphore(0);
		Thread mockListenerThread = new Thread("mock-listener-thread") {
			@SneakyThrows
			@Override
			public void run() {
				try {
					int nrTries = 0;
					while (nrTries++ < NR_TIMES_MESSAGE_OFFERED) {
						final TransactionStatus tx = txManager.getTransaction(TRANSACTION_DEFINITION);
						reset(errorStorage, listener);
						when(errorStorage.storeMessage(any(), any(), any(), any(), any(), any()))
							.thenAnswer(invocation -> {
								if (tx.isRollbackOnly()) {
									txRollbackOnlyInErrorStorage.incrementAndGet();
									throw new SQLException("TX is rollback-only. Getting out!");
								}
								int count = movedToErrorStorage.incrementAndGet();
								return "" + count;
							});
						try (PipeLineSession session = new PipeLineSession()) {
							receiver.processRawMessage(listener, jmsMessage, session, false);
							processedNoException.incrementAndGet();
						} catch (Exception e) {
							log.warn("Caught exception in Receiver:", e);
							exceptionsFromReceiver.incrementAndGet();
						} finally {
							if (tx.isRollbackOnly()) {
								rolledBackTXCounter.incrementAndGet();
							} else {
								log.warn("I had expected TX to be marked for rollback-only by now?");
							}
							if (!tx.isCompleted()) {
								// We do rollback inside the Receiver already but if the TX is aborted
								/// it never seems to be marked "Completed" by Narayana.
								txNotCompletedAfterReceiverEnds.incrementAndGet();
								txManager.rollback(tx);
							}
							retryIntervalField.set(receiver, 2); // To avoid test taking too long.
						}
					}
				} finally {
					semaphore.release();
				}
			}
		};

		// Act
		mockListenerThread.start();
		semaphore.acquire(); // Wait until thread is finished.

		// Assert
		assertAll(
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, rolledBackTXCounter.get(), "Mismatch in nr of messages marked for rollback by TX manager"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, processedNoException.get(), "Mismatch in nr of messages processed without exception from receiver"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, txRollbackOnlyInErrorStorage.get(), "Mismatch in nr of transactions already marked rollback-only while moving to error storage."),
			() -> assertEquals(0, exceptionsFromReceiver.get(), "Mismatch in nr of exceptions from Receiver method"),
			() -> assertEquals(0, movedToErrorStorage.get(), "Mismatch in nr of messages moved to error storage"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, txNotCompletedAfterReceiverEnds.get(), "Mismatch in nr of transactions not completed after receiver finishes")
		);
	}

	@Test
	public void testPullingReceiverStartBasic() throws Exception {
		testStartNoTimeout(setupSlowStartPullingListener(0));
	}

	@Test
	public void testPushingReceiverStartBasic() throws Exception {
		testStartNoTimeout(setupSlowStartPushingListener(0));
	}

	public void testStartNoTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.
		log.info("Receiver RunState "+receiver.getRunState());

		assertFalse(listener.isClosed()); // Not closed, thus open
		assertFalse(receiver.getSender().isSynchronous()); // Not closed, thus open
		assertEquals(RunState.STARTED, receiver.getRunState());
	}

	@Test
	public void testPullingReceiverStartWithTimeout() throws Exception {
		testStartTimeout(setupSlowStartPullingListener(10000));
	}

	@Test
	public void testPushingReceiverStartWithTimeout() throws Exception {
		testStartTimeout(setupSlowStartPushingListener(10000));
	}

	public void testStartTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.

		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState(), "Receiver should be in state [EXCEPTION_STARTING]");
		Thread.sleep(500); //Extra timeout to give the receiver some time to close all resources
		assertTrue(receiver.getSender().isSynchronous(), "Close has not been called on the Receiver's sender!"); //isSynchronous ==> isClosed

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		while(receiver.getRunState()!=RunState.STOPPED) {
			System.out.println(receiver.getRunState());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		assertTrue(listener.isClosed());
	}

	@Test
	public void testPullingReceiverStopWithTimeout() throws Exception {
		testStopTimeout(setupSlowStopPullingListener(100_000));
	}

	@Test
	public void testPushingReceiverStopWithTimeout() throws Exception {
		testStopTimeout(setupSlowStopPushingListener(100_000));
	}

	public void testStopTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	@Test
	public void testPollGuardStartTimeout() throws Exception {
		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListener(SlowListenerWithPollGuard.class, 0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering startup-delay timeout-guard
		listener.setStartupDelay(100_000);

		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
		Thread.sleep(5_000);
		log.warn("Test resuming");

		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		List<String> errors = (List<String>) adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg instanceof MessageKeeperMessage && "ERROR".equals(((MessageKeeperMessage)msg).getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());

		assertThat(errors, hasItem(containsString("Failed to restart receiver")));

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.STOPPED, receiver.getRunState());
	}

	@Test
	public void testPollGuardStopTimeout() throws Exception {
		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListener(SlowListenerWithPollGuard.class, 0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());
		log.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering stop-delay timeout-guard
		listener.setShutdownDelay(100_000);

		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
		Thread.sleep(5_000);
		log.warn("Test resuming");

		// Receiver may be in state "stopping" (by PollGuard) or in state "starting" while we come out of sleep, so wait until it's started
		waitForState(receiver, RunState.STARTED);

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());

		List<String> warnings = (List<String>) adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg instanceof MessageKeeperMessage && "WARN".equals(((MessageKeeperMessage)msg).getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());
		assertThat(warnings, everyItem(containsString("JMS poll timeout")));
	}

	@Test
	public void startReceiver() throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(setupSlowStartPullingListener(10000));
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);

		log.info("Adapter RunState "+adapter.getRunState());

		// stop receiver then start
		SimpleAsyncTaskExecutor taskExecuter = new SimpleAsyncTaskExecutor();
		taskExecuter.execute(()-> {
			configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
			waitWhileInState(receiver, RunState.STOPPING);

			configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
			waitWhileInState(receiver, RunState.STARTING);
		});

		// when receiver is in starting state
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		// try to stop the started adapter
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitForState(adapter, RunState.STOPPED);

		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STOPPED, adapter.getRunState());
	}
}
