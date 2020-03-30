package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class DelayPipeTest extends PipeTestBase<DelayPipe> {

	private IPipeLineSession session = new PipeLineSessionBase();

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