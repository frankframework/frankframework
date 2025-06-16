package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
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
	void inputMatchesExpressionValueTest() throws Exception {
		pipe.setExpressionValue("test123");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
		assertEquals("test123", pipeRunResult.getResult().asString());

		pipeRunResult = doPipe(pipe, getJsonMessage("test123"), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
		assertEquals("test123", pipeRunResult.getResult().asString());

		pipeRunResult = doPipe(pipe, new Message(new StringReader("test123")), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
		assertEquals("test123", pipeRunResult.getResult().asString());

		pipeRunResult = doPipe(pipe, getStreamingJsonMessage("test123"), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
		assertEquals("test123", pipeRunResult.getResult().asString());
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

	@Test
	void testInvalidJsonPathExpression() throws Exception {
		pipe.setJsonPathExpression("$[invalid]");
		configureAndStartPipe();

		PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> doPipe(pipe, getJsonMessage("invalid"), session));

		assertInstanceOf(com.jayway.jsonpath.InvalidPathException.class, pipeRunException.getCause());
		assertThat(pipeRunException.getMessage(), containsString("error evaluating expression"));
	}

	@Test
	void testInvalidJsonMessage() throws Exception {
		pipe.setJsonPathExpression("$.invalid");
		configureAndStartPipe();

		PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> doPipe(pipe, getJsonMessage("{invalid"), session));

		assertInstanceOf(com.jayway.jsonpath.InvalidJsonException.class, pipeRunException.getCause());
		assertThat(pipeRunException.getMessage(), containsString("error evaluating expression"));
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

	static Message getStreamingJsonMessage(String json) {
		Message jsonMessage = new Message(new StringReader(json));
		jsonMessage.getContext().withMimeType("application/json");
		return jsonMessage;
	}
}
