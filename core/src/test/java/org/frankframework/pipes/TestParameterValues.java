package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.ParameterBuilder;

public class TestParameterValues extends PipeTestBase<ParameterValueTestPipe>{

	/**
	 * Test all possible options for FixedForwardPipe#getParameterValue(ParameterValueList, String)
	 */
	@Override
	public ParameterValueTestPipe createPipe() throws ConfigurationException {
		return new ParameterValueTestPipe();
	}

	@Test
	void testParameterValue() throws Exception {
		pipe.addParameter(new Parameter("param1", "my-value"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("my-value", result.getResult().asString());
	}

	@Test
	void testParameterWithoutValue() throws Exception {
		pipe.addParameter(ParameterBuilder.create().withName("param1"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("dummy", result.getResult().asString());
	}

	@Test
	void testParameterWithDefaultValue() throws Exception {
		pipe.addParameter(new Parameter("param2", "other-value"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("other-value", result.getResult().asString());
	}

	@Test
	void testParameterWithoutValueAndDefaultValue() throws Exception {
		pipe.addParameter(new Parameter("param1", "fallback-value"));
		pipe.addParameter(ParameterBuilder.create().withName("param2"));
		pipe.configure();

		PipeRunResult result = doPipe("dummy");
		assertEquals("fallback-value", result.getResult().asString());
	}
}
