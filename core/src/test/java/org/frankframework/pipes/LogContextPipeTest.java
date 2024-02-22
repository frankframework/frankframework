package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.ThreadContext;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.junit.jupiter.api.Test;

public class LogContextPipeTest extends PipeTestBase<LogContextPipe>{

	@Override
	public LogContextPipe createPipe() throws ConfigurationException {
		return new LogContextPipe();
	}

	@Test
	public void testLogContextPipe() throws Exception {
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
	public void testLogContextPipeWithException() throws Exception {
		Parameter mockParam = mock(Parameter.class);
		when(mockParam.getName()).thenReturn("mock-param");
		when(mockParam.getValue(any(), any(), any(), anyBoolean())).thenThrow(NullPointerException.class);
		pipe.addParameter(mockParam);
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		assertThrows(PipeRunException.class, ()-> doPipe(input));
		assertNull(ThreadContext.get("mock-param"));
	}

	@Test
	public void testLogContextPipeWithContinueOnException1() throws Exception {
		Parameter mockParam = mock(Parameter.class);
		when(mockParam.getName()).thenReturn("mock-param");
		when(mockParam.getValue(any(), any(), any(), anyBoolean())).thenThrow(NullPointerException.class);
		pipe.addParameter(mockParam);
		pipe.setContinueOnError(true);
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		PipeRunResult prr = doPipe(input);

		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertNull(ThreadContext.get("mock-param"));
	}
	@Test
	public void testLogContextPipeWithContinueOnException2() throws Exception {
		Parameter mockParam = mock(Parameter.class);
		when(mockParam.getName()).thenReturn("mock-param");
		when(mockParam.getValue(any(), any(), any(), anyBoolean())).thenThrow(new ParameterException("mock-param", "this is my message"));
		pipe.addParameter(mockParam);
		pipe.setContinueOnError(true);
		configureAndStartPipe();

		String input = "fakeInput";
		ThreadContext.clearMap();

		PipeRunResult prr = doPipe(input);

		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertNotNull(ThreadContext.get("mock-param"));
		assertEquals("this is my message", ThreadContext.get("mock-param"));
	}

	@Test
	public void testLogContextPipeExport() throws Exception {
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
