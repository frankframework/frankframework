package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import nl.nn.adapterframework.util.TestAppender;
import nl.nn.adapterframework.web.filters.DeprecationFilter;

public class TestDeprecationFilter {

	private static final String CXF_METHOD_KEY = "org.apache.cxf.resource.method";

	@Test
	public void testDefaultBehaviour() throws Exception {
		DeprecationFilter filter = new DeprecationFilter();
		Environment env = mock(Environment.class);
		when(env.getProperty(any(), any(), any())).thenReturn(false);
		filter.setEnvironment(env);

		ContainerRequestContext request = mock(ContainerRequestContext.class);
		setMethod(ClassWithSlash.class, "normalMethod");

		// Act
		filter.filter(request);

		// Assert
		verify(request, never()).abortWith(any(Response.class));
	}

	@Test
	public void testDeprecatedMethodNotAllowed() throws Exception {
		DeprecationFilter filter = new DeprecationFilter();
		Environment env = mock(Environment.class);
		when(env.getProperty(any(), any(), any())).thenReturn(false);
		filter.setEnvironment(env);

		ContainerRequestContext request = mock(ContainerRequestContext.class);
		ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
		doNothing().when(request).abortWith(messageCapture.capture());

		setMethod(ClassWithSlash.class, "deprecatedMethod");

		// Act
		filter.filter(request);

		// Assert
		verify(request, times(1)).abortWith(any(Response.class));
		assertEquals(400, messageCapture.getValue().getStatus());
	}

	@Test
	public void testDeprecatedMethodAllowed() throws Exception {
		TestAppender appender = TestAppender.newBuilder().build();
		TestAppender.setRootLogLevel(Level.WARN);
		TestAppender.addToRootLogger(appender);
		try {
			DeprecationFilter filter = new DeprecationFilter();
			Environment env = mock(Environment.class);
			when(env.getProperty(any(), any(), any())).thenReturn(true);
			filter.setEnvironment(env);

			ContainerRequestContext request = mock(ContainerRequestContext.class);
			ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
			doNothing().when(request).abortWith(messageCapture.capture());

			setMethod(ClassWithSlash.class, "deprecatedMethod");

			// Act
			filter.filter(request);

			// Assert
			verify(request, never()).abortWith(any(Response.class));
		} finally {
			TestAppender.removeAppender(appender);
			TestAppender.setRootLogLevel(Level.ERROR);
		}
		assertTrue(appender.contains("endpoint [/request/path2] has been deprecated"));
	}

	@Test
	public void testDeprecatedMethodWithCombinedPath() throws Exception {
		TestAppender appender = TestAppender.newBuilder().build();
		TestAppender.setRootLogLevel(Level.WARN);
		TestAppender.addToRootLogger(appender);
		try {
			DeprecationFilter filter = new DeprecationFilter();
			Environment env = mock(Environment.class);
			when(env.getProperty(any(), any(), any())).thenReturn(true);
			filter.setEnvironment(env);

			ContainerRequestContext request = mock(ContainerRequestContext.class);
			ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
			doNothing().when(request).abortWith(messageCapture.capture());

			setMethod(ClassWithCombinedPath.class, "deprecatedMethod");

			// Act
			filter.filter(request);

			// Assert
			verify(request, never()).abortWith(any(Response.class));
		} finally {
			TestAppender.removeAppender(appender);
			TestAppender.setRootLogLevel(Level.ERROR);
		}
		assertTrue(appender.contains("endpoint [/base/path/request] has been deprecated"));
	}

	private void setMethod(Class<?> targetClass, String methodName) throws Exception {
		Message message = new MessageImpl();
		Method method = targetClass.getMethod(methodName, new Class[] {});
		message.put(CXF_METHOD_KEY, method);

		SortedSet<Phase> ps = new TreeSet<>();
		PhaseInterceptorChain chain = new PhaseInterceptorChain(ps);
		Field currentMessageField = PhaseInterceptorChain.class.getDeclaredField("CURRENT_MESSAGE");
		currentMessageField.setAccessible(true);
		@SuppressWarnings("unchecked")
		ThreadLocal<Message> threadLocal = (ThreadLocal<Message>) currentMessageField.get(chain);
		threadLocal.set(message);
	}

	@Path("/")
	private static class ClassWithSlash {

		@Path("/request/path1")
		public Response normalMethod() {
			return Response.accepted().build();
		}

		@Path("/request/path2")
		@Deprecated
		public Response deprecatedMethod() {
			return Response.accepted().build();
		}
	}

	@Path("base/path")
	private static class ClassWithCombinedPath {

		@Path("/request")
		@Deprecated
		public Response deprecatedMethod() {
			return Response.accepted().build();
		}
	}
}
