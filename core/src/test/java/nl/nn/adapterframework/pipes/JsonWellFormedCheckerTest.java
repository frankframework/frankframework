package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;

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
		initJsonWellFormedCheckerTest(input, forward);
		pipe.registerForward( new PipeForward("failure", "path") );
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		assertEquals(forward, pipeRunResult.getPipeForward().getName());
	}

	public void initJsonWellFormedCheckerTest(String input, String forward) {
		this.input = input;
		this.forward = forward;
	}
}
