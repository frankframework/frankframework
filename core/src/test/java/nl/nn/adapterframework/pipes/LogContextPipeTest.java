package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

public class LogContextPipeTest extends PipeTestBase<LogContextPipe>{

	@Override
	public LogContextPipe createPipe() throws ConfigurationException {
		return new LogContextPipe();
	}

	@Test
	public void testLogContexPipe() throws Exception {
		pipe.addParameter(new Parameter("paramName", "paramValue"));
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		PipeRunResult prr = doPipe(input);

		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("paramValue", ThreadContext.get("paramName"));

		session.close();
		assertNull(ThreadContext.get("paramName"));
	}

	@Test
	public void testLogContexPipeExport() throws Exception {
		pipe.addParameter(new Parameter("paramName", "paramValue"));
		pipe.setExport(true);
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		PipeRunResult prr = doPipe(input);

		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("paramValue", ThreadContext.get("paramName"));

		session.close();
		assertEquals("paramValue", ThreadContext.get("paramName"));
	}
}
