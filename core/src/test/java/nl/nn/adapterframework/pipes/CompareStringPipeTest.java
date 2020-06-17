package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class CompareStringPipeTest extends PipeTestBase<CompareStringPipe> {

	String key1 = "key1";
	String key2 = "key2";

	@Override
	public CompareStringPipe createPipe() {
		return new CompareStringPipe();
	}

	@Test(expected = ConfigurationException.class)
	public void emptySessionKeys() throws ConfigurationException {
		pipe.setSessionKey1("");
		pipe.setSessionKey2("");
		pipe.configure();
	}

	@Test
	public void setSessionKey1() {
		String dummyKey = "kappa123";
		pipe.setSessionKey1(dummyKey);
		String retrievedKey = pipe.getSessionKey1();
		assertEquals(dummyKey, retrievedKey);
	}

	@Test
	public void setSessionKey2() {
		String dummyKey = "Kappa123";
		pipe.setSessionKey2(dummyKey);
		String retrievedKey = pipe.getSessionKey2();
		assertEquals(dummyKey, retrievedKey);
	}
	
	@Test
	public void testLessThan() throws Exception {
		pipe.registerForward(new PipeForward("lessthan",null));
		pipe.registerForward(new PipeForward("greaterthan",null));
		pipe.registerForward(new PipeForward("equals",null));
		Parameter param1 = new Parameter();
		param1.setName("operand1");
		param1.setValue("a");
		Parameter param2 = new Parameter();
		param2.setName("operand2");
		param2.setValue("b");
		pipe.addParameter(param1);
		pipe.addParameter(param2);
		pipe.configure();
		pipe.start();
		
		PipeRunResult prr = doPipe(pipe, null, session);
		
		assertEquals("lessthan", prr.getPipeForward().getName());
	}
	
	@Test
	public void testEquals() throws Exception {
		pipe.registerForward(new PipeForward("lessthan",null));
		pipe.registerForward(new PipeForward("greaterthan",null));
		pipe.registerForward(new PipeForward("equals",null));
		Parameter param1 = new Parameter();
		param1.setName("operand1");
		param1.setValue("a");
		pipe.addParameter(param1);
		pipe.configure();
		pipe.start();
		
		PipeRunResult prr = doPipe(pipe, "a", session);
		
		assertEquals("equals", prr.getPipeForward().getName());
	}

	@Test
	public void testgreaterThan() throws Exception {
		pipe.registerForward(new PipeForward("lessthan",null));
		pipe.registerForward(new PipeForward("greaterthan",null));
		pipe.registerForward(new PipeForward("equals",null));
		Parameter param1 = new Parameter();
		param1.setName("operand2");
		param1.setValue("a");
		pipe.addParameter(param1);
		pipe.configure();
		pipe.start();
		
		PipeRunResult prr = doPipe(pipe, "b", session);
		
		assertEquals("greaterthan", prr.getPipeForward().getName());
	}
}