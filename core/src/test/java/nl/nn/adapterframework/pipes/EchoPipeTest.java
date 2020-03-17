package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class EchoPipeTest extends PipeTestBase<EchoPipe> {

	private IPipeLineSession session = new PipeLineSessionBase();

	@Override
	public EchoPipe createPipe() {
		return new EchoPipe();
	}

	@Test
	public void testDoPipe() throws PipeRunException {
		String dummyInput = "dummyInput";
		PipeRunResult prr = doPipe(pipe, dummyInput, session);

		String result = null;
		try {
			result = new Message(prr.getResult()).asString();
		} catch (IOException e) {
			fail("cannot open stream: " + e.getMessage());
		}
		assertEquals(dummyInput, result);
	}

}