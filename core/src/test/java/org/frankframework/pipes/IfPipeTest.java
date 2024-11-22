package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

/**
 * Contains generic {@link IfPipe} scenarios to test
 */
public class IfPipeTest extends PipeTestBase<IfPipe> {
	public static final String PIPE_FORWARD_THEN = "then";
	public static final String PIPE_FORWARD_ELSE = "else";
	private static final String TEST_INPUT = "<test />";
	private static final String TEST_JSON_INPUT = "{test: ''}";

	private PipeRunResult pipeRunResult;

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(pipeRunResult);

		super.tearDown();
	}

	@Override
	public IfPipe createPipe() throws ConfigurationException {
		IfPipe ifPipe = new IfPipe();

		// Add default forwards
		ifPipe.addForward(new PipeForward(PIPE_FORWARD_THEN, null));
		ifPipe.addForward(new PipeForward(PIPE_FORWARD_ELSE, null));

		return ifPipe;
	}

	/**
	 * Provides a MethodSource for a Json Message (with a specific mime type) and a XML message (without mime type)
	 */
	public static Stream<Arguments> messageSource() {
		return Stream.of(
				Arguments.of(getJsonMessage(), true),
				Arguments.of(new Message(TEST_INPUT), false)
		);
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void emptyExpressionTest(Message message) throws Exception {
		// both xpath and json expression are null
		pipe.configure();
		pipe.start();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void expressionValueWithoutExpressionTest(Message message) throws Exception {
		pipe.setExpressionValue("test");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void invalidXPathExpressionTest(Message message, boolean isJson) throws Exception {
		if (isJson) {
			pipe.setJsonPathExpression("someexpression");
		} else {
			pipe.setXpathExpression("someexpression");
		}
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void emptyRegexTest(Message message) throws Exception {
		pipe.setRegex("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void someRegexTextTest(Message message) throws Exception {
		pipe.setRegex("some");
		pipe.configure();
		pipe.start();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testInputRegexTest() throws Exception {
		pipe.setRegex("(hoi)+");
		pipe.configure();
		pipe.start();

		String input = "hoihoihoi"; // Note that 'hoi a hoi' input is not a match!

		// Act & Assert 1: Test with matching regex
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());

		// do the same for mediatype json
		pipeRunResult = doPipe(pipe, getJsonMessage(input), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());


		// Act & Assert 2: Test with non-matching regex
		pipe.setRegex("(test3)+");
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());

		// do the same for mediatype json
		pipeRunResult = doPipe(pipe, getJsonMessage(input), session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@Test
	@Disabled("Current regex implementation does not support multiline input. See #6963")
	void realWorldMultilineInputRegexTest() throws Exception {
		pipe.setRegex("(test1)+");
		pipe.configure();
		pipe.start();

		String input = """
				<directory>
					<file name="test1.txt"/>
					<file name="test2.txt"/>
				</directory>""";

		// Act & Assert 1: Test with matching regex
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());

		// Act & Assert 2: Test with non-matching regex
		pipe.setRegex("(test3)+");
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void emptyXPathExpressionTest(Message message, boolean isJson) throws Exception {
		if (isJson) {
			pipe.setJsonPathExpression("");
		} else {
			pipe.setXpathExpression("");
		}
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void emptyXPathExpressionWithEmptyExpressionValueTest(Message message, boolean isJson) throws Exception {
		if (isJson) {
			pipe.setJsonPathExpression("");
		} else {
			pipe.setXpathExpression("");
		}

		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void inputMatchesWithRegexTest() throws Exception {
		pipe.setRegex("test123");
		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());

		pipeRunResult = doPipe(pipe, getJsonMessage("test123"), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void inputMatchesExpressionValueTest() throws Exception {
		pipe.setExpressionValue("test123");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());

		pipeRunResult = doPipe(pipe, getJsonMessage("test123"), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void testWithCustomThenForward(Message message) throws Exception {
		String forwardName = "someText";
		pipe.setThenForwardName(forwardName);
		pipe.addForward(new PipeForward(forwardName, null));
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(forwardName, pipeRunResult.getPipeForward().getName());
	}

	@MethodSource("messageSource")
	@ParameterizedTest
	void testWithCustomElseForward(Message message) throws Exception {
		String forwardName = "someText";
		pipe.setElseForwardName(forwardName);
		pipe.addForward(new PipeForward(forwardName, null));
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	public static Stream<Arguments> spacePrefixedMessageSource() {
		return Stream.of(
				Arguments.of(getJsonMessage(" " + TEST_JSON_INPUT), true),
				Arguments.of(getJsonMessage("	" + TEST_JSON_INPUT), true),
				Arguments.of(new Message(" " + TEST_INPUT), false),
				Arguments.of(new Message("	" + TEST_INPUT), false)
		);
	}

	@ParameterizedTest
	@MethodSource("spacePrefixedMessageSource")
	void whitespaceOnValidThenPipeTest(Message message) throws Exception {
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testNullInput() throws Exception {
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, new Message((String) null), session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());

		pipeRunResult = doPipe(pipe, getJsonMessage(null), session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@ParameterizedTest
	@MethodSource("messageSource")
	void testInvalidPathExpressionForMediaType(Message message, boolean isJson) throws Exception {
		// Make sure to use this the wrong way around to test the error scenario
		if (isJson) {
			pipe.setXpathExpression("");
		} else {
			pipe.setJsonPathExpression("");
		}

		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, message, session));
		assertThat(e.getMessage(), Matchers.containsString("Incorrect pathExpression provided for given mediaType"));
	}

	@ParameterizedTest
	@MethodSource("messageSource")
	void testWrongExpressionInput(Message message, boolean isJson) throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setJsonPathExpression("$.root");

		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, message, session));
		String expectedMessage = "Incorrect pathExpression provided for given mediaType ";
		expectedMessage += (isJson) ? "application/json" : "application/xml";

		assertThat(e.getMessage(), Matchers.containsString(expectedMessage));
	}

	private static Message getJsonMessage() {
		return getJsonMessage(TEST_JSON_INPUT);
	}

	static Message getJsonMessage(String json) {
		Message jsonMessage = new Message(json);
		jsonMessage.getContext().withMimeType("application/json");
		return jsonMessage;
	}
}
