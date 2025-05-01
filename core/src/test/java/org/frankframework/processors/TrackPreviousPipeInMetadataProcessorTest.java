package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.mock.TransactionManagerMock;
import org.frankframework.util.SpringUtils;

public class TrackPreviousPipeInMetadataProcessorTest {
	private static final String INPUT_MESSAGE_TEXT = "input message";

	private PipeProcessor processor;
	private PipeLineSession session;
	private TestConfiguration configuration;

	@BeforeEach
	public void setUp() {
		configuration = new TestConfiguration();
		TransactionManagerMock txManager = configuration.createBean(TransactionManagerMock.class);
		SpringUtils.registerSingleton(configuration, "txManager", txManager);
		processor = configuration.getBean(PipeProcessor.class);
		session = new PipeLineSession();
	}

	@Test
	void testPreviousPipeValue() throws Exception {
		EchoPipe pipe = getEchoPipe(getPipeLine(), "Echo pipe", "success", null);
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		PipeRunResult prr = processor.processPipe(getPipeLine(), pipe, input, session);

		assertTrue(prr.getResult().getContext().containsKey(MessageContext.CONTEXT_PREVIOUS_PIPE));
		assertEquals("Echo pipe", prr.getResult().getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
		assertEquals(INPUT_MESSAGE_TEXT, prr.getResult().asString());
	}

	public static Stream<Arguments> conditionalIfParamArguments() {
		return Stream.of(
				// ifValue, expectedPreviousPipeValue
				Arguments.of("echo1", "echo2"),
				Arguments.of("blabla", "echo1"),
				Arguments.of("echo2", "echo1")
		);
	}

	@MethodSource("conditionalIfParamArguments")
	@ParameterizedTest
	void testConditionalIfParamTrue(String ifValue, String expectedPreviousPipeValue) throws Exception {
		PipeLine pipeLine = getPipeLine();

		getEchoPipe(pipeLine, "echo1", "echo2", null);

		Parameter parameter = new Parameter();
		parameter.setName("paramInput");
		parameter.setContextKey(MessageContext.CONTEXT_PREVIOUS_PIPE);
		parameter.configure();

		getEchoPipe(pipeLine, "echo2", "exit", echoPipe -> {
			// Provide parameter with value of context key for last pipe
			echoPipe.addParameter(parameter);

			echoPipe.setIfParam("paramInput");
			echoPipe.setIfValue(ifValue);

			return null;
		});

		pipeLine.configure();

		CorePipeLineProcessor cpp = configuration.createBean();
		cpp.setPipeProcessor(processor);
		PipeLineResult pipeLineResult = cpp.processPipeLine(pipeLine, "id", new Message(INPUT_MESSAGE_TEXT), new PipeLineSession(), "echo1");

		// assert here that the previous pipe value equals expectedPreviousPipeValue
		assertEquals(expectedPreviousPipeValue, pipeLineResult.getResult().getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
	}

	@Test
	void testPreviousPipeValueInCombinationWithPostProcessPipeResult() throws Exception {
		// Arrange
		PipeLine pipeLine = getPipeLine();
		EchoPipe pipe = getEchoPipe(pipeLine, "Echo pipe", "success", null);
		pipe.setRestoreMovedElements(true);
		pipe.configure();
		pipe.start();
		session.put("mineraalwater", "water");

		Message input = new Message("<xml>{sessionKey:mineraalwater}</xml>");

		// Act
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);

		// Assert
		assertEquals("<xml>water</xml>", prr.getResult().asString());

// TODO fix this
//		assertTrue(prr.getResult().getContext().containsKey(MessageContext.CONTEXT_PREVIOUS_PIPE));
//		assertEquals("Echo pipe", prr.getResult().getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
	}

	private PipeLine getPipeLine() {
		PipeLine pipeLine = configuration.createBean();
		Adapter owner = configuration.createBean();
		owner.setName("PipeLine owner");
		pipeLine.setApplicationContext(owner);

		PipeLineExit errorExit = new PipeLineExit();
		errorExit.setName("error");
		errorExit.setState(PipeLine.ExitState.ERROR);
		pipeLine.addPipeLineExit(errorExit);

		PipeLineExit successExit = new PipeLineExit();
		successExit.setName("exit");
		successExit.setState(PipeLine.ExitState.SUCCESS);
		pipeLine.addPipeLineExit(successExit);

		return pipeLine;
	}

	private EchoPipe getEchoPipe(PipeLine pipeLine, String pipeName, String forwardName, Function<EchoPipe, Void> additionalConfig) throws ConfigurationException {
		EchoPipe pipe = configuration.createBean();
		pipe.setName(pipeName);

		PipeForward forward = new PipeForward();
		forward.setName(forwardName);
		pipe.addForward(forward);

		if (additionalConfig != null) {
			additionalConfig.apply(pipe);
		}

		pipe.setPipeLine(pipeLine);
		pipeLine.addPipe(pipe);

		return pipe;
	}
}
