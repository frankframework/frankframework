package org.frankframework.jms;

import static org.awaitility.Awaitility.await;
import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IListenerConnector;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.management.Action;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.RunState;

@Log4j2
public class PushingJmsListenerTest {
	private TestConfiguration configuration;
	private String adapterName;


	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		adapterName = testInfo.getDisplayName().replace('/', '_');
	}

	private static TestConfiguration buildConfiguration(TransactionManagerType txManagerType) {
		TestConfiguration configuration;
		if (txManagerType != null) {
			configuration = txManagerType.create(false);
		} else {
			configuration = new TestConfiguration(false);
		}

		log.info("!Configuration Context for [{}] has been created.", txManagerType);
		return configuration;
	}

	private void createMessagingSource(PushingJmsListener listener) throws NoSuchFieldException, IllegalAccessException {
		Field messagingSourceField = JMSFacade.class.getDeclaredField("messagingSource");
		messagingSourceField.setAccessible(true);
		MessagingSource messagingSource = mock(MessagingSource.class);
		messagingSourceField.set(listener, messagingSource);
	}

	protected SlowListenerWithPollGuard createSlowListenerWithPollGuard(int startupDelay, int shutdownDelay) {
		SlowListenerWithPollGuard listener = configuration.createBean(SlowListenerWithPollGuard.class);
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
		return receiver;
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {
		return setupAdapter(receiver, PipeLine.ExitState.SUCCESS);
	}

	public <M> Adapter setupAdapter(Receiver<M> receiver, PipeLine.ExitState exitState) throws Exception {
		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName(adapterName);

		PipeLine pl = spy(configuration.createBean(PipeLine.class));
		doAnswer(p -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setState(exitState);
			plr.setResult(p.getArgument(1));
			return plr;
		}).when(pl).process(anyString(), any(org.frankframework.stream.Message.class), any(PipeLineSession.class));
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

	@Test
	void testJmsMessageWithExceptionUntransactedAckModeClientShouldAckMsgWhenRejected() throws Exception {
		// TODO: This test should be moved to JMS module, since it tests some JMS Listener specific features and not a receiver specific feature
		// Arrange
		configuration = buildConfiguration(TransactionManagerType.DATASOURCE);
		PushingJmsListener listener = spy(configuration.createBean(PushingJmsListener.class));
		listener.setTransacted(false);
		listener.setAcknowledgeMode(JMSFacade.AcknowledgeMode.CLIENT_ACKNOWLEDGE);
		doReturn(mock(Destination.class)).when(listener).getDestination();
		doNothing().when(listener).start();
		doNothing().when(listener).configure();

		createMessagingSource(listener);

		@SuppressWarnings("unchecked")
		IListenerConnector<Message> jmsConnectorMock = mock(IListenerConnector.class);
		listener.setJmsConnector(jmsConnectorMock);
		Receiver<Message> receiver = setupReceiver(listener);
		receiver.setMaxRetries(1);

		Adapter adapter = setupAdapter(receiver, PipeLine.ExitState.ERROR);

		assertEquals(RunState.STOPPED, adapter.getRunState());
		assertEquals(RunState.STOPPED, receiver.getRunState());

		// start adapter
		configuration.configure();
		configuration.start();

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);

		TextMessage jmsMessage = mock(TextMessage.class);
		doReturn("dummy-message-id").when(jmsMessage).getJMSMessageID();
		doAnswer(invocation -> receiver.getMaxRetries() + 2).when(jmsMessage).getIntProperty("JMSXDeliveryCount");
		doReturn(Collections.emptyEnumeration()).when(jmsMessage).getPropertyNames();
		doReturn("message").when(jmsMessage).getText();
		RawMessageWrapper<Message> messageWrapper = new RawMessageWrapper<>(jmsMessage, "dummy-message-id", "dummy-cid");

		final Semaphore semaphore = new Semaphore(0);
		Thread mockListenerThread = new Thread("mock-listener-thread") {
			@Override
			public void run() {
				try (PipeLineSession session = new PipeLineSession()) {
					receiver.processRawMessage(listener, messageWrapper, session, false);
				} catch (Exception e) {
					log.warn("Caught exception in Receiver:", e);
				} finally {
					semaphore.release();
				}
			}
		};

		// Act
		mockListenerThread.start();
		semaphore.acquire(); // Wait until thread is finished.

		// Assert
		verify(jmsMessage, atLeastOnce()).acknowledge();
	}


	@Test
	public void testPollGuardStartTimeout() throws Exception {
		// TODO: This test should test the actual PullingJmsListener with SpringJmsConnector
		// Arrange
		configuration = buildConfiguration(TransactionManagerType.NARAYANA);

		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListenerWithPollGuard(0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<jakarta.jms.Message> receiver = setupReceiver(listener);
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
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); // Don't continue until the receiver has been started.

		// Act
		// From here the PollGuard should be triggering startup-delay timeout-guard
		listener.setStartupDelay(100_000);

		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
		await().atMost(5, TimeUnit.SECONDS)
				.until(receiver::getRunState, equalTo(RunState.EXCEPTION_STARTING));

		// Assert
		assertEquals(RunState.EXCEPTION_STARTING, receiver.getRunState());

		List<String> errors = new ArrayList<>(adapter.getMessageKeeper())
				.stream()
				.filter(msg -> msg != null && "ERROR".equals(msg.getMessageLevel()))
				.map(Object::toString)
				.toList();

		assertThat(errors, hasItem(containsString("Failed to restart receiver")));

		// After
		configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState: {}", receiver.getRunState());

		assertEquals(RunState.STOPPED, receiver.getRunState());
	}

	@Test
	public void testPollGuardStopTimeout() throws Exception {
		// TODO: This test should test the actual PullingJmsListener with SpringJmsConnector
		configuration = buildConfiguration(TransactionManagerType.NARAYANA);
		// Create listener without any delays in starting or stopping, they will be set later
		SlowListenerWithPollGuard listener = createSlowListenerWithPollGuard(0, 0);
		listener.setPollGuardInterval(1_000);
		listener.setMockLastPollDelayMs(10_000); // Last Poll always before PollGuard triggered

		Receiver<jakarta.jms.Message> receiver = setupReceiver(listener);
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
		assertEquals(RunState.STARTED, adapter.getRunState());

		waitForState(receiver, RunState.STARTED); // Don't continue until the receiver has been started.

		// From here the PollGuard should be triggering stop-delay timeout-guard
		listener.setShutdownDelay(100_000);

//		log.warn("Test sleeping to let poll guard timer run and do its work for a while");
//		Thread.sleep(5_000);
//		log.warn("Test resuming");

		// Receiver may be in state "stopping" (by PollGuard) or in state "starting" while we come out of sleep, so wait until it's started
		waitForState(receiver, RunState.STARTED);

		configuration.getIbisManager().handleAction(Action.STOPRECEIVER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		waitWhileInState(receiver, RunState.STARTED);
		waitWhileInState(receiver, RunState.STOPPING);
		log.info("Receiver RunState: {}", receiver.getRunState());

		assertEquals(RunState.EXCEPTION_STOPPING, receiver.getRunState());

		List<String> warnings = new ArrayList<>(adapter.getMessageKeeper())
				.stream()
				.filter(msg -> msg != null && "WARN".equals(msg.getMessageLevel()))
				.map(Object::toString)
				.toList();
		assertThat(warnings, everyItem(containsString("JMS poll timeout")));
	}
}
