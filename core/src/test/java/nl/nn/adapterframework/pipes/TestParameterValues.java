package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.testutil.ParameterBuilder;

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
		pipe.addParameter(new ParameterBuilder("param1", "my-value"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("my-value", result.getResult().asString());
	}

	@Test
	public void testParameterWithoutValue() throws Exception {
		pipe.addParameter(new ParameterBuilder("param1", null));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("dummy", result.getResult().asString());
	}

	@Test
	public void testParameterWithDefaultValue() throws Exception {
		pipe.addParameter(new ParameterBuilder("param2", "other-value"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("other-value", result.getResult().asString());
	}

	@Test
	public void testParameterWithoutValueAndDefaultValue() throws Exception {
		pipe.addParameter(new ParameterBuilder("param1", "fallback-value"));
		pipe.addParameter(new ParameterBuilder("param2", null));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("fallback-value", result.getResult().asString());
	}
}
