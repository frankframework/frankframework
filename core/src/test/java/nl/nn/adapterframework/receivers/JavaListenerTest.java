package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IManagable;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jta.narayana.NarayanaJtaTransactionManager;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.processors.PipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunState;

public class JavaListenerTest {
	protected final static Logger LOG = LogUtil.getLogger(JavaListenerTest.class);

	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<String> receiver;
	private JavaListener<String> listener;

	@Before
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		configuration.stop();
		configuration.getBean("adapterManager", AdapterManager.class).close();

		listener = setupJavaListener();
		receiver = setupReceiver(listener);
		adapter = setupAdapter(receiver);
	}

	@After
	public void tearDown() throws Exception {
		configuration.getIbisManager().handleAction(IbisManager.IbisAction.STOPADAPTER, configuration.getName(), adapter.getName(), receiver.getName(), null, true);

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

	void waitWhileInState(IManagable object, RunState state) {
		while(object.getRunState()==state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
	}
	void waitForState(IManagable object, RunState state) {
		while(object.getRunState()!=state) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				fail("test interrupted");
			}
		}
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

		Map<String, Object> context = new HashMap<>();

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest("correlation-id", testMessage, context);

		// Assert
		assertTrue("Result message should be a stream", result.requiresStream());
		assertTrue("Result message should be a stream", result.asObject() instanceof Reader);
		assertEquals("TEST", result.asString());
	}

	@Test
	public void testProcessRequestMessageWithHttpRequest() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
		listener.setThrowException(true);

		Map<String, Object> context = new HashMap<>();

		HttpServletRequest servletRequest = mock(HttpServletRequest.class);
		context.put("httpRequest", servletRequest);

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest("correlation-id", testMessage, context);

		// Assert
		assertTrue("Result message should be a stream", result.requiresStream());
		assertTrue("Result message should be a stream", result.asObject() instanceof Reader);
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

		Map<String, Object> context = new HashMap<>();

		// start adapter
		startAdapter();

		// Act / Assert
		assertThrows(ListenerException.class,() -> listener.processRequest("correlation-id", testMessage, context));
	}

	@Test
	public void testProcessRequestMessageWithExceptionDoNotThrow() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = Message.asMessage(new StringReader(rawTestMessage));
		listener.setThrowException(false);

		PipeLine pipeLine = adapter.getPipeLine();
		doThrow(new PipeRunException(pipeLine.getPipe(0), "FAILED PIPE")).when(pipeLine).process(any(), any(), any());

		Map<String, Object> context = new HashMap<>();

		// start adapter
		startAdapter();

		// Act
		Message result = listener.processRequest("correlation-id", testMessage, context);

		// Assert
		String resultString = result.asString();
		assertTrue("Result message should contain string 'FAILED PIPE'", resultString.contains("FAILED PIPE"));
		assertTrue("Result message should start with string '<errorMessage'", resultString.startsWith("<errorMessage"));
	}
}
