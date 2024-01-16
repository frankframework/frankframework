package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.ThreadContext;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.junit.jupiter.api.Test;

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
