package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class CompareIntegerPipeTest extends PipeTestBase<CompareIntegerPipe> {

	private IPipeLineSession session = new PipeLineSessionBase();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Override
	public CompareIntegerPipe createPipe() {
		return new CompareIntegerPipe();
	}

	@Test
	public void sessionKeyDoesNotExist() throws PipeRunException {
		thrown.expectMessage("Exception on getting [operand1] from session key [1]");
		pipe.setSessionKey1("1");
		doPipe(pipe,"input", session);
	}

	@Test
	public void noSessionKey() throws ConfigurationException {
		thrown.expectMessage("has neither parameter [operand1] nor parameter [operand2] specified");
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		pipe.configure();
	}

	@Test
	public void happyFlowLessThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		Parameter op1 = new Parameter();
		op1.setName("operand1");
		op1.setValue("4");
		Parameter op2 = new Parameter();
		op2.setName("operand2");
		op2.setValue("5");
		pipe.addParameter(op1);
		pipe.addParameter(op2);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("lessthan", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowGreaterThanFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		Parameter op1 = new Parameter();
		op1.setName("operand1");
		op1.setValue("5");
		Parameter op2 = new Parameter();
		op2.setName("operand2");
		op2.setValue("4");
		pipe.addParameter(op1);
		pipe.addParameter(op2);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("greaterthan", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowEqualsFromParameters() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		Parameter op1 = new Parameter();
		op1.setName("operand1");
		op1.setValue("5");
		Parameter op2 = new Parameter();
		op2.setName("operand2");
		op2.setValue("5");
		pipe.addParameter(op1);
		pipe.addParameter(op2);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	public void happyFlowEqualsOperand1InputMessage() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		Parameter op2 = new Parameter();
		op2.setName("operand2");
		op2.setValue("5");
		pipe.addParameter(op2);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "5", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	public void numberFormatExceptionFromMessageOperand() throws ConfigurationException, PipeRunException {
		thrown.expectMessage("Exception on getting [operand2] from input");
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		Parameter op1 = new Parameter();
		op1.setName("operand1");
		op1.setValue("5");
		pipe.addParameter(op1);
		pipe.configure();

		doPipe(pipe, "non-numeric", session);
	}

	@Test
	public void happyFlowEqualsOperand1SessionKey() throws ConfigurationException, PipeRunException {
		pipe.registerForward(new PipeForward("lessthan", null));
		pipe.registerForward(new PipeForward("greaterthan", null));
		pipe.registerForward(new PipeForward("equals", null));
		String sessionKey1 = "sessionKey1";
		session.put(sessionKey1, 5);

		Parameter op2 = new Parameter();
		op2.setName("operand2");
		op2.setValue("5");

		pipe.addParameter(op2);
		pipe.setSessionKey1(sessionKey1);
		pipe.configure();

		PipeRunResult prr = doPipe(pipe, "", session);
		assertEquals("equals", prr.getPipeForward().getName());
	}
}