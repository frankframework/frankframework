package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class EchoPipeTest extends PipeTestBase<EchoPipe> {

	@Override
	public EchoPipe createPipe() {
		return new EchoPipe();
	}

	@Test
	public void testDoPipe() throws PipeRunException, IOException {
		String dummyInput = "dummyInput";

		PipeRunResult prr = doPipe(pipe, dummyInput, session);
		String result = Message.asString(prr.getResult());

		assertEquals(dummyInput, result);
	}

}
