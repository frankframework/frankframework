package org.frankframework.receivers;

import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;

public class JavaListenerTest {
	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<String> receiver;
	private JavaListener<String> listener;
	private PipeLineSession session;

	@BeforeEach
	public void setUp() throws Exception {
		configuration = new TestConfiguration(false);
		listener = setupJavaListener();
		receiver = setupReceiver(listener);
		adapter = setupAdapter(receiver);
		configuration.configure();
		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown() throws Exception {
		CloseUtils.closeSilently(session, configuration);
	}

	Receiver<String> setupReceiver(JavaListener<String> listener) {
		Receiver<String> receiver = configuration.createBean(Receiver.class);
		receiver.setListener(listener);
		receiver.setName("receiver");
		DummySender sender = configuration.createBean(DummySender.class);
		receiver.setSender(sender);

		NarayanaJtaTransactionManager transactionManager = configuration.createBean(NarayanaJtaTransactionManager.class);
		receiver.setTxManager(transactionManager);

		return receiver;
	}

	JavaListener<String> setupJavaListener() {
		JavaListener<String> listener = spy(configuration.createBean(JavaListener.class));
		listener.setReturnedSessionKeys("copy-this,this-doesnt-exist");
		return listener;
	}

	<M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {
		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName("ReceiverTestAdapterName");

		CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
		PipeProcessor pipeProcessor = new CorePipeProcessor();
		pipeLineProcessor.setPipeProcessor(pipeProcessor);

		PipeLine pl = spy(configuration.createBean(PipeLine.class));
		pl.setFirstPipe("dummy");
		pl.setPipeLineProcessor(pipeLineProcessor);

		EchoPipe pipe = new EchoPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) {
				session.put("key-not-configured-for-copy", "dummy");
				session.put("copy-this", "return-value");
				return super.doPipe(message, session);
			}
		};
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(PipeLine.ExitState.SUCCESS);
		pl.addPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.addReceiver(receiver);
		configuration.addAdapter(adapter);
		return adapter;
	}

	void startAdapter() {
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);
	}

	@Test
	public void testProcessRequestString() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		listener.setThrowException(true);

		// start adapter
		startAdapter();

		// Act
		String result = listener.processRequest("correlation-id", rawTestMessage, null);

		// Assert
		assertEquals(rawTestMessage, result);
	}

	@Test
	public void testProcessRequestMessage() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest(testMessage, session);

		// Assert
		assertTrue(result.requiresStream(), "Result message should be a stream");
		assertTrue(result.isRequestOfType(Reader.class), "Result message should be of type Reader");
		assertEquals(rawTestMessage, result.asString());
		testMessage.close();
	}

	@Test
	public void testProcessRequestMessageWithHttpRequest() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		HttpServletRequest servletRequest = mock(HttpServletRequest.class);
		session.put(PipeLineSession.HTTP_REQUEST_KEY, servletRequest);

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest(testMessage, session);

		// Assert
		assertTrue(result.requiresStream(), "Result message should be a stream");
		assertTrue(result.isRequestOfType(Reader.class), "Result message should be of type Reader");
		assertEquals(rawTestMessage, result.asString());
		result.close();
	}

	@Test
	public void testProcessRequestMessageWithException() throws Exception {
		// Arrange 1
		Message testMessage = new Message(new StringReader("TEST"));
		listener.setThrowException(true);
		assertSame(Receiver.OnError.CONTINUE, receiver.getOnError()); // Validate default setting: in state CONTINUE after an error occurs

		PipeLine pl = adapter.getPipeLine();
		doThrow(PipeRunException.class).when(pl).process(any(), any(), any());

		startAdapter();

		// Act / Assert 1 - OnError.CONTINUE
		assertThrows(ListenerException.class, () -> listener.processRequest(testMessage, session));
		assertSame(RunState.STARTED, receiver.getRunState(), "Receiver should still be in state STARTED");

		// Arrange 2
		receiver.setOnError(Receiver.OnError.CLOSE); // Put receiver in state STOPPED after an error occurs

		// Act / Assert 2 - OnError.CLOSE
		assertThrows(ListenerException.class, () -> listener.processRequest(testMessage, session));
		assertSame(RunState.STOPPED, receiver.getRunState(), "Receiver should be in state STOPPED");
		testMessage.close();
	}

	@Test
	public void testProcessRequestMessageWithExceptionDoNotThrow() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));
		listener.setThrowException(false);

		PipeLine pipeLine = adapter.getPipeLine();
		doThrow(new PipeRunException(pipeLine.getPipe(0), "FAILED PIPE")).when(pipeLine).process(any(), any(), any());

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest(testMessage, session);

		// Assert
		String resultString = result.asString();
		assertTrue(resultString.contains("FAILED PIPE"), "Result message should contain string 'FAILED PIPE'");
		assertTrue(resultString.startsWith("<errorMessage"), "Result message should start with string '<errorMessage'");
		result.close();
		testMessage.close();
	}

	@Test
	public void testProcessRequestWithReturnSessionKeys() throws Exception {
		// Arrange
		Message testMessage = new Message(new StringReader("TEST"));
		session.put("copy-this", "original-value");

		// start adapter
		startAdapter();

		// Act
		listener.processRequest(testMessage, session);

		// Assert
		assertAll(
			() -> assertFalse(session.containsKey("key-not-configured-for-copy"), "Session should not contain key 'key-not-configured-for-copy'"),
			() -> assertEquals("return-value", session.get("copy-this")),
			() -> assertTrue(session.containsKey("this-doesnt-exist"), "After request the pipeline-session should contain key [this-doesnt-exist]"),
			() -> assertNull(session.get("this-doesnt-exist"), "Key not in return from service should have value [NULL]")
		);
		testMessage.close();
	}

	@Test
	public void testProcessRequestWithReturnSessionKeysWhenNoneConfigured() throws Exception {
		// Arrange
		Message testMessage = new Message(new StringReader("TEST"));
		listener.setReturnedSessionKeys(null);
		session.put("copy-this", "original-value");

		// start adapter
		startAdapter();

		// Act
		listener.processRequest(testMessage, session);

		// Assert
		assertAll(
				() -> assertTrue(session.containsKey("key-not-configured-for-copy"), "Session should contain key 'key-not-configured-for-copy'"),
				() -> assertEquals("dummy", session.get("key-not-configured-for-copy")),
				() -> assertEquals("return-value", session.get("copy-this")),
				() -> assertFalse(session.containsKey("this-doesnt-exist"), "After request the pipeline-session should not contain key [this-doesnt-exist]")
		);
		testMessage.close();
	}

	@Test
	public void testProcessRequestStringWithReturnSessionKeys() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		HashMap<String, String> context = new HashMap<>();
		context.put("copy-this", "original-value");

		// start adapter
		startAdapter();

		// Act
		listener.processRequest("correlation-id", rawTestMessage, context);

		// Assert
		assertAll(
			() -> assertFalse(context.containsKey("key-not-configured-for-copy"), "Session should not contain key 'key-not-configured-for-copy'"),
			() -> assertEquals("return-value", context.get("copy-this")),
			() -> assertTrue(context.containsKey("this-doesnt-exist"), "After request the pipeline-session should not contain key [this-doesnt-exist]"),
			() -> assertNull(session.get("this-doesnt-exist"), "Key not in return from service should have value [NULL]")
		);
	}

	@Test
	public void testProcessRequestStringWithReturnSessionKeysWhenNoneConfigured() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		HashMap<String, String> context = new HashMap<>();
		context.put("copy-this", "original-value");
		listener.setReturnedSessionKeys(null);

		// start adapter
		startAdapter();

		// Act
		listener.processRequest("correlation-id", rawTestMessage, context);

		// Assert
		assertAll(
			() -> assertTrue(context.containsKey("key-not-configured-for-copy"), "Session should contain key 'key-not-configured-for-copy'"),
			() -> assertEquals("dummy", context.get("key-not-configured-for-copy")),
			() -> assertEquals("return-value", context.get("copy-this")),
			() -> assertFalse(context.containsKey("this-doesnt-exist"), "After request the pipeline-session should not contain key [this-doesnt-exist]")
		);
	}
}
