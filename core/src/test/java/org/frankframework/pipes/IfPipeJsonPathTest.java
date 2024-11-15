package org.frankframework.pipes;

import static org.frankframework.pipes.IfPipeTest.PIPE_FORWARD_ELSE;
import static org.frankframework.pipes.IfPipeTest.PIPE_FORWARD_THEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.util.CloseUtils;

public class IfPipeJsonPathTest extends PipeTestBase<IfPipe> {

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

	static String testJson = """
			{
			    "store": {
			        "book": [
			            {
			                "category": "reference",
			                "author": "Nigel Rees",
			                "title": "Sayings of the Century",
			                "price": 8.95
			            },
			            {
			                "category": "fiction",
			                "author": "Evelyn Waugh",
			                "title": "Sword of Honour",
			                "price": 12.99
			            },
			            {
			                "category": "fiction",
			                "author": "Herman Melville",
			                "title": "Moby Dick",
			                "isbn": "0-553-21311-3",
			                "price": 8.99
			            },
			            {
			                "category": "fiction",
			                "author": "J. R. R. Tolkien",
			                "title": "The Lord of the Rings",
			                "isbn": "0-395-19395-8",
			                "price": 22.99
			            }
			        ]
			    }
			}
			""";

	public static Stream<Arguments> messageSource() {
		return Stream.of(
				// input, expression, expressionValue, expectedValue
				Arguments.of("{root: ''}", "$.root", "", PIPE_FORWARD_THEN),
				Arguments.of("{root: 'test'}", "$.root", "", PIPE_FORWARD_THEN),
				Arguments.of("{root: ''}", "$.root", "test", PIPE_FORWARD_ELSE),
				Arguments.of("{root: 'test'}", "$.root", "test", PIPE_FORWARD_THEN),
				Arguments.of("{root: 'test123'}", "$.root", "test", PIPE_FORWARD_ELSE),
				Arguments.of(testJson, "$.store.book[1].author", "", PIPE_FORWARD_THEN),
				Arguments.of(testJson, "$.store.book[1].author", "Evelyn Waugh", PIPE_FORWARD_THEN),
				Arguments.of(testJson, "$.store.book[1].isbn", "", PIPE_FORWARD_ELSE),
				Arguments.of(testJson, "$.store.book[1].category", "reference", PIPE_FORWARD_ELSE),
				Arguments.of(testJson, "$.store.book[?(@.price == 22.99)].author", "J. R. R. Tolkien", PIPE_FORWARD_THEN),
				Arguments.of(testJson, "$.store.book[?(@.category == 'fiction')]", "", PIPE_FORWARD_THEN)
		);
	}

	@ParameterizedTest
	@MethodSource("messageSource")
	void testExpressions(String input, String expression, String expressionValue, String expectedValue) throws Exception {
		pipe.setJsonPathExpression(expression);
		pipe.setExpressionValue(expressionValue);
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, IfPipeTest.getJsonMessage(input), session);
		assertEquals(expectedValue, pipeRunResult.getPipeForward().getName());
	}
}
