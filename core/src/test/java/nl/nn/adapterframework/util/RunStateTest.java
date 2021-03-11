package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RunStateTest {

	@Test
	public void startedEqualsSTARTED() throws Exception {
		assertTrue(RunStateEnum.STARTED.isState("Started"));
	}

	@Test
	public void compareTo() throws Exception {
		assertEquals(0, RunStateEnum.ERROR.compareTo(RunStateEnum.ERROR));
		assertTrue(RunStateEnum.ERROR.compareTo(RunStateEnum.STARTED) < 0);
		assertTrue(RunStateEnum.ERROR.compareTo(RunStateEnum.STOPPED) < 0);
		assertTrue(RunStateEnum.STOPPED.compareTo(RunStateEnum.STARTED) > 0);
	}

	@Test
	public void checkGetName() throws Exception {
		assertEquals("Started", RunStateEnum.STARTED.getName());
		assertEquals("Starting", RunStateEnum.STARTING.getName());
		assertEquals("Stopped", RunStateEnum.STOPPED.getName());
		assertEquals("Stopping", RunStateEnum.STOPPING.getName());
		assertEquals("**ERROR**", RunStateEnum.ERROR.getName());
	}
}
