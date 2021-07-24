package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;

public class JsonWellFormedCheckerTest extends PipeTestBase<JsonWellFormedChecker>{

	@Override
	public JsonWellFormedChecker createPipe() {
		return new JsonWellFormedChecker();
	}

	@Test
	public void doFailureEmptyInputTest() throws Exception {
		pipe.registerForward(createPipeFailureForward());
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "", session);
		assertEquals("failure", pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void doSuccessTest() throws Exception {
		pipe.registerForward(createPipeFailureForward());
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "{\"input\":\"ok\"}", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void doFailureNonWellFormedInputTest() throws Exception {
		pipe.registerForward(createPipeFailureForward());
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "input=ok", session);
		assertEquals("failure", pipeRunResult.getPipeForward().getName());
	}

	private PipeForward createPipeFailureForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("failure");
		return pipeForward;
	}

}
