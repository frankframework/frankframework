/*
   Copyright 2024 WeAreFrank!

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
import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.receivers.Receiver.OnError;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.RunState;

@Log4j2
@Tag("slow")
public class TestReceiverOnError {
	private static final TestConfiguration configuration = TransactionManagerType.DATASOURCE.create(false);
	private String adapterName;

	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		adapterName = testInfo.getDisplayName().replace('/', '_');
	}

	@AfterEach
	void tearDown() throws Exception {
		log.info("!> tearing down test");
		configuration.stop();
		configuration.getBean("configurationMetrics", MetricsInitializer.class).destroy(); //Meters are cached...
		log.info("!> Configuration Context for [{}] has been cleaned up.", TransactionManagerType.DATASOURCE);
	}

	private <T extends MockListenerBase> T createListener(Class<T> listenerClass) {
		return configuration.createBean(listenerClass);
	}

	private Receiver<String> setupReceiver(MockListenerBase listener) {
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = spy(configuration.createBean(Receiver.class));
		configuration.autowireByName(listener);
		doNothing().when(receiver).suspendReceiverThread(anyInt());
		receiver.setListener(listener);
		receiver.setName("receiver");
		receiver.setStartTimeout(2);
		receiver.setStopTimeout(2);
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);
		return receiver;
	}

	private <M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {
		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName(adapterName);

		doAnswer(invocation -> {
			Message m = invocation.getArgument(1);
			if("processMessageException".equals(m.asString())) {
				throw new ListenerException(m.asString());
			}
			return invocation.callRealMethod();
		}).when(adapter).processMessageWithExceptions(anyString(), any(Message.class), any(PipeLineSession.class));

		PipeLine pl = spy(configuration.createBean(PipeLine.class));
		doAnswer(p -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setState(ExitState.SUCCESS);
			plr.setResult(p.getArgument(1));
			return plr;
		}).when(pl).process(anyString(), any(Message.class), any(PipeLineSession.class));
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

	private Receiver<String> startReceiver(MockListenerBase listener) throws Exception {
		Receiver<String> receiver = setupReceiver(listener);
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

		assertEquals(RunState.STARTED, receiver.getRunState());
		log.info("!> started the receiver, lets do some testing!");
		return receiver;
	}

	@ParameterizedTest
	@ValueSource(classes = {MockPushingListener.class, MockPullingListener.class})
	public <T extends MockListenerBase> void testNormalOperation(Class<T> listenerClass) throws Exception {
		MockListenerBase listener = createListener(listenerClass);
		Receiver<String> receiver = startReceiver(listener);

		// Act
		listener.offerMessage("Test Message");
		await()
			.atMost(30, TimeUnit.SECONDS)
			.pollInterval(100, TimeUnit.MILLISECONDS)
			.until(() -> receiver.getMessagesReceived() > 0);

		// Assert the receiver is still in state started and has processed a message
		assertEquals(RunState.STARTED, receiver.getRunState());
		assertEquals(1, receiver.getMessagesReceived());
		assertNotEquals(0, receiver.getLastMessageDate()); // Make sure we've processed a message
		assertTrue(System.currentTimeMillis() + 200 > receiver.getLastMessageDate());
	}

	@ParameterizedTest(name = "{index} - {0}")
	@CsvSource({
		"getRawMessageException, Receiver [receiver] caught Exception retrieving message, will continue retrieving messages",
		"extractMessageException, Receiver [receiver] caught Exception processing message, will continue processing next message",
		"processMessageException, Receiver [receiver] Exception in message processing"
	})
	public void testPullingListenerWithExceptionAndOnErrorContinue(final String message, final String logMessage) throws Exception {
		MockListenerBase listener = createListener(MockPullingListener.class);
		Receiver<String> receiver = startReceiver(listener);
		receiver.setOnError(OnError.CONTINUE); //Luckily we can change this runtime...

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			// Act
			listener.offerMessage(message);
			await()
					.atMost(30, TimeUnit.SECONDS)
					.pollInterval(100, TimeUnit.MILLISECONDS)
					.until(() -> appender.contains(logMessage));

			// Assert the Receiver state
			assertEquals(RunState.STARTED, receiver.getRunState());
			assertTrue(System.currentTimeMillis() + 200 > receiver.getLastMessageDate());
		}
	}

	@ParameterizedTest
	@CsvSource({
		"getRawMessageException, Receiver [receiver] exception occurred while retrieving message, stopping receiver",
		"extractMessageException, Receiver [receiver] caught Exception processing message",
		"processMessageException, Receiver [receiver] exception occurred while processing message, stopping receiver",
	})
	public void testPullingListenerWithExceptionAndOnErrorClose(final String message, final String logMessage) throws Exception {
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			MockListenerBase listener = createListener(MockPullingListener.class);
			Receiver<String> receiver = startReceiver(listener);
			receiver.setOnError(OnError.CLOSE); //Luckily we can change this runtime...

			// Act
			listener.offerMessage(message);
			await()
					.atMost(30, TimeUnit.SECONDS)
					.pollInterval(100, TimeUnit.MILLISECONDS)
					.until(() -> appender.contains(logMessage));

			// Assert the Receiver state
			waitWhileInState(receiver, RunState.STARTED);
			waitForState(receiver, RunState.STOPPING, RunState.STOPPED);
			assertEquals(RunState.STOPPED, receiver.getRunState());
		}
	}

	@Test
	public void testPushingListenerWithExceptionAndOnErrorContinue() throws Exception {
		MockListenerBase listener = createListener(MockPushingListener.class);
		Receiver<String> receiver = startReceiver(listener);
		receiver.setOnError(OnError.CONTINUE); //Luckily we can change this runtime...

		// Act
		assertThrows(ListenerException.class, () -> listener.offerMessage("processMessageException"));

		// Assert the Receiver state
		assertEquals(RunState.STARTED, receiver.getRunState());
		assertTrue(System.currentTimeMillis() + 200 > receiver.getLastMessageDate());
		assertEquals(ExitState.ERROR, listener.getLastExitState());
	}

	@Test
	public void testPushingListenerWithExceptionAndOnErrorClose() throws Exception {
		MockListenerBase listener = createListener(MockPushingListener.class);
		Receiver<String> receiver = startReceiver(listener);
		receiver.setOnError(OnError.CLOSE); //Luckily we can change this runtime...

		// Act
		assertThrows(ListenerException.class, () -> listener.offerMessage("processMessageException"));

		// Assert the Receiver state
		waitWhileInState(receiver, RunState.STARTED);
		waitForState(receiver, RunState.STOPPING, RunState.STOPPED);
		assertEquals(RunState.STOPPED, receiver.getRunState());
		assertEquals(ExitState.ERROR, listener.getLastExitState());
	}
}
