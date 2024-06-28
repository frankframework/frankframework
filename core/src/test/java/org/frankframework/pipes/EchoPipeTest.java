package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

public class EchoPipeTest extends PipeTestBase<EchoPipe> {

	@Override
	public EchoPipe createPipe() {
		return new EchoPipe();
	}

	@Test
	public void testDoPipe() throws PipeRunException, IOException {
		String dummyInput = "dummyInput";

		PipeRunResult prr = doPipe(pipe, dummyInput, session);
		String result = prr.getResult().asString();

		assertEquals(dummyInput, result);
	}
}
