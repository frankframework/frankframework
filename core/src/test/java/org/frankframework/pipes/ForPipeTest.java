package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;

public class ForPipeTest extends PipeTestBase<ForPipe> {

	@Override
	public ForPipe createPipe() {
		return new ForPipe();
	}

	@Test
	void assertMaxNotSet() {
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("is mandatory to break out of the for loop pipe"));
	}

	@Test
	void assertForwardsSet() throws ConfigurationException, PipeStartException {
		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));
		pipe.setMax(10);
		configureAndStartPipe();

		assertDoesNotThrow(this::configurePipe);
	}

	@Test
	void testStop() throws PipeRunException, IOException, ConfigurationException, PipeStartException {
		String dummyInput = "dummyInput";

		pipe.setStartAt(10);
		pipe.setMax(10);
		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));
		configureAndStartPipe();

		// Assert that we start at 0
		assertEquals(10, pipe.getStartAt());

		PipeRunResult prr = doPipe(pipe, dummyInput, session);
		String result = prr.getResult().asString();

		// Assert that the stop forward was returned
		assertEquals(ForPipe.STOP_FORWARD_NAME, prr.getPipeForward().getName());

		// Assert that the session key is not present
		assertNull(session.get(pipe.getIncrementSessionKey()));

		assertEquals(dummyInput, result);
	}

	@Test
	void testIncrement() throws PipeRunException, IOException, ConfigurationException, PipeStartException {
		String dummyInput = "dummyInput";

		pipe.setMax(10);
		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));
		configureAndStartPipe();

		// Assert that we start at 10
		assertEquals(pipe.getStartAt(), 0);

		PipeRunResult prr = doPipe(pipe, dummyInput, session);
		String result = prr.getResult().asString();

		// Assert that the session key is now 0 + 1
		assertEquals(1, session.get(pipe.getIncrementSessionKey()));

		// Assert that the stop forward was returned
		assertEquals(ForPipe.CONTINUE_FORWARD_NAME, prr.getPipeForward().getName());

		assertEquals(dummyInput, result);
	}
}
