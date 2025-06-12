package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
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

	@Test
	void doPipe() throws Exception {
		// Arrange
		pipe.setJsonPathExpression("$.a");
		pipe.configure();

		input = new Message("""
		{"a": "Hello World"}
		"""
		);

		// Act
		result = pipe.doPipe(input, session);

		// Arrange
		assertNotNull(result);
		assertNotNull(result.getResult());

		String resultString = result.getResult().asString();
		assertEquals("Hello World", resultString);
	}
}
