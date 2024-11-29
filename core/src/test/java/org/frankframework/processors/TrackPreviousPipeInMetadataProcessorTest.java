package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;

public class TrackPreviousPipeInMetadataProcessorTest {
	private static final String INPUT_MESSAGE_TEXT = "input message";

	private TrackPreviousPipeInMetadataProcessor processor;
	private PipeLineSession session;
	private TestConfiguration configuration;

	@BeforeEach
	public void setUp() {
		configuration = new TestConfiguration();
		processor = new TrackPreviousPipeInMetadataProcessor();

		PipeProcessor chain = new PipeProcessor() {
			@Override
			public PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession) throws PipeRunException {
				return pipe.doPipe(message, pipeLineSession);
			}

			@Override
			public PipeRunResult validate(@Nonnull PipeLine pipeLine, @Nonnull IValidator validator, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
				return validator.validate(message, pipeLineSession, messageRoot);
			}
		};

		processor.setPipeProcessor(chain);
		session = new PipeLineSession();
	}

	@Test
	void testPreviousPipeValue() throws Exception {
		EchoPipe pipe = getEchoPipe(null, "Echo pipe", "success", null);
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		PipeRunResult prr = processor.processPipe(getPipeLine(), pipe, input, session);

		assertTrue(prr.getResult().getContext().containsKey(TrackPreviousPipeInMetadataProcessor.CONTEXT_PREVIOUS_PIPE));
		assertEquals("Echo pipe", prr.getResult().getContext().get(TrackPreviousPipeInMetadataProcessor.CONTEXT_PREVIOUS_PIPE));
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
		parameter.setContextKey(TrackPreviousPipeInMetadataProcessor.CONTEXT_PREVIOUS_PIPE);
		parameter.configure();

		getEchoPipe(pipeLine, "echo2", "exit", echoPipe -> {
			// Provide parameter with value of context key for last pipe
			echoPipe.addParameter(parameter);

			echoPipe.setIfParam("paramInput");
			echoPipe.setIfValue(ifValue);

			return null;
		});

		pipeLine.configure();

		CorePipeLineProcessor cpp = configuration.createBean(CorePipeLineProcessor.class);
		cpp.setPipeProcessor(processor);
		PipeLineResult pipeLineResult = cpp.processPipeLine(pipeLine, "id", new Message(INPUT_MESSAGE_TEXT), new PipeLineSession(), "echo1");

		// assert here that the previous pipe value equals expectedPreviousPipeValue
		assertEquals(expectedPreviousPipeValue, pipeLineResult.getResult().getContext().get(TrackPreviousPipeInMetadataProcessor.CONTEXT_PREVIOUS_PIPE));
	}

	private PipeLine getPipeLine() {
		PipeLine pipeLine = configuration.createBean(PipeLine.class);
		Adapter owner = new Adapter();
		owner.setName("PipeLine owner");
		pipeLine.setOwner(owner);

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
		EchoPipe pipe = configuration.createBean(EchoPipe.class);
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
