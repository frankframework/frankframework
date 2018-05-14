package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;

public class JsonWellFormedCheckerTest {

	private IPipeLineSession session = new PipeLineSessionBase();

	@Test
	public void doFailureEmptyInputTest() throws Exception {
		JsonWellFormedChecker jsonWellFormedChecker = new JsonWellFormedChecker();
		jsonWellFormedChecker.registerForward(createPipeSuccessForward());
		jsonWellFormedChecker.registerForward(createPipeFailureForward());
		jsonWellFormedChecker.configure();
		PipeRunResult pipeRunResult = jsonWellFormedChecker.doPipe("", session);
		assertEquals("failure", pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void doSuccessTest() throws Exception {
		JsonWellFormedChecker jsonWellFormedChecker = new JsonWellFormedChecker();
		jsonWellFormedChecker.registerForward(createPipeSuccessForward());
		jsonWellFormedChecker.registerForward(createPipeFailureForward());
		jsonWellFormedChecker.configure();
		PipeRunResult pipeRunResult = jsonWellFormedChecker
				.doPipe("{\"input\":\"ok\"}", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void doFailureNonWellFormedInputTest() throws Exception {
		JsonWellFormedChecker jsonWellFormedChecker = new JsonWellFormedChecker();
		jsonWellFormedChecker.registerForward(createPipeSuccessForward());
		jsonWellFormedChecker.registerForward(createPipeFailureForward());
		jsonWellFormedChecker.configure();
		PipeRunResult pipeRunResult = jsonWellFormedChecker.doPipe("input=ok",
				session);
		assertEquals("failure", pipeRunResult.getPipeForward().getName());
	}

	private PipeForward createPipeSuccessForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("success");
		return pipeForward;
	}

	private PipeForward createPipeFailureForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("failure");
		return pipeForward;
	}
}
