package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

public class DelayPipeTest extends PipeTestBase<DelayPipe> {

	@Override
	public DelayPipe createPipe() {
		return new DelayPipe();
	}

	@Test
	public void getterSetterDelayTime() {
		long dummyTime = 1337;
		pipe.setDelayTime(dummyTime);
		assertEquals(pipe.getDelayTime(), dummyTime);
	}

	@Test
	public void testUnInterruptedSession() throws Exception {
		Object input = "dummyInput";
		pipe.setDelayTime(1000);
		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertEquals(input, result);
	}
}
