package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;

public class LogContextPipeTest extends PipeTestBase<LogContextPipe>{

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		ThreadContext.clearMap();
	}

	@Override
	@AfterEach
	public void tearDown() {
		super.tearDown();
		ThreadContext.clearMap();
	}

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

		PipeRunResult prr;
		try (CloseableThreadContext.Instance outer = CloseableThreadContext.put("pipe", "Dummy Outer Value")) {
			try (CloseableThreadContext.Instance inner = CloseableThreadContext.put("pipe", "LogContextPipe")) {
				assertEquals("LogContextPipe", ThreadContext.get("pipe"));

				prr = doPipe(input);

				assertEquals("LogContextPipe", ThreadContext.get("pipe"));

				ThreadContext.put("test", "value");
				assertEquals("value", ThreadContext.get("test"));
			}
			assertEquals("value", ThreadContext.get("test"));
		}
		assertEquals("value", ThreadContext.get("test"));

		assertNull(ThreadContext.get("pipe"));

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

		PipeRunResult prr;
		try (CloseableThreadContext.Instance inner = CloseableThreadContext.put("pipe", "LogContextPipe")) {
			assertEquals("LogContextPipe", ThreadContext.get("pipe"));

			prr = doPipe(input);

			assertEquals("LogContextPipe", ThreadContext.get("pipe"));
		}
		assertNull(ThreadContext.get("pipe"));
		assertEquals(input, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("paramValue", ThreadContext.get("paramName"));

		session.close();
		assertEquals("paramValue", ThreadContext.get("paramName"));
	}

	@Test
	public void testAutoCloseableThreadContext() {
		try (final CloseableThreadContext.Instance ctc1 = CloseableThreadContext.put("outer", "one")) {
			try (final CloseableThreadContext.Instance ctc2 = CloseableThreadContext.put("outer", "two")) {
				assertEquals("two", ThreadContext.get("outer"));

				try (final CloseableThreadContext.Instance ctc3 = CloseableThreadContext.put("inner", "one")) {
					assertEquals("one", ThreadContext.get("inner"));

					ThreadContext.put("not-in-closeable", "true");
					assertEquals("two", ThreadContext.get("outer"));
				}

				assertEquals("two", ThreadContext.get("outer"));
				assertNull(ThreadContext.get("inner"));
			}

			assertEquals("one", ThreadContext.get("outer"));
			assertNull(ThreadContext.get("inner"));
		}
		assertEquals("true", ThreadContext.get("not-in-closeable"));

		assertNull(ThreadContext.get("inner"));
		assertNull(ThreadContext.get("outer"));
	}

	@Test
	public void testAutoCloseableThreadContextPutAll() {
		try (final CloseableThreadContext.Instance ctc1 = CloseableThreadContext.put("outer", "one")) {
			try (final CloseableThreadContext.Instance ctc2 = CloseableThreadContext.put("outer", "two")) {
				assertEquals("two", ThreadContext.get("outer"));

				try (final CloseableThreadContext.Instance ctc3 = CloseableThreadContext.put("inner", "one")) {
					assertEquals("one", ThreadContext.get("inner"));

					ThreadContext.put("not-in-closeable", "true");
					ThreadContext.putAll(Collections.singletonMap("inner", "two"));
					System.err.println(ThreadContext.getContext());
					assertEquals("two", ThreadContext.get("inner"));
					assertEquals("two", ThreadContext.get("outer"));
				}

				assertEquals("two", ThreadContext.get("outer"));
				assertNull(ThreadContext.get("inner"));
			}

			assertEquals("one", ThreadContext.get("outer"));
			assertNull(ThreadContext.get("inner"));
		}
		assertEquals("true", ThreadContext.get("not-in-closeable"));

		assertNull(ThreadContext.get("inner"));
		assertNull(ThreadContext.get("outer"));
	}

	@Test
	public void testCloseableThreadContextWithMultipleNestingLevels() {
		try (CloseableThreadContext.Instance k1Outer = CloseableThreadContext.put("k1", "outer")) {
			assertEquals("outer", ThreadContext.get("k1"));
			try (CloseableThreadContext.Instance k1Inner = CloseableThreadContext.put("k1", "inner")) {
				assertEquals("inner", ThreadContext.get("k1"));

				try (CloseableThreadContext.Instance k2Outer = CloseableThreadContext.put("k2", "outer")) {
					assertEquals("inner", ThreadContext.get("k1"));
					assertEquals("outer", ThreadContext.get("k2"));
					try (CloseableThreadContext.Instance k2Inner = CloseableThreadContext.put("k2", "inner")) {
						k2Inner.put("k3", "inner");
						assertEquals("inner", ThreadContext.get("k1"));
						assertEquals("inner", ThreadContext.get("k2"));
						assertEquals("inner", ThreadContext.get("k3"));
					}
					assertEquals("inner", ThreadContext.get("k1")); // Expect k1 to still exist but it has disappeared here
					assertEquals("outer", ThreadContext.get("k2"));
					assertNull(ThreadContext.get("k3"));
				}
				assertEquals("inner", ThreadContext.get("k1"));
				assertNull(ThreadContext.get("k2"));
			}
			assertEquals("outer", ThreadContext.get("k1"));
			assertNull(ThreadContext.get("k2"));
		}
		assertNull(ThreadContext.get("k1"));
	}

	@Test
	public void testCloseableThreadContext2() {
		try (CloseableThreadContext.Instance outer = CloseableThreadContext.put("k1", "outer")) {
			assertEquals("outer", ThreadContext.get("k1"));
			try (CloseableThreadContext.Instance inner = CloseableThreadContext.putAll(Collections.singletonMap("k2", "inner"))) {
				assertEquals("inner", ThreadContext.get("k2"));
				assertEquals("outer", ThreadContext.get("k1")); // Cannot find key "k1" anymore, but it should
			}
			assertNull(ThreadContext.get("k2"));
			assertEquals("outer", ThreadContext.get("k1"));
		}
		assertNull(ThreadContext.get("k1"));
	}
}
