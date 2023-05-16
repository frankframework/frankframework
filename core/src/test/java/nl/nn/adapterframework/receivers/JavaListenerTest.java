package nl.nn.adapterframework.receivers;

import static nl.nn.adapterframework.testutil.mock.WaitUtils.waitForState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.management.IbisAction;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.processors.PipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.RunState;

public class JavaListenerTest {
	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<String> receiver;
	private JavaListener<String> listener;
	private PipeLineSession session;

	@BeforeEach
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();

		listener = setupJavaListener();
		receiver = setupReceiver(listener);
		adapter = setupAdapter(receiver);
		session = new PipeLineSession();
	}

	@AfterEach
	public void tearDown() throws Exception {
		session.close();
		configuration.getIbisManager().handleAction(IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();
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
		return listener;
	}

	<M> Adapter setupAdapter(Receiver<M> receiver) throws Exception {

		Adapter adapter = spy(configuration.createBean(Adapter.class));
		adapter.setName("ReceiverTestAdapterName");

		CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
		PipeProcessor pipeProcessor = new CorePipeProcessor();
		pipeLineProcessor.setPipeProcessor(pipeProcessor);

		PipeLine pl = spy(new PipeLine());
		pl.setFirstPipe("dummy");
		pl.setPipeLineProcessor(pipeLineProcessor);

		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState(PipeLine.ExitState.SUCCESS);
		pl.registerPipeLineExit(ple);
		adapter.setPipeLine(pl);

		adapter.registerReceiver(receiver);
		configuration.registerAdapter(adapter);
		return adapter;
	}

	void startAdapter() throws ConfigurationException {
		configuration.configure();
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);
	}

	@Test
	public void testProcessRequestString() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		HashMap context = new HashMap<>();
		listener.setThrowException(true);

		// start adapter
		startAdapter();

		// Act
		String result = listener.processRequest("correlation-id", rawTestMessage, context);

		// Assert
		assertEquals("TEST", result);
	}

	@Test
	public void testProcessRequestMessage() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest(testMessage, session);

		// Assert
		assertTrue(result.requiresStream(), "Result message should be a stream");
		assertTrue(result.asObject() instanceof Reader, "Result message should be a stream");
		assertEquals("TEST", result.asString());
	}

	@Test
	public void testProcessRequestMessageWithHttpRequest() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		HttpServletRequest servletRequest = mock(HttpServletRequest.class);
		session.put("httpRequest", servletRequest);

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest(testMessage, session);

		// Assert
		assertTrue(result.requiresStream(), "Result message should be a stream");
		assertTrue(result.asObject() instanceof Reader, "Result message should be a stream");
		assertEquals("TEST", result.asString());
	}

	@Test
	public void testProcessRequestMessageWithException() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		PipeLine pl = adapter.getPipeLine();
		doThrow(PipeRunException.class).when(pl).process(any(), any(), any());

		// start adapter
		startAdapter();

		// Act / Assert
		assertThrows(ListenerException.class,() -> listener.processRequest(testMessage, session));
	}

	@Test
	public void testProcessRequestMessageWithExceptionDoNotThrow() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
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
	}
}
