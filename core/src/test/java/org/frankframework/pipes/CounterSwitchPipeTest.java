package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;

public class CounterSwitchPipeTest extends PipeTestBase<CounterSwitchPipe> {

	@Override
	public CounterSwitchPipe createPipe() throws ConfigurationException {
		CounterSwitchPipe pipe = new CounterSwitchPipe();
		pipe.addForward(new PipeForward("1", null));
		pipe.addForward(new PipeForward("2", null));
		return pipe;
	}

	@Test
	public void getterSetterDivisor() {
		int dummyDivisor = 1337;
		pipe.setDivisor(1337);
		int otherDivisor = pipe.getDivisor();
		assertEquals(dummyDivisor, otherDivisor);
	}

	@Test
	public void testDivisorLessThanTwo() throws Exception {
		pipe.setDivisor(1);
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertEquals("Exception configuring CounterSwitchPipe [CounterSwitchPipe under test]: divisor [1] should be greater than or equal to 2", ex.getMessage());
	}

	@Test
	public void testLegitimateDivisor() throws Exception {
		configureAndStartPipe();
		PipeRunResult result = doPipe(pipe, "dummy", session);
		assertEquals("dummy", result.getResult().asString());
		assertEquals("2", result.getPipeForward().getName());
	}

	@Test
	public void testNonExistingForward() throws Exception {
		pipe.setDivisor(3);
		ConfigurationException ex = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertEquals("Exception configuring CounterSwitchPipe [CounterSwitchPipe under test]: forward [3] is not defined", ex.getMessage());
	}
}
