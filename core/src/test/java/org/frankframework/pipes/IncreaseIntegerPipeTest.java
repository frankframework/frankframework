package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.ParameterBuilder;

/**
 * IncreaseIntegerPipe Tester.
 *
 * @author <Sina Sen>
 */
public class IncreaseIntegerPipeTest extends PipeTestBase<IncreaseIntegerPipe> {

	@Override
	public IncreaseIntegerPipe createPipe() {
		return new IncreaseIntegerPipe();
	}

	@Test
	public void testIncreaseBy2() throws Exception {
		session.put("a", "4");
		pipe.setSessionKey("a");
		pipe.setIncrement(2);
		pipe.configure();
		doPipe(pipe, "doesnt matter", session);
		assertEquals("6", session.get("a"));
	}

	@Test
	public void testCannotIncreaseBy2AsNoSessionKey() {
		session.put("a", "4");
		pipe.setIncrement(2);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("sessionKey must be filled"));
	}

	@Test
	public void testIncrementParameter() throws Exception {
		String numberSession = "number";
		session.put(numberSession, "4");
		pipe.addParameter(new Parameter("increment", "5"));
		pipe.setSessionKey(numberSession);
		pipe.configure();
		doPipe(pipe, "message", session);
		assertEquals("9", session.get(numberSession));
	}

	@Test
	public void testNullIncrementParameter() throws Exception {
		String numberSession = "number";
		session.put(numberSession, "4");
		pipe.addParameter(ParameterBuilder.create().withName("increment"));
		pipe.setSessionKey(numberSession);
		pipe.configure();
		doPipe(pipe, null, session);
		assertEquals("5", session.get(numberSession));
	}

	@Test
	public void testEmptyIncrementParameter() throws Exception {
		Exception exception = assertThrows(NumberFormatException.class, () -> {
			String numberSession = "number";
			session.put(numberSession, "4");
			pipe.addParameter(new Parameter("increment", ""));
			pipe.setSessionKey(numberSession);
			pipe.configure();
			doPipe(pipe, "", session);
		});
		String expectedMessage = "For input string";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage));
	}

}
