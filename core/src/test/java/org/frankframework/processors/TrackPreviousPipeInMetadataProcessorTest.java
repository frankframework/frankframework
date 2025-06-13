package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;

public class TrackPreviousPipeInMetadataProcessorTest extends PipeProcessorTestBase {
	private static final String INPUT_MESSAGE_TEXT = "input message";

	@Test
	void testPreviousPipeValue() throws Exception {
		EchoPipe pipe = createPipe(EchoPipe.class, "Echo pipe", "success");
		pipe.configure();
		pipe.start();

		Message input = new Message(INPUT_MESSAGE_TEXT);

		Message result = processPipeLine(input);

		assertTrue(result.getContext().containsKey(MessageContext.CONTEXT_PREVIOUS_PIPE));
		assertEquals("Echo pipe", result.getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
		assertEquals(INPUT_MESSAGE_TEXT, result.asString());
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
		createPipe(EchoPipe.class, "echo1", "echo2");

		Parameter parameter = new Parameter();
		parameter.setName("paramInput");
		parameter.setContextKey(MessageContext.CONTEXT_PREVIOUS_PIPE);
		parameter.configure();

		createPipe(EchoPipe.class, "echo2", "exit", echoPipe -> {
			// Provide parameter with value of context key for last pipe
			echoPipe.addParameter(parameter);

			echoPipe.setIfParam("paramInput");
			echoPipe.setIfValue(ifValue);
		});

		Message result = processPipeLine(new Message(INPUT_MESSAGE_TEXT));

		// assert here that the previous pipe value equals expectedPreviousPipeValue
		assertEquals(expectedPreviousPipeValue, result.getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
	}

	@Test
	void testPreviousPipeValueInCombinationWithPostProcessPipeResult() throws Exception {
		// Arrange
		EchoPipe pipe = createPipe(EchoPipe.class, "Echo pipe", "success");
		pipe.setRestoreMovedElements(true);
		pipe.configure();
		pipe.start();
		session.put("mineraalwater", "water");

		Message input = new Message("<xml>{sessionKey:mineraalwater}</xml>");

		// Act
		Message result = processPipeLine(input);

		// Assert
		assertEquals("<xml>water</xml>", result.asString());

		assertTrue(result.getContext().containsKey(MessageContext.CONTEXT_PREVIOUS_PIPE));
		assertEquals("Echo pipe", result.getContext().get(MessageContext.CONTEXT_PREVIOUS_PIPE));
	}
}
