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
package nl.nn.adapterframework.receivers;

import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitForState;
import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.receivers.Receiver.OnError;
import nl.nn.adapterframework.statistics.MetricsInitializer;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.util.RunState;

@Log4j2
public class TestReceiverOnError {
	private static TestConfiguration configuration = TransactionManagerType.DATASOURCE.create();
	private TestAppender appender;

	@BeforeEach
	void setup() throws Exception {
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
		configuration.getBean("configurationMetrics", MetricsInitializer.class).destroy(); //Meters are cached...
		log.info("!> Configuration Context for [{}] has been created.", TransactionManagerType.DATASOURCE);
	}

	@AfterEach
	void tearDown() {
		log.info("!> tearing down test");
		if (appender != null) {
			TestAppender.removeAppender(appender);
			appender = null;
		}
	}

	private <T extends MockListenerBase> T createListener(Class<T> listenerClass) {
		return configuration.createBean(listenerClass);
	}

	private Receiver<String> setupReceiver(MockListenerBase listener) {
		@SuppressWarnings("unchecked")
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		configuration.autowireByName(listener);
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
		adapter.setName("ReceiverTestAdapterName");

		doAnswer(invocation -> {
			Message m = invocation.getArgument(1);
			if(m.asString().equals("processMessageException")) {
				throw new ListenerException(m.asString());
			}
			return invocation.callRealMethod();
		}).when(adapter).processMessageWithExceptions(anyString(), any(Message.class), any(PipeLineSession.class));

		PipeLine pl = spy(new PipeLine());
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
	public <T extends MockListenerBase> void testNormalOperation(Class<T> pullingListener) throws Exception {
		MockListenerBase listener = createListener(pullingListener);
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

	@ParameterizedTest
	@CsvSource({
		"getRawMessageException, Receiver [receiver] caught Exception retrieving message, will continue retrieving messages",
		"extractMessageException, Receiver [receiver] caught Exception processing message, will continue processing next message",
		"processMessageException, Receiver [receiver] Exception in message processing"
	})
	public void testPullingListenerWithExceptionAndOnErrorContinue(final String message, final String logMessage) throws Exception {
		MockListenerBase listener = createListener(MockPullingListener.class);
		Receiver<String> receiver = startReceiver(listener);
		receiver.setOnError(OnError.CONTINUE); //Luckily we can change this runtime...

		appender = TestAppender.newBuilder().build();
		TestAppender.addToRootLogger(appender);

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

	@ParameterizedTest
	@CsvSource({
		"getRawMessageException, Receiver [receiver] exception occurred while retrieving message, stopping receiver",
		"extractMessageException, Receiver [receiver] caught Exception processing message",
		"processMessageException, Receiver [receiver] exception occurred while processing message, stopping receiver",
	})
	public void testPullingListenerWithExceptionAndOnErrorClose(final String message, final String logMessage) throws Exception {
		MockListenerBase listener = createListener(MockPullingListener.class);
		Receiver<String> receiver = startReceiver(listener);
		receiver.setOnError(OnError.CLOSE); //Luckily we can change this runtime...

		appender = TestAppender.newBuilder().build();
		TestAppender.addToRootLogger(appender);

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
	}
}