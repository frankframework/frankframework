package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.NumberParameterBuilder;

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
		pipe.setStopAt(10);
		configureAndStartPipe();

		assertDoesNotThrow(this::configurePipe);
	}

	@Test
	void assertFailOnDoubleStopAt() throws ConfigurationException, PipeStartException {
		pipe.addParameter(NumberParameterBuilder.create().withName(ForPipe.STOP_AT_PARAMETER_VALUE).withValue(10));
		pipe.setStopAt(10);

		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));
		configureAndStartPipe();

		List<String> warnings = getConfigurationWarnings().getWarnings();
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("both as attribute and Parameter"));
	}

	@Test
	void testStop() throws PipeRunException, IOException, ConfigurationException, PipeStartException {
		String dummyInput = "dummyInput";

		pipe.setStartAt(10);
		pipe.setStopAt(10);
		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));
		configureAndStartPipe();

		// Assert that we start at 10
		assertEquals(10, pipe.getStartAt());

		PipeRunResult prr = doPipe(pipe, dummyInput, session);
		String result = prr.getResult().asString();

		// Assert that the stop forward was returned
		assertEquals(ForPipe.STOP_FORWARD_NAME, prr.getPipeForward().getName());

		// Assert that the session key is not present
		assertNull(session.get(pipe.getIncrementSessionKey()));

		assertEquals(dummyInput, result);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void testIncrement(boolean useParameter) throws PipeRunException, IOException, ConfigurationException, PipeStartException {
		String dummyInput = "dummyInput";

		if (useParameter) {
			pipe.addParameter(NumberParameterBuilder.create().withName(ForPipe.STOP_AT_PARAMETER_VALUE).withValue(10));
		} else {
			pipe.setStopAt(10);
		}

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

	@Test
	void testFailOnEmptyParameter() throws Exception {
		String dummyInput = "dummyInput";

		pipe.addParameter(new Parameter(ForPipe.STOP_AT_PARAMETER_VALUE, ""));
		pipe.registerForward(new PipeForward("continue", null));
		pipe.registerForward(new PipeForward("stop", null));

		configureAndStartPipe();

		assertThrows(PipeRunException.class, () -> doPipe(pipe, dummyInput, session));
	}
}
