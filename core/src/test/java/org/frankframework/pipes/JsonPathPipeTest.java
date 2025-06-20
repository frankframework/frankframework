package org.frankframework.pipes;

import static org.frankframework.testutil.TestAssertions.assertJsonEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

class JsonPathPipeTest {

	private JsonPathPipe pipe;
	private PipeLineSession session;
	private PipeRunResult result;
	private Message input;

	@BeforeEach
	void setUp() {
		pipe = new JsonPathPipe();
		pipe.addForward(new PipeForward("success", "success"));
		session = new PipeLineSession();
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session, result, input);
	}

	static Stream<Arguments> doPipe() {
		// The arguments are:
		// - JSON Path Expression
		// - Expected output
		// - Input message for thest
		return Stream.of(
				arguments(
						"$.a", "Hello World",
						"""
								{"a": "Hello World"}
								"""
				),
				arguments(
						"$.a", "123",
						"""
								{"a": 123}
								"""
				),
				arguments(
						"$.*.a", "[1, 2]",
						"""
								{
								"k1": {"a": 1},
								"k2": {"a": 2}
								}
								"""
				),
				arguments(
						"sum($.*.a)", "3.0",
						"""
								{
								"k1": {"a": 1},
								"k2": {"a": 2}
								}
								"""
				),
				arguments(
						"$..[?(@.a)].length()", "2",
						"""
								{
								"k1": {"a": 1},
								"k2": {"a": 2},
								"k3": {"b": 3}
								}
								"""
				),
				arguments(
						"concat(\"result count=\", $..[?(@.a)].length())", "result count=2",
						"""
								{
								"k1": {"a": 2},
								"k2": {"a": 4},
								"k3": {"b": 3}
								}
								"""
				),
				arguments("$.a", """
						{"Hello": "World"}
						""",
						"""
								{
								  "a": {"Hello": "World"},
								}
								""")
		);
	}

	@ParameterizedTest
	@MethodSource
	void doPipe(String jsonPath, String expected, Object messageData) throws Exception {
		// Arrange
		pipe.setJsonPathExpression(jsonPath);
		pipe.configure();

		input = Message.asMessage(messageData);

		// Act
		result = pipe.doPipe(input, session);

		// Assert
		assertNotNull(result);
		assertNotNull(result.getResult());

		String resultString = result.getResult().asString();
		assertNotNull(resultString);
		if (resultString.startsWith("[") || resultString.startsWith("{")) {
			assertJsonEquals(expected, resultString);
		} else {
			assertEquals(expected, resultString);
		}
	}

	@Test
	void testConfigureFailsNoExpressionSet() {

		ConfigurationException e = assertThrows(ConfigurationException.class, pipe::configure);

		assertEquals("jsonPathExpression has to be set", e.getMessage());
	}

	@Test
	void testConfigureFailsInvalidExpressionSet() {
		pipe.setJsonPathExpression("$[invalid");
		ConfigurationException e = assertThrows(ConfigurationException.class, pipe::configure);

		assertThat(e.getMessage(), containsString("Invalid JSON Path expression: [$[invalid]"));
	}

	@Test
	void testPathEvaluationNoResult() throws ConfigurationException {
		// Arrange
		pipe.setJsonPathExpression("$.a");
		pipe.configure();

		input = Message.asMessage("{k: \"Hello World\"}");

		// Act
		PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> pipe.doPipe(input, session));

		// Assert
		assertEquals("No results for path: $['a']", pipeRunException.getCause().getMessage());
	}
}
