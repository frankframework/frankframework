package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
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
	public void testUnInterruptedSession() throws PipeRunException, ConfigurationException, PipeStartException {
		Object input = "dummyInput";
		pipe.setDelayTime(1000);
		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = null;
		try {
			result = new Message(prr.getResult()).asString();
		} catch (IOException e) {
			fail("cannot open stream: " + e.getMessage());
		}
		assertEquals(input, result);
	}
}