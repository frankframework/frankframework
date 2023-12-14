/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import static nl.nn.adapterframework.functional.FunctionalUtil.supplier;
import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitForState;
import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jms.Destination;
import javax.jms.TextMessage;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import bitronix.tm.TransactionManagerServices;
import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.TransactionAttribute;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.MessageStoreListener;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.jms.MessagingSource;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.management.IbisAction;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.RunState;

public class ReceiverTest {
	public static final DefaultTransactionDefinition TRANSACTION_DEFINITION = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	protected static final Logger LOG = LogUtil.getLogger(ReceiverTest.class);
	private TestConfiguration configuration;
	private TestAppender appender;

	@BeforeAll
	static void beforeAll() {
		// Ensure all lingering contexts from previous tests are closed.
		TransactionManagerType.closeAllConfigurationContexts();
	}

	@AfterEach
	void tearDown() {
		if (configuration != null) {
			configuration.stop();
			configuration.close();
			configuration = null;
		}
		if (TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerServices.getTransactionManager().shutdown();
		}
		if (appender != null) {
			TestAppender.removeAppender(appender);
			appender = null;
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
		Receiver<M> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {
		return setupAdapter(receiver, ExitState.SUCCESS);
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver, ExitState exitState) throws Exception {

		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName("ReceiverTestAdapterName");

		PipeLine pl = spy(new PipeLine());
		doAnswer(p -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setState(exitState);
			plr.setResult(p.getArgument(1));
			return plr;
		}).when(pl).process(anyString(), any(nl.nn.adapterframework.stream.Message.class), any(PipeLineSession.class));
		pl.setFirstPipe("dummy");

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(exitState);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.registerReceiver(receiver);
		configuration.registerAdapter(adapter);
		return adapter;
	}

	public Receiver<String> setupReceiverWithMessageStoreListener(MessageStoreListener<String> listener, ITransactionalStorage<Serializable> errorStorage) {
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		receiver.setListener(listener);
		receiver.setName("receiver");
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		receiver.setErrorStorage(errorStorage);
		receiver.setNumThreads(2);
		return receiver;
	}

	public MessageStoreListener<String> setupMessageStoreListener() throws Exception {
		Connection connection = mock(Connection.class);
		MessageStoreListener<String> listener = spy(new MessageStoreListener<>());
		listener.setConnectionsArePooled(true);
		doReturn(connection).when(listener).getConnection();
		listener.setSessionKeys("ANY-KEY");
		listener.extractSessionKeyList();
		doReturn(false).when(listener).hasRawMessageAvailable();

		doNothing().when(listener).configure();
		doNothing().when(listener).open();

		return listener;
	}

	public ITransactionalStorage<Serializable> setupErrorStorage() {
		return mock(JdbcTransactionalStorage.class);
	}

	public static Stream<Arguments> transactionManagers() {
		return Stream.of(
			Arguments.of(supplier(ReceiverTest::buildNarayanaTransactionManagerConfiguration)),
			Arguments.of(supplier(ReceiverTest::buildBtmTransactionManagerConfiguration))
		);
	}

	private static TestConfiguration buildBtmTransactionManagerConfiguration() {
		if (TransactionManagerServices.isTransactionManagerRunning()) {
			TransactionManagerServices.getTransactionManager().shutdown();
		}
		TestConfiguration configuration = buildConfiguration(TransactionManagerType.BTM);
		return configuration;
	}

	private static TestConfiguration buildNarayanaTransactionManagerConfiguration() {
		TestConfiguration configuration =  buildConfiguration(TransactionManagerType.NARAYANA);
		return configuration;
	}

	private static TestConfiguration buildConfiguration(TransactionManagerType txManagerType) {
		TestConfiguration configuration;
		if (txManagerType != null) {
			configuration = txManagerType.create();
		} else {
			configuration = new TestConfiguration();
		}

		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
		LOG.info("!Configuration Context for [{}] has been created.", txManagerType);
		return configuration;
	}

	@ParameterizedTest
	@MethodSource("transactionManagers")
	void testJmsMessageWithHighDeliveryCount(Supplier<TestConfiguration> configurationSupplier) throws Exception {
		// Arrange
		configuration = configurationSupplier.get();
		PushingJmsListener listener = spy(configuration.createBean(PushingJmsListener.class));
		doReturn(mock(Destination.class)).when(listener).getDestination();
		doNothing().when(listener).open();
		doNothing().when(listener).configure();

		@SuppressWarnings("unchecked")
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

		final JtaTransactionManager txManager = configuration.getBean(JtaTransactionManager.class);
		txManager.setDefaultTimeout(1);
		receiver.setTxManager(txManager);
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
		RawMessageWrapper<javax.jms.Message> messageWrapper = new RawMessageWrapper<>(jmsMessage, "dummy-message-id", "dummy-cid");


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
								return String.valueOf(count);
							});
						try (PipeLineSession session = new PipeLineSession()) {
							receiver.processRawMessage(listener, messageWrapper, session, false);
							processedNoException.incrementAndGet();
						} catch (Exception e) {
							LOG.warn("Caught exception in Receiver:", e);
							exceptionsFromReceiver.incrementAndGet();
						} finally {
							if (tx.isRollbackOnly()) {
								rolledBackTXCounter.incrementAndGet();
							} else {
								LOG.warn("I had expected TX to be marked for rollback-only by now?");
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

		((DisposableBean) txManager).destroy();

		// Assert
		assertAll(
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
	void testJmsMessageWithException(Supplier<TestConfiguration> configurationSupplier) throws Exception {
		// Arrange
		configuration = configurationSupplier.get();
		PushingJmsListener listener = spy(configuration.createBean(PushingJmsListener.class));
		doReturn(mock(Destination.class)).when(listener).getDestination();
		doNothing().when(listener).open();
		doNothing().when(listener).configure();

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Field messagingSourceField = JMSFacade.class.getDeclaredField("messagingSource");
		messagingSourceField.setAccessible(true);
		MessagingSource messagingSource = mock(MessagingSource.class);
		messagingSourceField.set(listener, messagingSource);

		@SuppressWarnings("unchecked")
		IListenerConnector<javax.jms.Message> jmsConnectorMock = mock(IListenerConnector.class);
		listener.setJmsConnector(jmsConnectorMock);
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);

		final JtaTransactionManager txManager = configuration.getBean(JtaTransactionManager.class);
		txManager.setDefaultTimeout(1);
//		txManager.setDefaultTimeout(1000000); // Long timeout for debug, do not commit this timeout!! Should be 1

		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.REQUIRED);

		// assume there was no connectivity, the message was not able to be stored in the database, retryInterval keeps increasing.
		final Field retryIntervalField = Receiver.class.getDeclaredField("retryInterval");
		retryIntervalField.setAccessible(true);
		retryIntervalField.set(receiver, 2);

		Adapter adapter = setupAdapter(receiver, ExitState.ERROR);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		final int TEST_MAX_RETRIES = 2;
		final int NR_TIMES_MESSAGE_OFFERED = TEST_MAX_RETRIES + 1;
		receiver.setMaxRetries(TEST_MAX_RETRIES);
		receiver.setMaxDeliveries(TEST_MAX_RETRIES);

		final AtomicInteger rolledBackTXCounter = new AtomicInteger();
		final AtomicInteger txRollbackOnlyInErrorStorage = new AtomicInteger();
		final AtomicInteger movedToErrorStorage = new AtomicInteger();
		final AtomicInteger exceptionsFromReceiver = new AtomicInteger();
		final AtomicInteger processedNoException = new AtomicInteger();
		final AtomicInteger txNotCompletedAfterReceiverEnds = new AtomicInteger();

		TextMessage jmsMessage = mock(TextMessage.class);
		doReturn("dummy-message-id").when(jmsMessage).getJMSMessageID();
		doAnswer(invocation -> rolledBackTXCounter.get() + 1).when(jmsMessage).getIntProperty("JMSXDeliveryCount");
		doReturn(Collections.emptyEnumeration()).when(jmsMessage).getPropertyNames();
		doReturn("message").when(jmsMessage).getText();
		RawMessageWrapper<javax.jms.Message> messageWrapper = new RawMessageWrapper<>(jmsMessage, "dummy-message-id", "dummy-cid");

		ArgumentCaptor<String> messageIdCaptor = forClass(String.class);
		ArgumentCaptor<String> correlationIdCaptor = forClass(String.class);
		ArgumentCaptor<Serializable> messageCaptor = forClass(Serializable.class);

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
							LOG.warn("Caught exception in Receiver:", e);
							exceptionsFromReceiver.incrementAndGet();
						} finally {
							if (tx.isRollbackOnly()) {
								rolledBackTXCounter.incrementAndGet();
							} else {
								LOG.warn("I had expected TX to be marked for rollback-only by now?");
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

		((DisposableBean) txManager).destroy();

		// Assert
		assertAll(
			() -> assertEquals("dummy-message-id", messageIdCaptor.getValue(), "Message ID does not match"),
			() -> assertEquals("dummy-cid", correlationIdCaptor.getValue(), "Correlation ID does not match"),
			() -> assertEquals("message", ((MessageWrapper<?>)messageCaptor.getValue()).getMessage().asString(), "Message contents do not match"),
			() -> assertEquals(0, rolledBackTXCounter.get(), "rolledBackTXCounter: Mismatch in nr of messages marked for rollback by TX manager"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, processedNoException.get(), "processedNoException: Mismatch in nr of messages processed without exception from receiver"),
			() -> assertEquals(0, txRollbackOnlyInErrorStorage.get(), "txRollbackOnlyInErrorStorage: Mismatch in nr of transactions already marked rollback-only while moving to error storage."),
			() -> assertEquals(0, exceptionsFromReceiver.get(), "exceptionsFromReceiver: Mismatch in nr of exceptions from Receiver method"),
			() -> assertEquals(1, movedToErrorStorage.get(), "movedToErrorStorage: Mismatch in nr of messages moved to error storage"),
			() -> assertEquals(NR_TIMES_MESSAGE_OFFERED, txNotCompletedAfterReceiverEnds.get(), "txNotCompletedAfterReceiverEnds: Mismatch in nr of transactions not completed after receiver finishes")
		);
	}

	@Test
	void testGetDeliveryCountWithJmsListener() throws Exception {
		// Arrange
		configuration = buildConfiguration(null);
		PushingJmsListener listener = spy(configuration.createBean(PushingJmsListener.class));
		doReturn(mock(Destination.class)).when(listener).getDestination();
		doNothing().when(listener).open();

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Field messagingSourceField = JMSFacade.class.getDeclaredField("messagingSource");
		messagingSourceField.setAccessible(true);
		MessagingSource messagingSource = mock(MessagingSource.class);
		messagingSourceField.set(listener, messagingSource);

		@SuppressWarnings("unchecked")
		IListenerConnector<javax.jms.Message> jmsConnectorMock = mock(IListenerConnector.class);
		listener.setJmsConnector(jmsConnectorMock);
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);

		TextMessage jmsMessage = mock(TextMessage.class);
		doReturn("dummy-message-id").when(jmsMessage).getJMSMessageID();
		doAnswer(invocation -> 5).when(jmsMessage).getIntProperty("JMSXDeliveryCount");
		doReturn(Collections.emptyEnumeration()).when(jmsMessage).getPropertyNames();
		doReturn("message").when(jmsMessage).getText();
		RawMessageWrapper<javax.jms.Message> rawMessage = new RawMessageWrapper<>(jmsMessage, "dummy-message-id", "dummy-cid");

		// Act
		int result = receiver.getDeliveryCount(rawMessage);

		// Assert
		assertEquals(4, result);
	}

	@Test
	void testGetDeliveryCountWithDirectoryListener() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		DirectoryListener listener = spy(configuration.createBean(DirectoryListener.class));
		doNothing().when(listener).open();

		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> errorStorage = mock(ITransactionalStorage.class);
		@SuppressWarnings("unchecked")
		ITransactionalStorage<Serializable> messageLog = mock(ITransactionalStorage.class);

		Receiver<Path> receiver = setupReceiver(listener);
		receiver.setErrorStorage(errorStorage);
		receiver.setMessageLog(messageLog);

		final JtaTransactionManager txManager = configuration.getBean(JtaTransactionManager.class);
		txManager.setDefaultTimeout(1);
		receiver.setTxManager(txManager);
		receiver.setTransactionAttribute(TransactionAttribute.NOTSUPPORTED);

		Adapter adapter = setupAdapter(receiver, ExitState.ERROR);
		configuration.configure();
		configuration.start();
		waitForState(adapter, RunState.STARTED);

		final String messageId = "A Path";
		Path fileMessage = Paths.get(messageId);
		RawMessageWrapper<Path> rawMessageWrapper = new RawMessageWrapper<>(fileMessage, messageId, null);

		// Act
		int result1 = receiver.getDeliveryCount(rawMessageWrapper);

		// Assert
		assertEquals(1, result1);

		// Arrange (for 2nd invocation)
		try (PipeLineSession session = new PipeLineSession()) {
			session.put(PipeLineSession.MESSAGE_ID_KEY, messageId);
			receiver.processRawMessage(listener, rawMessageWrapper, session, false);
		} catch (Exception e) {
			// We expected an exception here...
		}

		// Act
		int result2 = receiver.getDeliveryCount(rawMessageWrapper);

		// Assert
		assertEquals(2, result2);
	}

	@Test
	public void testProcessRequest() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		RawMessageWrapper<String> rawTestMessageWrapper = new RawMessageWrapper<>(rawTestMessage, "mid", "cid");
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));

		configuration = buildNarayanaTransactionManagerConfiguration();
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		MessageStoreListener<String> listener = setupMessageStoreListener();
		Receiver<String> receiver = setupReceiverWithMessageStoreListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		PipeLine pipeLine = adapter.getPipeLine();
		PipeLineResult pipeLineResult = new PipeLineResult();
		pipeLineResult.setState(ExitState.SUCCESS);
		pipeLineResult.setResult(testMessage);
		doReturn(pipeLineResult).when(pipeLine).process(any(), any(), any());

		NarayanaJtaTransactionManager transactionManager = configuration.createBean(NarayanaJtaTransactionManager.class);
		receiver.setTxManager(transactionManager);

		// start adapter
		configuration.configure();
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);

		try (PipeLineSession session = new PipeLineSession()) {
			// Act
			Message result = receiver.processRequest(listener, rawTestMessageWrapper, testMessage, session);

			// Assert
			assertFalse(result.isScheduledForCloseOnExitOf(session), "Result message should not be scheduled for closure on exit of session");
			assertTrue(result.requiresStream(), "Result message should be a stream");
			assertTrue(result.asObject() instanceof Reader, "Result message should be a stream");
			assertEquals("TEST", result.asString());
		} finally {
			configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		}
	}

	@Test
	public void testManualRetryWithMessageStoreListener() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		final String testMessage = "\"<msg attr=\"\"an attribute\"\"/>\",\"ANY-KEY-VALUE\"";
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		MessageStoreListener<String> listener = setupMessageStoreListener();
		Receiver<String> receiver = setupReceiverWithMessageStoreListener(listener, errorStorage);
		Adapter adapter = setupAdapter(receiver);

		when(errorStorage.getMessage(any())).thenAnswer((Answer<RawMessageWrapper<?>>) invocation -> new RawMessageWrapper<>(testMessage, invocation.getArgument(0), null));

		// start adapter
		configuration.configure();
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);

		ArgumentCaptor<Message> messageCaptor = forClass(Message.class);
		ArgumentCaptor<PipeLineSession> sessionCaptor = forClass(PipeLineSession.class);

		PipeLineResult plr = new PipeLineResult();
		plr.setState(ExitState.SUCCESS);
		plr.setResult(Message.asMessage(testMessage));
		doReturn(plr).when(adapter).processMessageWithExceptions(any(), messageCaptor.capture(), sessionCaptor.capture());

		// Act
		receiver.retryMessage("1");

		// Assert
		Message message = messageCaptor.getValue();
		PipeLineSession pipeLineSession = sessionCaptor.getValue();
		assertEquals("<msg attr=\"an attribute\"/>", message.asString());
		assertTrue(pipeLineSession.containsKey("ANY-KEY"));
		assertEquals("ANY-KEY-VALUE", pipeLineSession.get("ANY-KEY"));

		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
	}

	@Test
	public void testPullingReceiverStartBasic() throws Exception {
		configuration = buildConfiguration(null);
		testStartNoTimeout(setupSlowStartPullingListener(0));
	}

	@Test
	public void testPushingReceiverStartBasic() throws Exception {
		configuration = buildConfiguration(null);
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

		LOG.info("Adapter RunState "+adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.
		LOG.info("Receiver RunState "+receiver.getRunState());

		assertFalse(listener.isClosed()); // Not closed, thus open
		assertFalse(receiver.getSender().isSynchronous()); // Not closed, thus open
		assertEquals(RunState.STARTED, receiver.getRunState());
	}

	@Test
	public void testPullingReceiverStartWithTimeout() throws Exception {
		configuration = buildConfiguration(null);
		testStartTimeout(setupSlowStartPullingListener(10_000));
	}

	@Test
	public void testPushingReceiverStartWithTimeout() throws Exception {
		configuration = buildConfiguration(null);
		testStartTimeout(setupSlowStartPushingListener(10_000));
	}

	public void testStartTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		receiver.setStartTimeout(1);
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		LOG.info("Adapter RunState " + adapter.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitWhileInState(receiver, RunState.STOPPED); //Ensure the next waitWhileInState doesn't skip when STATE is still STOPPED
		waitWhileInState(receiver, RunState.STARTING); //Don't continue until the receiver has been started.

		LOG.info("Receiver RunState " + receiver.getRunState());
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState(), "Receiver should be in state [EXCEPTION_STARTING]");
		await().atMost(500, TimeUnit.MILLISECONDS)
						.until(()-> receiver.getSender().isSynchronous());
		assertTrue(receiver.getSender().isSynchronous(), "Close has not been called on the Receiver's sender!"); //isSynchronous ==> isClosed

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		await()
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(100, TimeUnit.MILLISECONDS)
				.until(() -> {
					System.out.println(receiver.getRunState());
					return receiver.isInRunState(RunState.STOPPED);
				});
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());
		assertTrue(listener.isClosed());
	}

	@Test
	public void testStopAdapterWhileReceiverIsStillStarting() throws Exception {
		assumeFalse(TestAssertions.isTestRunningOnCI() || TestAssertions.isTestRunningOnGitHub(), "For unknown reasons this test is unreliable on Github and CI so only run locally for now until we have time to investigate");

		// Arrange
		configuration = buildConfiguration(null);
		SlowListenerBase listener = setupSlowStartPushingListener(1_000);
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);

		appender = TestAppender.newBuilder().build();
		TestAppender.addToRootLogger(appender);

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
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), null, null, true);
		await()
				.atMost(5, TimeUnit.SECONDS)
				.pollInterval(1, TimeUnit.SECONDS)
				.until(() -> {
					LOG.info("<*> Receiver runstate: " + receiver.getRunState());
					return adapter.getRunState() == RunState.STOPPED;
				});

		// Assert
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertTrue(listener.isClosed());

		// If logs do not contain these lines, then we did not actually test what we meant to test. Perhaps receiver start delay need to be increased.
		assertThat(appender.getLogLines(), hasItem(containsString("receiver currently in state [STARTING], ignoring stop() command")));
		assertThat(appender.getLogLines(), hasItem(containsString("which was still starting when stop() command was received")));
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
		Receiver<javax.jms.Message> receiver = setupReceiver(setupSlowStopPushingListener(100_000));
		Adapter adapter = setupAdapter(receiver);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		LOG.info("Adapter RunState "+adapter.getRunState());
		LOG.info("Receiver RunState "+receiver.getRunState());
		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		LOG.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		new Thread(
				()-> configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true),
				"Stopping Adapter Async")
				.start();
		await()
				.atMost(10, TimeUnit.SECONDS)
				.until(()-> adapter.getRunState() == RunState.STOPPED);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	public void testStopTimeout(SlowListenerBase listener) throws Exception {
		Receiver<javax.jms.Message> receiver = setupReceiver(listener);
		Adapter adapter = setupAdapter(receiver);
		receiver.setStopTimeout(1);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		LOG.info("Adapter RunState "+adapter.getRunState());
		LOG.info("Receiver RunState "+receiver.getRunState());
		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		LOG.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());
	}

	@Test
	public void testPollGuardStartTimeout() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();

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

		LOG.info("Adapter RunState " + adapter.getRunState());
		LOG.info("Receiver RunState " + receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// Act
		// From here the PollGuard should be triggering startup-delay timeout-guard
		listener.setStartupDelay(100_000);

		LOG.warn("Test sleeping to let poll guard timer run and do its work for a while");
		await().atMost(5, TimeUnit.SECONDS)
				.until(receiver::getRunState, equalTo(RunState.EXCEPTION_STARTING));

		// Assert
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		List<String> errors = adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg != null && "ERROR".equals(msg.getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());

		assertThat(errors, hasItem(containsString("Failed to restart receiver")));

		// After
		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		LOG.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.STOPPED, receiver.getRunState());
	}

	@Test
	public void testPollGuardStopTimeout() throws Exception {
		configuration = buildNarayanaTransactionManagerConfiguration();
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

		LOG.info("Adapter RunState "+adapter.getRunState());
		LOG.info("Receiver RunState "+receiver.getRunState());
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); //Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering stop-delay timeout-guard
		listener.setShutdownDelay(100_000);

//		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
//		Thread.sleep(5_000);
//		log.warn("Test resuming");

		// Receiver may be in state "stopping" (by PollGuard) or in state "starting" while we come out of sleep, so wait until it's started
		waitForState(receiver, RunState.STARTED);

		configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		LOG.info("Receiver RunState "+receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());

		List<String> warnings = adapter.getMessageKeeper()
				.stream()
				.filter((msg) -> msg instanceof MessageKeeperMessage && "WARN".equals(((MessageKeeperMessage)msg).getMessageLevel()))
				.map(Object::toString)
				.collect(Collectors.toList());
		assertThat(warnings, everyItem(containsString("JMS poll timeout")));
	}

	@Test
	public void startReceiver() throws Exception {
		configuration = buildConfiguration(null);
		Receiver<javax.jms.Message> receiver = setupReceiver(setupSlowStartPullingListener(10_000));
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

		LOG.info("Adapter RunState " + adapter.getRunState());
		LOG.info("Receiver RunState " + receiver.getRunState());

		// stop receiver then start
		Semaphore semaphore = new Semaphore(0);
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.execute(()-> {
			try {
				LOG.debug("Stopping receiver [{}] from executor-thread.", receiver.getName());
				configuration.getIbisManager().handleAction(IbisAction.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
				waitForState(receiver, RunState.STOPPED);

				if (receiver.getRunState() != RunState.STOPPED) {
					LOG.error("Receiver should be in state STOPPED, instead is in state [{}]", receiver.getRunState());
					return;
				}

				LOG.debug("Restarting receiver [{}] from executor-thread.", receiver.getName());
				configuration.getIbisManager().handleAction(IbisAction.STARTRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
				waitForState(receiver, RunState.STARTING, RunState.EXCEPTION_STARTING);
				waitWhileInState(receiver, RunState.STARTING);
			} finally {
				semaphore.release();
			}
		});

		semaphore.acquire();
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		// try to stop the started adapter
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);
		waitForState(adapter, RunState.STOPPED);

		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(RunState.STOPPED, adapter.getRunState());
	}

	@Test
	public void testResultLargerThanMaxCommentSize() throws Exception {
		// Arrange
		configuration = buildNarayanaTransactionManagerConfiguration();
		ITransactionalStorage<Serializable> errorStorage = setupErrorStorage();
		MessageStoreListener<String> listener = setupMessageStoreListener();
		Receiver<String> receiver = setupReceiverWithMessageStoreListener(listener, errorStorage);
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

		NarayanaJtaTransactionManager transactionManager = configuration.createBean(NarayanaJtaTransactionManager.class);
		receiver.setTxManager(transactionManager);

		configuration.configure();
		configuration.start();

		waitForState(receiver, RunState.STARTED);

		PipeLineSession session = new PipeLineSession();

		// Act
		Message message = receiver.processRequest(listener, new RawMessageWrapper<>("raw"), Message.nullMessage(), session);

		// Assert
		assertEquals(result, message);
	}
}
