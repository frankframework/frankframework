/*
   Copyright 2022-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.receivers;

import static org.awaitility.Awaitility.await;
import static org.frankframework.functional.FunctionalUtil.supplier;
import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Lombok;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SpringEventErrorHandler;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.core.SenderException;
import org.frankframework.core.TransactionAttribute;
import org.frankframework.jdbc.JdbcTransactionalStorage;
import org.frankframework.jdbc.MessageStoreListener;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.management.Action;
import org.frankframework.monitoring.AdapterFilter;
import org.frankframework.monitoring.EventType;
import org.frankframework.monitoring.IMonitorDestination;
import org.frankframework.monitoring.ITrigger;
import org.frankframework.monitoring.Monitor;
import org.frankframework.monitoring.MonitorManager;
import org.frankframework.monitoring.Severity;
import org.frankframework.monitoring.SourceFiltering;
import org.frankframework.monitoring.Trigger;
import org.frankframework.monitoring.events.FireMonitorEvent;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.testutil.mock.DataSourceFactoryMock;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

@Tag("slow")
@Log4j2
public class ReceiverTest {
	public static final DefaultTransactionDefinition TX_REQUIRES_NEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	private TestConfiguration configuration;
	private String adapterName;

	@BeforeAll
	static void beforeAll() {
		// Ensure all lingering contexts from previous tests are closed.
		TransactionManagerType.closeAllConfigurationContexts();
	}

	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		adapterName = testInfo.getDisplayName().replace('/', '_');
	}

	@AfterEach
	@Timeout(value = 30) // Unfortunately this doesn't work on other threads
	void tearDown() {
		if (configuration != null) {
			configuration.close();
			configuration = null;
		}
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

	public <M> Receiver<M> setupReceiver(IListener<M> listener) {
		@SuppressWarnings("unchecked")
		Receiver<M> receiver = spy(configuration.createBean(Receiver.class));
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		// To speed up test, we don't actually sleep
		doNothing().when(receiver).suspendReceiverThread(anyInt());
		DummySender sender = configuration.createBean();
		receiver.setSender(sender);
		return receiver;
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {
		return setupAdapter(receiver, ExitState.SUCCESS);
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver, ExitState exitState) throws Exception {
		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName(adapterName);

		PipeLine pl = spy(SpringUtils.createBean(adapter, PipeLine.class));
		doAnswer(p -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setState(exitState);
			plr.setResult(p.getArgument(1));
			return plr;
		}).when(pl).process(anyString(), any(Message.class), any(PipeLineSession.class));
		pl.setFirstPipe("dummy");

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(exitState);
		pl.addPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.addReceiver(receiver);
		configuration.addAdapter(adapter);
		return adapter;
	}

	public Receiver<Serializable> setupReceiverWithListener(IListener listener, ITransactionalStorage<Serializable> errorStorage) {
		@SuppressWarnings("unchecked")
		Receiver<Serializable> receiver = spy(configuration.createBean(Receiver.class));
		receiver.setListener(listener);
		receiver.setName("receiver");
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		receiver.setErrorStorage(errorStorage);
		receiver.setNumThreads(2);
		// To speed up test, we don't actually sleep
		doNothing().when(receiver).suspendReceiverThread(anyInt());
		return receiver;
	}

	public MessageStoreListener setupMessageStoreListener() throws Exception {
		MessageStoreListener listener = spy(new MessageStoreListener());
		listener.setDataSourceFactory(new DataSourceFactoryMock());
		listener.setConnectionsArePooled(true);
		listener.setName("messageStoreListener");
		listener.setSessionKeys("ANY-KEY");
		listener.extractSessionKeyList();

		doReturn("dummy-destination").when(listener).getPhysicalDestinationName();
		doReturn(false).when(listener).hasRawMessageAvailable();
		doNothing().when(listener).configure();
		doNothing().when(listener).start();

		return listener;
	}

	public JavaListener setupJavaListener() throws Exception {
		JavaListener listener = spy(new JavaListener());
		listener.setName("javaListener");

		doReturn("dummy-destination").when(listener).getPhysicalDestinationName();
		doNothing().when(listener).configure();
		doNothing().when(listener).start();

		return listener;
	}

	@SuppressWarnings("unchecked")
	public ITransactionalStorage<Serializable> setupErrorStorage() {
		JdbcTransactionalStorage<Serializable> txStorage = mock(JdbcTransactionalStorage.class);
		txStorage.setDataSourceFactory(new DataSourceFactoryMock());
		return txStorage;
	}

	public static Stream<Arguments> transactionManagers() {
		return Stream.of(
			Arguments.of(supplier(ReceiverTest::buildNarayanaTransactionManagerConfiguration)),
			Arguments.of(supplier(ReceiverTest::buildDataSourceTransactionManagerConfiguration))
		);
	}

	private static TestConfiguration buildNarayanaTransactionManagerConfiguration() {
		return buildConfiguration(TransactionManagerType.NARAYANA);
	}

	private static TestConfiguration buildDataSourceTransactionManagerConfiguration() {
		return buildConfiguration(TransactionManagerType.DATASOURCE);
	}

	private static TestConfiguration buildConfiguration(TransactionManagerType txManagerType) {
		TestConfiguration configuration;
		if (txManagerType != null) {
			configuration = txManagerType.create(false);
		} else {
			configuration = new TestConfiguration(false);
		}

		log.info("Configuration Context for [{}] has been created.", txManagerType);
		return configuration;
	}

	@ParameterizedTest
	@MethodSource("transactionManagers")
	void testMessageWithHighDeliveryCount(Supplier<TestConfiguration> configurationSupplier) throws Exception {
		// Arrange
		configuration = configurationSupplier.get();
		MockPushingListener listener = spy(configuration.createBean(MockPushingListener.class));
		doNothing().when(listener).start();
		doNothing().when(listener).configure();

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		Receiver<String> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);

		final PlatformTransactionManager txManager = configuration.getBean("txManagerReal", PlatformTransactionManager.class);
		((AbstractPlatformTransactionManager)txManager).setDefaultTimeout(1);
		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.REQUIRED);

		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		RawMessageWrapper<String> messageWrapper = new RawMessageWrapper<>("dummy-message", "dummy-message-id", "dummy-cid");
		listener.setMockedDeliveryCount(receiver.getMaxRetries() + 2);

		final int NR_TIMES_MESSAGE_OFFERED = 5;
		final AtomicInteger rolledBackTXCounter = new AtomicInteger();
		final AtomicInteger txRollbackOnlyInErrorStorage = new AtomicInteger();
		final AtomicInteger movedToErrorStorage = new AtomicInteger();
		final AtomicInteger exceptionsFromReceiver = new AtomicInteger();
		final AtomicInteger processedNoException = new AtomicInteger();
		final AtomicInteger txNotCompletedAfterReceiverEnds = new AtomicInteger();

		final Semaphore semaphore = new Semaphore(0);
		Thread mockListenerThread = new Thread("mock-listener-thread") {
			@Override
			public void run() {
				try {
					int nrTries = 0;
					while (nrTries++ < NR_TIMES_MESSAGE_OFFERED) {
						final TransactionStatus tx = txManager.getTransaction(TX_REQUIRES_NEW);
						//noinspection unchecked
						reset(errorStorage, listener);
						when(errorStorage.storeMessage(any(), any(), any(), any(), any(), any()))
							.thenAnswer(invocation -> {
								if (tx.isRollbackOnly()) {
									txRollbackOnlyInErrorStorage.incrementAndGet();
									throw new SQLException("TX is rollback-only. Getting out!");
								}
								int count = movedToErrorStorage.incrementAndGet();
								return String.valueOf(count);
							});
						try (PipeLineSession session = new PipeLineSession()) {
							receiver.processRawMessage(listener, messageWrapper, session, false);
							processedNoException.incrementAndGet();
						} catch (Exception e) {
							log.warn("Caught exception in Receiver:", e);
							exceptionsFromReceiver.incrementAndGet();
						} finally {
							if (tx.isRollbackOnly()) {
								rolledBackTXCounter.incrementAndGet();
							} else {
								log.debug("Main TX not marked for rollback-only.");
							}
							if (!tx.isCompleted()) {
								// We do rollback inside the Receiver already but if the TX is aborted
								// it never seems to be marked "Completed" by Narayana.
								txNotCompletedAfterReceiverEnds.incrementAndGet();
								txManager.rollback(tx);
							}
						}
					}
				} catch (SenderException | IllegalArgumentException e) {
					throw Lombok.sneakyThrow(e);
				} finally {
					semaphore.release();
				}
			}
		};

		// Act
		mockListenerThread.start();
		semaphore.acquire(); // Wait until thread is finished.

		if (txManager instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}

		// Assert
		assertAll(
			() -> assertEquals(3, receiver.getMaxRetries()),
			() -> assertEquals(0, rolledBackTXCounter.get(), "rolledBackTXCounter: Mismatch in nr of messages marked for rollback by TX manager"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, processedNoException.get(), "processedNoException: Mismatch in nr of messages processed without exception from receiver"),
			() -> assertEquals(0, txRollbackOnlyInErrorStorage.get(), "txRollbackOnlyInErrorStorage: Mismatch in nr of transactions already marked rollback-only while moving to error storage."),
			() -> assertEquals(0, exceptionsFromReceiver.get(), "exceptionsFromReceiver: Mismatch in nr of exceptions from Receiver method"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, movedToErrorStorage.get(), "movedToErrorStorage: Mismatch in nr of messages moved to error storage"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, txNotCompletedAfterReceiverEnds.get(), "txNotCompletedAfterReceiverEnds: Mismatch in nr of transactions not completed after receiver finishes")
		);
	}

	@ParameterizedTest
	@MethodSource("transactionManagers")
	void testMessageWithException(Supplier<TestConfiguration> configurationSupplier) throws Exception {
		// Arrange
		configuration = configurationSupplier.get();
		MockPushingListener listener = spy(configuration.createBean(MockPushingListener.class));

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Receiver<String> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);

		final PlatformTransactionManager txManager = configuration.getBean("txManagerReal", PlatformTransactionManager.class);
		((AbstractPlatformTransactionManager)txManager).setDefaultTimeout(10);
//		txManager.setDefaultTimeout(1000000); // Long timeout for debug, do not commit this timeout!! Should be 10

		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.REQUIRED);

		final int TEST_MAX_RETRIES = 2;
		final int MAX_NR_TIMES_MESSAGE_OFFERED = TEST_MAX_RETRIES + 3;
		receiver.setMaxRetries(TEST_MAX_RETRIES);

		Adapter adapter = setupAdapter(receiver, ExitState.ERROR);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		final AtomicInteger rolledBackTXCounter = new AtomicInteger();
		final AtomicInteger txRollbackOnlyInErrorStorage = new AtomicInteger();
		final AtomicInteger movedToErrorStorage = new AtomicInteger();
		final AtomicInteger exceptionsFromReceiver = new AtomicInteger();
		final AtomicInteger processedNoException = new AtomicInteger();
		final AtomicInteger txNotCompletedAfterReceiverEnds = new AtomicInteger();

		RawMessageWrapper<String> messageWrapper = new RawMessageWrapper<>("message", "dummy-message-id", "dummy-cid");

		ArgumentCaptor<String> messageIdCaptor = forClass(String.class);
		ArgumentCaptor<String> correlationIdCaptor = forClass(String.class);
		ArgumentCaptor<Serializable> messageCaptor = forClass(Serializable.class);

		final Semaphore semaphore = new Semaphore(0);
		Thread mockListenerThread = new Thread("mock-listener-thread") {
			@Override
			public void run() {
				try {
					int nrTries = 0;
					while (nrTries++ < MAX_NR_TIMES_MESSAGE_OFFERED && movedToErrorStorage.get() == 0) {
						final int deliveryCount = nrTries;
						listener.setMockedDeliveryCount(deliveryCount);

						log.info("Nr tries: {}, Nr rolled back transactions: {}, delivery count: {}", nrTries, rolledBackTXCounter.get(), receiver.getDeliveryCount(messageWrapper));
						final TransactionStatus tx = txManager.getTransaction(TX_REQUIRES_NEW);
						//noinspection unchecked
						reset(errorStorage, listener);
						when(errorStorage.storeMessage(messageIdCaptor.capture(), correlationIdCaptor.capture(), any(), any(), any(), messageCaptor.capture()))
								.thenAnswer(invocation -> {
								if (tx.isRollbackOnly()) {
									txRollbackOnlyInErrorStorage.incrementAndGet();
									throw new SQLException("TX is rollback-only. Getting out!");
								}
								int count = movedToErrorStorage.incrementAndGet();
								return String.valueOf(count);
							});
						try (PipeLineSession session = new PipeLineSession()) {
							receiver.processRawMessage(listener, messageWrapper, session, false);
							processedNoException.incrementAndGet();
						} catch (Exception e) {
							log.warn("Caught exception in Receiver:", e);
							exceptionsFromReceiver.incrementAndGet();
						} finally {
							if (tx.isRollbackOnly()) {
								rolledBackTXCounter.incrementAndGet();
							} else {
								log.debug("Main TX not marked for rollback-only");
							}
							if (!tx.isCompleted()) {
								// We do rollback inside the Receiver already but if the TX is aborted
								// it never seems to be marked "Completed" by Narayana.
								txNotCompletedAfterReceiverEnds.incrementAndGet();
								txManager.rollback(tx);
							}
						}
					}
				} catch (SenderException | IllegalArgumentException e) {
					throw Lombok.sneakyThrow(e);
				} finally {
					semaphore.release();
				}
			}
		};

		// Act
		mockListenerThread.start();
		semaphore.acquire(); // Wait until thread is finished.

		if (txManager instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}

		// Assert
		int expectedNrTimesMessageActuallyOffered = receiver.getMaxRetries() + 2;
		assertAll(
			() -> assertEquals("dummy-message-id", messageIdCaptor.getValue(), "Message ID does not match"),
			() -> assertEquals("dummy-cid", correlationIdCaptor.getValue(), "Correlation ID does not match"),
			() -> assertEquals("message", ((MessageWrapper<?>)messageCaptor.getValue()).getMessage().asString(), "Message contents do not match"),
			() -> assertEquals(0, rolledBackTXCounter.get(), "rolledBackTXCounter: Mismatch in nr of messages marked for rollback by TX manager"),
			() -> assertEquals(expectedNrTimesMessageActuallyOffered, processedNoException.get(), "processedNoException: Mismatch in nr of messages processed without exception from receiver"),
			() -> assertEquals(0, txRollbackOnlyInErrorStorage.get(), "txRollbackOnlyInErrorStorage: Mismatch in nr of transactions already marked rollback-only while moving to error storage."),
			() -> assertEquals(0, exceptionsFromReceiver.get(), "exceptionsFromReceiver: Mismatch in nr of exceptions from Receiver method"),
			() -> assertEquals(1, movedToErrorStorage.get(), "movedToErrorStorage: Mismatch in nr of messages moved to error storage"),
			() -> assertEquals(expectedNrTimesMessageActuallyOffered, txNotCompletedAfterReceiverEnds.get(), "txNotCompletedAfterReceiverEnds: Mismatch in nr of transactions not completed after receiver finishes")
		);
	}

	@Test
	void testStopReceiverWithFaultyMonitor() throws Exception {
		// Arrange
		configuration = buildDataSourceTransactionManagerConfiguration();
		IListener<Serializable> listener = setupMessageStoreListener();
		Receiver<Serializable> receiver = setupReceiver(listener);

		IMonitorDestination destination = mock(IMonitorDestination.class);
		when(destination.getName()).thenReturn("dummy-destination-name");

		MonitorManager monitorManager = configuration.getBean("monitorManager", MonitorManager.class);
		Monitor monitor = SpringUtils.createBean(monitorManager);
		monitor.setName("test-monitor");
		monitor.setType(EventType.TECHNICAL);

		monitorManager.addMonitor(monitor);
		monitorManager.addDestination(destination);
		monitor.setDestinations(destination.getName());

		Trigger badTrigger = spy(SpringUtils.createBean(monitorManager, Trigger.class));
		doThrow(IllegalStateException.class).when(badTrigger).onApplicationEvent(any(FireMonitorEvent.class));
		badTrigger.setSeverity(Severity.WARNING);
		badTrigger.setTriggerType(ITrigger.TriggerType.ALARM);
		badTrigger.setEventCode(Receiver.RCV_SHUTDOWN_MONITOR_EVENT);

		monitor.addTrigger(badTrigger);

		ConfigurableListableBeanFactory beanFactory = configuration.getBeanFactory();
		SimpleApplicationEventMulticaster eventMulticaster = beanFactory.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME, SimpleApplicationEventMulticaster.class);
		eventMulticaster.setErrorHandler(new SpringEventErrorHandler());

		Adapter adapter = setupAdapter(receiver, ExitState.ERROR);

		badTrigger.setSourceFiltering(SourceFiltering.ADAPTER);
		AdapterFilter af = new AdapterFilter();
		af.setAdapter(adapter.getName());
		badTrigger.addAdapterFilter(af);

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
		waitWhileInState(receiver, RunState.STOPPED);
		waitWhileInState(receiver, RunState.STARTING);

		log.info("Adapter RunState: {}", adapter.getRunState());
		log.info("Receiver RunState: {}", receiver.getRunState());

		// Act
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			adapter.stop();

			waitForState(adapter, RunState.STOPPED);

			assertThat(appender.getLogLines(), hasItem(containsString("Error handling event")));
		}
	}

	@Test
	void testGetDeliveryCountWithListenerThatKnowsDeliveryCount() throws Exception {
		// Arrange
		configuration = buildDataSourceTransactionManagerConfiguration();
		MockPushingListener listener = spy(configuration.createBean(MockPushingListener.class));

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Receiver<String> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);
		receiver.configure();

		// For backward compatibility reasons, should have 3 retries with listeners that know the delivery count.
		assertEquals(3, receiver.getMaxRetries());

		RawMessageWrapper<String> rawMessage = new RawMessageWrapper<>("message", "dummy-message-id", "dummy-cid");
		MessageWrapper<String> messageWrapper = new MessageWrapper<>(rawMessage, Message.nullMessage());
		listener.setMockedDeliveryCount(5);

		// Act / Assert
		assertAll(
				()-> assertEquals(5, receiver.getDeliveryCount(rawMessage)),
				()-> assertTrue(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, new PipeLineSession(), false)),
				()-> assertTrue(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper))
		);

		listener.setMockedDeliveryCount(receiver.getMaxRetries());
		receiver.updateMessageReceiveCount(messageWrapper);

		assertAll(
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, new PipeLineSession(), false)),
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper))
		);

		listener.setMockedDeliveryCount(receiver.getMaxRetries() - 1);
		receiver.updateMessageReceiveCount(messageWrapper);

		assertAll(
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, new PipeLineSession(), false)),
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper))
		);

		listener.setMockedDeliveryCount(receiver.getMaxRetries() + 1);
		receiver.updateMessageReceiveCount(messageWrapper);

		assertAll(
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, new PipeLineSession(), false)),
				()-> assertTrue(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper))
		);

		listener.setMockedDeliveryCount(receiver.getMaxRetries() + 2);
		receiver.updateMessageReceiveCount(messageWrapper);

		assertAll(
				()-> assertTrue(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper)),
				()-> assertTrue(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, new PipeLineSession(), false))
		);
	}

	@Test
	void testGetDeliveryCount() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		MockPullingListener listener = spy(configuration.createBean(MockPullingListener.class));

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Receiver<String> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);

		final JtaTransactionManager txManager = configuration.getBean(JtaTransactionManager.class);
		txManager.setDefaultTimeout(1);
		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.NOTSUPPORTED);
		receiver.configure();

		Adapter adapter = setupAdapter(receiver, ExitState.ERROR);
		configuration.configure();
		configuration.start();
		waitForState(adapter, RunState.STARTED);

		final String messageId = "A Path";
		RawMessageWrapper<String> rawMessageWrapper = new RawMessageWrapper<>("message", messageId, null);
		MessageWrapper<String> messageWrapper = new MessageWrapper<>(rawMessageWrapper, Message.asMessage(rawMessageWrapper.rawMessage));

		// Act
		try (PipeLineSession session = new PipeLineSession()) {
			session.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
			receiver.processRawMessage(listener, rawMessageWrapper, session, false);
		} catch (Exception e) {
			// Exception might occur here...
		}

		int result = receiver.getDeliveryCount(rawMessageWrapper);

		// Assert
		assertAll(
				()-> assertEquals(1, result),
				()-> assertEquals(1, receiver.getMaxRetries()),
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(messageWrapper)),
				()-> assertFalse(receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessageWrapper, new PipeLineSession(), false))
		);
	}

	@Test
	public void testProcessRequest() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));
		MessageWrapper<Serializable> rawTestMessageWrapper = new MessageWrapper<>(testMessage, "mid", "cid");

		configuration = buildNarayanaTransactionManagerConfiguration();
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		JavaListener listener = setupJavaListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		PipeLine pipeLine = adapter.getPipeLine();
		PipeLineResult pipeLineResult = new PipeLineResult();
		pipeLineResult.setState(ExitState.SUCCESS);
		pipeLineResult.setResult(testMessage);
		doReturn(pipeLineResult).when(pipeLine).process(any(), any(), any());

		NarayanaJtaTransactionManager transactionManager = configuration.createBean();
		receiver.setTxManager(transactionManager);

		// start adapter
		configuration.configure();
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);

		try (PipeLineSession session = new PipeLineSession()) {
			// Act
			Message result = receiver.processRequest(listener, rawTestMessageWrapper, session);

			// Assert
			assertFalse(result.isScheduledForCloseOnExitOf(session), "Result message should not be scheduled for closure on exit of session");
			assertTrue(result.requiresStream(), "Result message should be a stream");
			assertTrue(result.isRequestOfType(Reader.class), "Result message should be of type Reader");
			assertEquals("TEST", result.asString());
		}
	}

	@Test
	public void testManualRetryWithErrorStorage() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		final String testMessage = "\"<msg attr=\"\"an attribute\"\"/>\",\"ANY-KEY-VALUE\"";
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		MessageStoreListener listener = setupMessageStoreListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		when(errorStorage.getMessage("1")).thenAnswer((Answer<RawMessageWrapper<?>>) invocation -> new RawMessageWrapper<>(testMessage, invocation.getArgument(0), null));

		// start adapter
		configuration.configure();

		ArgumentCaptor<Message> messageCaptor = forClass(Message.class);
		ArgumentCaptor<PipeLineSession> sessionCaptor = forClass(PipeLineSession.class);

		PipeLineResult plr = new PipeLineResult();
		plr.setState(ExitState.SUCCESS);
		plr.setResult(new Message(testMessage));
		doReturn(plr).when(adapter).processMessageWithExceptions(any(), messageCaptor.capture(), sessionCaptor.capture());

		// Act
		receiver.retryMessage("1");

		// Assert
		Message message = messageCaptor.getValue();
		PipeLineSession pipeLineSession = sessionCaptor.getValue();
		assertEquals("<msg attr=\"an attribute\"/>", message.asString());
		assertTrue(pipeLineSession.containsKey("ANY-KEY"));
		assertEquals("ANY-KEY-VALUE", pipeLineSession.get("ANY-KEY"));

		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
	}

	@Test
	public void testManualRetryWithMessageStoreListener() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		final String testMessage = "\"<msg attr=\"\"an attribute\"\"/>\",\"ANY-KEY-VALUE\"";
		MessageStoreListener listener = setupMessageStoreListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, null);
		Adapter adapter = setupAdapter(receiver);
		IMessageBrowser<Serializable> messageBrowser = mock();

		when(messageBrowser.browseMessage("1")).thenAnswer((Answer<RawMessageWrapper<?>>) invocation -> new RawMessageWrapper<>(testMessage, invocation.getArgument(0), null));
		when(listener.getMessageBrowser(ProcessState.ERROR)).thenReturn(messageBrowser);
		when(listener.knownProcessStates()).thenReturn(Set.of(ProcessState.ERROR));

		// start adapter
		configuration.configure();

		ArgumentCaptor<Message> messageCaptor = forClass(Message.class);
		ArgumentCaptor<PipeLineSession> sessionCaptor = forClass(PipeLineSession.class);

		PipeLineResult plr = new PipeLineResult();
		plr.setState(ExitState.SUCCESS);
		plr.setResult(new Message(testMessage));
		doReturn(plr).when(adapter).processMessageWithExceptions(any(), messageCaptor.capture(), sessionCaptor.capture());

		// Act
		receiver.retryMessage("1");

		// Assert
		Message message = messageCaptor.getValue();
		PipeLineSession pipeLineSession = sessionCaptor.getValue();
		assertEquals("<msg attr=\"an attribute\"/>", message.asString());
		assertTrue(pipeLineSession.containsKey("ANY-KEY"));
		assertEquals("ANY-KEY-VALUE", pipeLineSession.get("ANY-KEY"));

		verify(listener).changeProcessState(any(), eq(ProcessState.DONE), any());

		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
	}

	@Test
	public void testManualRetryWithErrorStorageThrowsError() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		final String testMessage = "\"<msg attr=\"\"an attribute\"\"/>\",\"ANY-KEY-VALUE\"";
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		MessageStoreListener listener = setupMessageStoreListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		doThrow(new RuntimeException()).when(adapter).processMessageWithExceptions(any(), any(), any());
		doAnswer((Answer<RawMessageWrapper<?>>) invocation -> new RawMessageWrapper<>(testMessage, invocation.getArgument(0), null)).when(errorStorage).getMessage("1");
		doAnswer(invocation -> invocation.getArgument(0)).when(listener).changeProcessState(any(), any(), any());

		doReturn(false).when(listener).hasRawMessageAvailable();
		doReturn(true).when(listener).isPeekUntransacted();

		// start adapter
		configuration.configure();

		// Act
		assertThrows(ListenerException.class, ()-> receiver.retryMessage("1"));

		// Assert
		// TODO: Figure out how to trigger the branch in code that would trigger these lines
//		verify(errorStorage).deleteMessage("1");
//		verify(errorStorage).storeMessage(eq("1"), any(), any(), any(), any(), any());
		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
	}

	@Test
	public void testManualRetryWithMessageStoreListenerThrowsError() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		final String testMessage = "\"<msg attr=\"\"an attribute\"\"/>\",\"ANY-KEY-VALUE\"";
		MessageStoreListener listener = setupMessageStoreListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, null);
		Adapter adapter = setupAdapter(receiver);
		IMessageBrowser<String> messageBrowser = mock();

		doThrow(new RuntimeException()).when(adapter).processMessageWithExceptions(any(), any(), any());

		doAnswer(invocation -> invocation.getArgument(0)).when(listener).changeProcessState(any(), any(), any());
		doAnswer((Answer<RawMessageWrapper<?>>) invocation -> new RawMessageWrapper<>(testMessage, invocation.getArgument(0), null)).when(messageBrowser).browseMessage(any());
		doReturn(messageBrowser).when(listener).getMessageBrowser(ProcessState.ERROR);
		doReturn(Set.of(ProcessState.ERROR)).when(listener).knownProcessStates();

		doReturn(false).when(listener).hasRawMessageAvailable();
		doReturn(true).when(listener).isPeekUntransacted();

		// start adapter
		configuration.configure();

		// Act
		assertThrows(ListenerException.class, ()-> receiver.retryMessage("1"));

		// Assert
		verify(listener, never()).changeProcessState(any(), eq(ProcessState.DONE), any());

		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
	}

	@Test
	public void testPullingReceiverStartBasic() throws Exception {
		configuration = buildDataSourceTransactionManagerConfiguration();
		testStartNoTimeout(setupSlowStartPullingListener(0));
	}

	@Test
	public void testPushingReceiverStartBasic() throws Exception {
		configuration = buildDataSourceTransactionManagerConfiguration();
		testStartNoTimeout(setupSlowStartPushingListener(0));
	}

	public void testStartNoTimeout(SlowListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState: {}", adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); // Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); // Don't continue until the receiver has been started.
		log.info("Receiver RunState: {}", receiver.getRunState());

		assertFalse(listener.isClosed()); // Not closed, thus open
		assertFalse(receiver.getSender().isSynchronous()); // Not closed, thus open
		assertEquals(RunState.STARTED, receiver.getRunState());
	}

	@Test
	public void testPullingReceiverStartWithTimeout() throws Exception {
		configuration = buildDataSourceTransactionManagerConfiguration();
		testStartTimeout(setupSlowStartPullingListener(10_000));
	}

	@Test
	public void testPushingReceiverStartWithTimeout() throws Exception {
		configuration = buildDataSourceTransactionManagerConfiguration();
		testStartTimeout(setupSlowStartPushingListener(10_000));
	}

	public void testStartTimeout(SlowListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
		receiver.setStartTimeout(1);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState: {}", adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); // Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); // Don't continue until the receiver has been started.

		log.info("Receiver RunState: {}", receiver.getRunState());
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState(), "Receiver should be in state [EXCEPTION_STARTING]");
		await().atMost(500, TimeUnit.MILLISECONDS)
						.until(()-> receiver.getSender().isSynchronous());
		assertTrue(receiver.getSender().isSynchronous(), "Close has not been called on the Receiver's sender!"); // isSynchronous ==> isClosed

		configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		await()
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> {
					log.info("<*> Receiver runstate: {}", receiver.getRunState());
					return receiver.isInRunState(RunState.STOPPED);
				});
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		assertTrue(listener.isClosed());
	}

	/*
	 * This test is flaky when running with other tests because you don't know how many
	 * threads exist in the threadpool and can run concurrently.
	 */
	@Test
	public void testStopAdapterWhileReceiverIsStillStarting() throws Exception {
		assumeFalse(TestAssertions.isTestRunningWithSurefire() || TestAssertions.isTestRunningOnCI(), "flaky test, should not fail ci");

		// Arrange
		configuration = buildDataSourceTransactionManagerConfiguration();
		SlowListenerBase listener = setupSlowStartPushingListener(1_000);
		Receiver<String> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			assertEquals(RunState.STOPPED, adapter.getRunState());
			assertEquals(RunState.STOPPED, receiver.getRunState());

			// start adapter
			configuration.configure();
			configuration.start();

			waitWhileInState(adapter, RunState.STOPPED);
			waitWhileInState(adapter, RunState.STARTING);

			assertEquals(RunState.STARTED, adapter.getRunState());
			assertEquals(RunState.STARTING, receiver.getRunState());

			// Act
			configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), null, null, true);
			await()
					.atMost(5, TimeUnit.SECONDS)
					.pollInterval(1, TimeUnit.SECONDS)
					.until(() -> {
						log.info("<*> Receiver runstate: {}", receiver.getRunState());
						return adapter.getRunState() == RunState.STOPPED;
					});

			// Assert
			assertEquals(RunState.STOPPED, receiver.getRunState());
			assertEquals(RunState.STOPPED, adapter.getRunState());
			assertTrue(listener.isClosed());

			// If logs do not contain these lines, then we did not actually test what we meant to test. Perhaps receiver start delay need to be increased.
			assertThat(appender.getLogLines(), hasItem(containsString("receiver currently in state [STARTING], ignoring stop() command")));

			// This log line is not always present, it seems that the receiver sometimes starts/stops quicker then we expect...
			assertThat(appender.getLogLines(), hasItem(containsString("which was still starting when stop() command was received")));
		}
	}

	@Test
	public void testPullingReceiverStopWithTimeout() throws Exception {
		configuration = buildNarayanaTransactionManagerConfiguration();
		testStopTimeout(setupSlowStopPullingListener(100_000));
	}

	@Test
	public void testPushingReceiverStopWithTimeout() throws Exception {
		configuration = buildNarayanaTransactionManagerConfiguration();
		testStopTimeout(setupSlowStopPushingListener(100_000));
	}

	@Test
	public void testStopAdapterAfterStopReceiverWithException() throws Exception {
		configuration = buildNarayanaTransactionManagerConfiguration();
		Receiver<String> receiver = setupReceiver(setupSlowStopPushingListener(100_000));
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState: {}", adapter.getRunState());
		log.info("Receiver RunState: {}", receiver.getRunState());
		waitForState(receiver, RunState.STARTED); // Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState: {}", receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		new Thread(
				()-> configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true),
				"Stopping Adapter Async")
				.start();
		await()
				.atMost(10, TimeUnit.SECONDS)
				.until(()-> adapter.getRunState() == RunState.STOPPED);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	public void testStopTimeout(SlowListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);
		receiver.setStopTimeout(1);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		log.info("Adapter RunState: {}", adapter.getRunState());
		log.info("Receiver RunState: {}", receiver.getRunState());
		waitForState(receiver, RunState.STARTED); // Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState: {}", receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	@Test
	public void startReceiver() throws Exception {
		configuration = buildDataSourceTransactionManagerConfiguration();
		Receiver<String> receiver = setupReceiver(setupSlowStartPullingListener(10_000));
		receiver.setStartTimeout(1);
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

		log.info("Adapter RunState: {}", adapter.getRunState());
		log.info("Receiver RunState: {}", receiver.getRunState());

		// stop receiver then start
		Semaphore semaphore = new Semaphore(0);
		TaskExecutor taskExecutor = configuration.getApplicationContext().getBean("taskExecutor", TaskExecutor.class);
		taskExecutor.execute(()-> {
			try {
				log.debug("Stopping receiver [{}] from executor-thread.", receiver.getName());
				configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
				waitForState(receiver, RunState.STOPPED);

				if (receiver.getRunState() != RunState.STOPPED) {
					log.error("Receiver should be in state STOPPED, instead is in state [{}]", receiver.getRunState());
					return;
				}

				log.debug("Restarting receiver [{}] from executor-thread.", receiver.getName());
				configuration.getIbisManager().handleAction(Action.STARTRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
				waitForState(receiver, RunState.STARTING, RunState.EXCEPTION_STARTING);
				waitWhileInState(receiver, RunState.STARTING);
			} finally {
				semaphore.release();
			}
		});

		semaphore.acquire();
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		// try to stop the started adapter
		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitForState(adapter, RunState.STOPPED);

		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STOPPED, adapter.getRunState());
	}

	@Test
	public void testResultLargerThanMaxCommentSize() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		JavaListener listener = setupJavaListener();
		Receiver<Serializable> receiver = setupReceiverWithListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		// The actual size of a message as string can be shorter than the reported size. This could be due to incorrect
		// cached size in metadata, or due to conversion from byte[] to String for instance.
		// If the reported size was just above the MAXCOMMENTLEN while the actual size was below then this could cause
		// a StringIndexOutOfBoundsException. (Issue #5752).
		// Here we force the issue by specifically crafting such a message; in practice the difference will be less extreme.
		Message result = new Message("a short message");
		result.getContext().put(MessageContext.METADATA_SIZE, (long)ITransactionalStorage.MAXCOMMENTLEN + 100);
		PipeLineResult plr = new PipeLineResult();
		plr.setResult(result);
		plr.setState(ExitState.SUCCESS);

		doReturn(plr).when(adapter).processMessageWithExceptions(any(), any(), any());

		NarayanaJtaTransactionManager transactionManager = configuration.createBean();
		receiver.setTxManager(transactionManager);

		configuration.configure();
		configuration.start();

		waitForState(receiver, RunState.STARTED);

		PipeLineSession session = new PipeLineSession();

		// Act
		Message message = receiver.processRequest(listener, new MessageWrapper<>(new Message("raw"), null, null), session);

		// Assert
		assertEquals(result, message);
	}

	@ParameterizedTest
	@CsvSource({
			"500, 50, true",
			"50, 50, false",
			"40, 40, false"
	})
	public void testMaxBackoffDelayAdjustment(Integer maxBackoffDelay, int expectedBackoffDelay, boolean expectConfigWarning) {
		// Arrange
		configuration = buildDataSourceTransactionManagerConfiguration();
		Adapter adapter = configuration.createBean();
		adapter.setName("adapter");
		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();

		Receiver<String> receiver = SpringUtils.createBean(adapter);

		receiver.setMaxBackoffDelay(maxBackoffDelay);
		receiver.setTransactionTimeout(100);

		// Act
		int actualBackoffDelay = receiver.calculateAdjustedMaxBackoffDelay(maxBackoffDelay);

		// Assert
		assertEquals(expectedBackoffDelay, actualBackoffDelay);
		if (expectConfigWarning) {
			assertEquals(1, configWarnings.size(), "There should have been exactly 1 config warning");
			assertThat(configWarnings.get(0), containsString("Maximum backoff delay reduced"));
		} else {
			assertTrue(configWarnings.isEmpty(), "There should not have been any config warnings");
		}
	}

	@ParameterizedTest
	@MethodSource("transactionManagers")
	void testMessageTransactionRollbackAfterListenerCompleted(Supplier<TestConfiguration> configurationSupplier) throws Exception {
		// Arrange
		configuration = configurationSupplier.get();
		MockPushingListener listener = spy(configuration.createBean(MockPushingListener.class));

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);

		Receiver<String> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);

		final PlatformTransactionManager txManager = configuration.getBean("txManagerReal", PlatformTransactionManager.class);
		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.REQUIRED);

		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		RawMessageWrapper<String> messageWrapper = new RawMessageWrapper<>("message", "dummy-message-id", "dummy-cid");

		final TransactionStatus tx = txManager.getTransaction(TX_REQUIRES_NEW);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void beforeCommit(boolean readOnly) {
				throw new RuntimeException("Sabotage the TX commit");
			}
		});

		// No errors before we start
		assertEquals(0, adapter.getNumOfMessagesInError());

		// Act
		try (PipeLineSession session = new PipeLineSession()) {
			receiver.processRawMessage(listener, messageWrapper, session, false);
		} catch (Exception e) {
			fail("Caught exception in Receiver:", e);
		}
		// Still no errors before we commit
		assertEquals(0, adapter.getNumOfMessagesInError());

		assertThrows(RuntimeException.class, () -> txManager.commit(tx));

		// A bit of cleanup
		if (txManager instanceof DisposableBean disposableBean) {
			disposableBean.destroy();
		}

		configuration.getIbisManager().handleAction(Action.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitForState(adapter, RunState.STOPPED);

		// Assert
		// The commit should have set the message in error
		assertEquals(1, adapter.getNumOfMessagesInError());

	}
}
