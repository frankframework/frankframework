package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class MdcPipeTest extends PipeTestBase<MdcPipe>{

	@Override
	public MdcPipe createPipe() throws ConfigurationException {
		return new MdcPipe();
	}

	@Test
	public void testMdcPipe() throws Exception {
		pipe.addParameter(new Parameter("paramName", "paramValue"));
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		PipeRunResult prr = doPipe(input);

		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("paramValue", ThreadContext.get("paramName"));
	}

}
