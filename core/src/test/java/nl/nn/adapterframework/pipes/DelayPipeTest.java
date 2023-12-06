package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		String result = Message.asString(prr.getResult());
		assertEquals(input, result);
	}
}
