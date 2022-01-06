package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class TestParameterValues extends PipeTestBase<ParameterValueTestPipe>{

	/**
	 * Test all possible options for FixedForwardPipe#getParameterValue(ParameterValueList, String)
	 */
	@Override
	public ParameterValueTestPipe createPipe() throws ConfigurationException {
		return new ParameterValueTestPipe();
	}

	@Test
	public void testParameterValue() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setValue("my-value");
		pipe.addParameter(param1);
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("my-value", result.getResult().asString());
	}

	@Test
	public void testParameterWithoutValue() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("param1");
		pipe.addParameter(param1);
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("dummy", result.getResult().asString());
	}

	@Test
	public void testParameterWithDefaultValue() throws Exception {
		Parameter param2 = new Parameter();
		param2.setName("param2");
		param2.setValue("other-value");
		pipe.addParameter(param2);
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("other-value", result.getResult().asString());
	}

	@Test
	public void testParameterWithoutValueAndDefaultValue() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setValue("fallback-value");
		pipe.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("param2");
		pipe.addParameter(param2);
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("fallback-value", result.getResult().asString());
	}
}
