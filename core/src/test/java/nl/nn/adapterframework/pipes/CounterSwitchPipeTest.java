package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;

public class CounterSwitchPipeTest extends PipeTestBase<CounterSwitchPipe> {

	@Override
	public CounterSwitchPipe createPipe() throws ConfigurationException {
		CounterSwitchPipe pipe = new CounterSwitchPipe();
		pipe.registerForward(new PipeForward("1", null));
		pipe.registerForward(new PipeForward("2", null));
		pipe.registerForward(new PipeForward("", null));
		return pipe;
	}

	@Test
	public void getterSetterDivisor() {
		int dummyDivisor = 1337;
		pipe.setDivisor(1337);
		int otherDivisor = pipe.getDivisor();
		assertEquals(dummyDivisor, otherDivisor);
	}

	@Test(expected = ConfigurationException.class)
	public void testDivisorLessThanTwo() throws Exception {
		pipe.setDivisor(1);
		configureAndStartPipe();

	}

	@Test
	public void testLegitimateDivisor() throws Exception {
		configureAndStartPipe();
		PipeRunResult result = doPipe(pipe, "dummy", session);
		assertEquals("dummy", result.getResult().asString());
		assertEquals("2", result.getPipeForward().getName());
	}

	@Test(expected = ConfigurationException.class)
	public void testNonExistingForward() throws Exception {
		pipe.setDivisor(3);
		configureAndStartPipe();
	}
}