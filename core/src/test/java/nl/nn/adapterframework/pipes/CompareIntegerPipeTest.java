package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class CompareIntegerPipeTest extends PipeTestBase<CompareIntegerPipe> {

	@Override
	public CompareIntegerPipe createPipe() {
		return new CompareIntegerPipe();
	}

	@Test
	public void sessionKeyDoesNotExist() {
		pipe.setSessionKey1("1");
		assertThrows(PipeRunException.class, () -> {
			doPipe(pipe,"input", session);
		}, "Exception on getting [operand1] from session key [1]");
	}

	@Test
	public void noSessionKey() throws ConfigurationException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));

		assertThrows(ConfigurationException.class, () -> {
			pipe.configure();
		}, "has neither parameter [operand1] nor parameter [operand2] specified");
	}

	@Test
	public void happyFlowLessThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter("operand1", "4"));
		pipe.addParameter(new Parameter("operand2", "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("lessthan", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowGreaterThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter("operand1", "5"));
		pipe.addParameter(new Parameter("operand2", "4"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("greaterthan", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowEqualsFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter("operand1", "5"));
		pipe.addParameter(new Parameter("operand2", "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowEqualsOperand1InputMessage() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter("operand2", "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "5", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	public void numberFormatExceptionFromMessageOperand() throws ConfigurationException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter("operand1", "5"));
		pipe.configure();

		assertThrows(PipeRunException.class, () -> {
			doPipe(pipe, "non-numeric", session);
		}, "Exception on getting [operand1] from session key [1]");
	}

	@Test
	public void happyFlowEqualsOperand1SessionKey() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		String sessionKey1 = "sessionKey1";
		session.put(sessionKey1, 5);

		pipe.addParameter(new Parameter("operand2", "5"));
		pipe.setSessionKey1(sessionKey1);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}
}
