package org.frankframework.processors;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.TransactionManagerMock;
import org.frankframework.util.SpringUtils;

public class PipeProcessorTestBase {

	private PipeProcessor processor;
	protected PipeLineSession session;
	private TestConfiguration configuration;
	private PipeLine pipeLine;
	private Adapter adapter;

	@BeforeEach
	public void setUp() {
		configuration = new TestConfiguration();
		TransactionManagerMock txManager = configuration.createBean(TransactionManagerMock.class);
		SpringUtils.registerSingleton(configuration, "txManager", txManager);
		processor = configuration.getBean(PipeProcessor.class);

		createAdapterAndPipeLine();
		session = new PipeLineSession();
	}

	@SuppressWarnings("deprecation")
	private void createAdapterAndPipeLine() {
		adapter = configuration.createBean();
		adapter.setName("Adapter Name");
		pipeLine = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pipeLine);

		PipeLineExit errorExit = new PipeLineExit();
		errorExit.setName("error");
		errorExit.setState(PipeLine.ExitState.ERROR);
		pipeLine.addPipeLineExit(errorExit);

		PipeLineExit successExit = new PipeLineExit();
		successExit.setName("exit");
		successExit.setState(PipeLine.ExitState.SUCCESS);
		pipeLine.addPipeLineExit(successExit);
	}

	protected final void configurePipeLine(Consumer<PipeLine> additionalConfig) {
		additionalConfig.accept(pipeLine);
	}

	protected final Message processPipeLine(Message inputMessage) throws PipeRunException, ConfigurationException {
		adapter.configure();

		CorePipeLineProcessor cpp = configuration.createBean();
		cpp.setPipeProcessor(processor);
		String firstPipe = pipeLine.getPipe(0).getName();
		PipeLineResult plr = cpp.processPipeLine(pipeLine, "mid", inputMessage, session, firstPipe);
		return plr.getResult();
	}

	protected final <T extends IPipe> T createPipe(Class<T> className, String pipeName, String forwardName) throws ConfigurationException {
		return createPipe(className, pipeName, forwardName, null);
	}

	protected final <T extends IPipe> T createPipe(Class<T> className, String pipeName, String forwardName, Consumer<T> additionalConfig) throws ConfigurationException {
		T pipe = SpringUtils.createBean(adapter, className);
		pipe.setName(pipeName);

		PipeForward forward = new PipeForward();
		forward.setName(forwardName);
		forward.setPath("exit");
		pipe.addForward(forward);

		if (additionalConfig != null) {
			additionalConfig.accept(pipe);
		}

		pipe.setPipeLine(pipeLine);
		pipeLine.addPipe(pipe);

		return pipe;
	}
}
