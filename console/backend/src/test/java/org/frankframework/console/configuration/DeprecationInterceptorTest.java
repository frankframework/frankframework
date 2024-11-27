package org.frankframework.console.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.frankframework.console.controllers.FrankApiTestBase;
import org.frankframework.console.controllers.WebTestConfiguration;
import org.frankframework.console.util.TestAppender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ContextConfiguration(classes = {WebTestConfiguration.class, DeprecationInterceptorTest.ClassWithSlash.class})
public class DeprecationInterceptorTest extends FrankApiTestBase {

//	private static final String CXF_METHOD_KEY = "org.apache.cxf.resource.method";

	@Autowired
	private RequestMappingHandlerAdapter handlerAdapter;

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	public void testDefaultBehaviour() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/request/path1");
		request.setMethod("GET");

		MockHttpServletResponse response = new MockHttpServletResponse();

		DeprecationInterceptor interceptor = new DeprecationInterceptor();
		Environment env = Mockito.mock(Environment.class);
		Mockito.when(env.getProperty(Mockito.eq(DeprecationInterceptor.ALLOW_DEPRECATED_ENDPOINTS_KEY), Mockito.any(), Mockito.any())).thenReturn(false);
		interceptor.setEnvironment(env);

		HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
//		HandlerInterceptor[] interceptors = handlerExecutionChain.getInterceptors();

		interceptor.preHandle(request, response, handlerExecutionChain.getHandler());
		handlerAdapter.handle(request, response, handlerExecutionChain.getHandler());

		assertEquals(202, response.getStatus());
	}

	@Test
	public void testDeprecatedMethodNotAllowed() throws Exception {
		TestAppender.setRootLogLevel(Level.WARN);
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setRequestURI("/request/path2");
			request.setMethod("GET");

			MockHttpServletResponse response = new MockHttpServletResponse();

			DeprecationInterceptor interceptor = new DeprecationInterceptor();
			Environment env = Mockito.mock(Environment.class);
			Mockito.when(env.getProperty(Mockito.eq(DeprecationInterceptor.ALLOW_DEPRECATED_ENDPOINTS_KEY), Mockito.any(), Mockito.any())).thenReturn(false);
			interceptor.setEnvironment(env);

			HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
//		HandlerInterceptor[] interceptors = handlerExecutionChain.getInterceptors();

			interceptor.preHandle(request, response, handlerExecutionChain.getHandler());
			handlerAdapter.handle(request, response, handlerExecutionChain.getHandler());

			assertEquals(400, response.getStatus());
			assertTrue(appender.contains("endpoint [/request/path2] has been deprecated"));
		} finally {
			TestAppender.setRootLogLevel(Level.ERROR);
		}
	}

	@Test
	public void testDeprecatedMethodAllowed() throws Exception {
		TestAppender.setRootLogLevel(Level.WARN);
		try (TestAppender appender = TestAppender.newBuilder().build()) {
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setRequestURI("/request/path2");
			request.setMethod("GET");

			MockHttpServletResponse response = new MockHttpServletResponse();

			DeprecationInterceptor interceptor = new DeprecationInterceptor();
			Environment env = Mockito.mock(Environment.class);
			Mockito.when(env.getProperty(Mockito.eq(DeprecationInterceptor.ALLOW_DEPRECATED_ENDPOINTS_KEY), Mockito.any(), Mockito.any())).thenReturn(true);
			interceptor.setEnvironment(env);

			HandlerExecutionChain handlerExecutionChain = handlerMapping.getHandler(request);
//		HandlerInterceptor[] interceptors = handlerExecutionChain.getInterceptors();

			interceptor.preHandle(request, response, handlerExecutionChain.getHandler());
			handlerAdapter.handle(request, response, handlerExecutionChain.getHandler());

			assertEquals(202, response.getStatus());
			assertTrue(appender.getLogLines().isEmpty());
		} finally {
			TestAppender.setRootLogLevel(Level.ERROR);
		}
	}

	@RestController("/")
	protected static class ClassWithSlash {

		@GetMapping("/request/path1")
		public ResponseEntity<?> normalMethod() {
			return ResponseEntity.accepted().build();
		}

		@GetMapping("/request/path2")
		@Deprecated
		public ResponseEntity<?> deprecatedMethod() {
			return ResponseEntity.accepted().build();
		}
	}

}
