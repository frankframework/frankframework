package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CompareIntegerPipeTest extends PipeTestBase<CompareIntegerPipe> {
	@Override
	public CompareIntegerPipe createPipe() {
		return new CompareIntegerPipe();
	}

	@Test
	void noSessionKey() throws ConfigurationException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));

		assertThrows(ConfigurationException.class, pipe::configure, "has neither parameter [operand1] nor parameter [operand2] specified");
	}

	@Test
	void happyFlowLessThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND1, "4"));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND2, "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		Assertions.assertEquals("lessthan", prr.getPipeForward().getName());
	}

	@Test
	void happyFlowGreaterThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND1, "5"));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND2, "4"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		Assertions.assertEquals("greaterthan", prr.getPipeForward().getName());
	}

	@Test
	void happyFlowEqualsFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND1, "5"));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND2, "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		Assertions.assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	void happyFlowEqualsOperand1InputMessage() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND2, "5"));
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "5", session);
		Assertions.assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	void numberFormatExceptionFromMessageOperand() throws ConfigurationException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.addParameter(new Parameter(CompareIntegerPipe.OPERAND1, "5"));
		pipe.configure();

		PipeRunException exception = assertThrows(PipeRunException.class, () -> doPipe(pipe, "non-numeric", session));
		assertTrue(exception.getMessage().contains("Exception on getting [operand2] from input"));
	}

}
