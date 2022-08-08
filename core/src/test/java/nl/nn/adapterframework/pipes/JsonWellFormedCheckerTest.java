package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;

@RunWith(Parameterized.class)
public class JsonWellFormedCheckerTest extends PipeTestBase<JsonWellFormedChecker> {

	@Parameter(0)
	public String input = null;
	@Parameter(1)
	public String forward = null;

	@Parameters(name = "{index}: input [ {0} ] forward [{1}]")
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

	@Test
	public void runTest() throws Exception {
		pipe.registerForward( new PipeForward("failure", "path") );
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		assertEquals(forward, pipeRunResult.getPipeForward().getName());
	}
}
