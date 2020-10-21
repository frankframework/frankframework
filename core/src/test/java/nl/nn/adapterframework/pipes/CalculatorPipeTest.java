package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class CalculatorPipeTest extends PipeTestBase<CalculatorPipe> {

	@Override
	public CalculatorPipe createPipe() {
		return new CalculatorPipe();
	}

	@Test
	public void validPipeTest() throws PipeRunException, IOException {
		String input = "1 + 1";
		String expected = "1 + 1 = 2";

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertEquals(expected, result);
	}
	
	@Test(expected = PipeRunException.class)
	public void invalidPipeTest() throws PipeRunException, IOException {
		String input = "A";
		doPipe(pipe, input, session);
	}
}
