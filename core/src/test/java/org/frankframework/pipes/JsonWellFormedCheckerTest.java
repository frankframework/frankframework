package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

public class JsonWellFormedCheckerTest extends PipeTestBase<JsonWellFormedChecker> {
	public String input = null;
	public String forward = null;

	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "", "failure" },
			{ "{\"input\":\"ok\"}", "success" },
			{ "input=ok", "failure" },
			{ "[]", "success" },
			{ "[\"input\":{}]", "failure" },
			{ "[\"input\"]", "success" },
			{ "[\"input\", \"text\"]", "success" },
			{ "[{}]", "success" },
		});
	}

	@Override
	public JsonWellFormedChecker createPipe() {
		return new JsonWellFormedChecker();
	}

	@MethodSource("data")
	@ParameterizedTest(name = "{index}: input [ {0} ] forward [{1}]")
	public void runTest(String input, String forward) throws Exception {
		initJsonWellFormedCheckerTestData(input, forward);
		pipe.addForward( new PipeForward("failure", "path") );
		pipe.configure();
		pipe.start();

		try (Message inputMessage = new Message(input)) {
			PipeRunResult pipeRunResult = doPipe(pipe, inputMessage, session);
			assertEquals(forward, pipeRunResult.getPipeForward().getName());
			assertFalse(inputMessage.isClosed(), "pipe may close the reader but not the input (with is the output) message");
		}
	}

	public void initJsonWellFormedCheckerTestData(String input, String forward) {
		this.input = input;
		this.forward = forward;
	}
}
