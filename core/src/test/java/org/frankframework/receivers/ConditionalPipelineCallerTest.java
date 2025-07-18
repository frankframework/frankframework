package org.frankframework.receivers;

import static org.frankframework.testutil.mock.WaitUtils.waitForState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.jta.narayana.NarayanaJtaTransactionManager;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

@SuppressWarnings("unchecked")
public class ConditionalPipelineCallerTest {
	private static final String SESSION_VALUE_ECHO_PIPE_CALLED = "echo-pipe-was-called";
	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<String> receiver;
	private JavaListener<String> listener;
	private PipeLineSession session;

	@BeforeEach
	public void setUp() throws Exception {
		configuration = new TestConfiguration();
		adapter = setupAdapter();
		listener = setupJavaListener();
		receiver = setupReceiver(listener);
		session = new PipeLineSession();
	}

	Receiver<String> setupReceiver(JavaListener<String> listener) {
		Receiver<String> receiverBean = SpringUtils.createBean(adapter);
		receiverBean.setListener(listener);
		receiverBean.setName("receiver");
		DummySender sender = SpringUtils.createBean(adapter);
		receiverBean.setSender(sender);

		NarayanaJtaTransactionManager transactionManager = configuration.createBean();
		receiverBean.setTxManager(transactionManager);
		adapter.addReceiver(receiverBean);

		return receiverBean;
	}

	JavaListener<String> setupJavaListener() {
		JavaListener<String> listenerBean = spy(SpringUtils.createBean(adapter, JavaListener.class));
		listenerBean.setReturnedSessionKeys(SESSION_VALUE_ECHO_PIPE_CALLED);
		listenerBean.setThrowException(true);

		return listenerBean;
	}

	<M> Adapter setupAdapter() throws Exception {
		Adapter adapterBean = configuration.createBean(Adapter.class);
		adapterBean.setName("ReceiverTestAdapterName");

		// Correctly chain the pipe processors
		CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
		InputOutputPipeProcessor inputOutputPipeProcessor = new InputOutputPipeProcessor();
		PipeProcessor pipeProcessor = new CorePipeProcessor();
		inputOutputPipeProcessor.setPipeProcessor(pipeProcessor);

		pipeLineProcessor.setPipeProcessor(inputOutputPipeProcessor);

		PipeLine pl = SpringUtils.createBean(adapterBean, PipeLine.class);
		pl.setFirstPipe("dummy");
		pl.setPipeLineProcessor(pipeLineProcessor);

		EchoPipe pipe = new EchoPipe() {
			@Override
			public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
				session.put(SESSION_VALUE_ECHO_PIPE_CALLED, "true");
				return super.doPipe(message, session);
			}
		};
		pipe.setName("dummy");
		pl.addPipe(pipe);

		PipeLineExit ple = new PipeLineExit();
		ple.setName("success");
		ple.setState(PipeLine.ExitState.SUCCESS);
		pl.addPipeLineExit(ple);
		adapterBean.setPipeLine(pl);

		configuration.addAdapter(adapterBean);
		return adapterBean;
	}

	void startAdapter() throws ConfigurationException {
		configuration.configure();
		configuration.start();

		waitForState(adapter, RunState.STARTED);
		waitForState(receiver, RunState.STARTED);
	}

	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(session, configuration);
	}

	@Test
	void testPipelineCaller() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));

		// Setup the echopipe to only run when the pipeline caller equals 'listener of [receiver]'
		configureEchoPipe("listener of [receiver]");

		// start adapter
		startAdapter();

		// Act
		listener.processRequest(testMessage, session);

		// Was set to 'true' in custom EchoPipe implementation in #setupAdapter
		assertTrue(session.containsKey(SESSION_VALUE_ECHO_PIPE_CALLED));
		assertEquals("true", session.get(SESSION_VALUE_ECHO_PIPE_CALLED));
	}

	@Test
	void testPipelineCallerNoMatch() throws Exception {
		// Arrange
		String rawTestMessage = "TEST";
		Message testMessage = new Message(new StringReader(rawTestMessage));

		// This should never match with the listenerName
		configureEchoPipe("niet goed");

		// start adapter
		startAdapter();

		// Act
		listener.processRequest(testMessage, session);

		// Was not set to 'true' in custom EchoPipe implementation in #setupAdapter.
		assertTrue(session.containsKey(SESSION_VALUE_ECHO_PIPE_CALLED));
		assertNull(session.get(SESSION_VALUE_ECHO_PIPE_CALLED));
	}

	private void configureEchoPipe(String ifValue) throws ConfigurationException {
		EchoPipe echoPipe = (EchoPipe) adapter.getPipeLine().getPipe("dummy");
		echoPipe.setIfParam("paramInput");
		echoPipe.setIfValue(ifValue);

		Parameter parameter = new Parameter();
		parameter.setName("paramInput");
		parameter.setContextKey(MessageContext.CONTEXT_PIPELINE_CALLER);
		parameter.configure();

		echoPipe.addParameter(parameter);
	}
}
