package org.frankframework.management.web.spring;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, Monitors.class})
public class DeprecationFilterTest extends FrankApiTestBase{

//	private static final String CXF_METHOD_KEY = "org.apache.cxf.resource.method";

	/*@BeforeEach
	public void setUp() {
		super.setUp();
		DefaultMockMvcBuilder mockBuilder = MockMvcBuilders.webAppContextSetup(webApplicationContext);
		this.mockMvc = mockBuilder.addFilter(new DeprecationFilter()).build();
	}*/

	/*@Test
	public void testDefaultBehaviour() throws Exception {
		DeprecationFilter filter = new DeprecationFilter();
		Environment env = Mockito.mock(Environment.class);
		Mockito.when(env.getProperty(ArgumentMatchers.eq(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY), Mockito.any(), Mockito.any())).thenReturn(false);
		filter.setEnvironment(env);

		ContainerRequestContext request = Mockito.mock(ContainerRequestContext.class);
		setMethod(DeprecationFilterTest.ClassWithSlash.class, "normalMethod");

		// Act
		filter.filter(request);

		// Assert
		Mockito.verify(request, Mockito.never()).abortWith(Mockito.any(Response.class));
	}*/

	/*@Test
	public void testDeprecatedMethodNotAllowed() throws Exception {
		TestAppender appender = TestAppender.newBuilder().build();
		TestAppender.setRootLogLevel(Level.WARN);
		TestAppender.addToRootLogger(appender);
		try {
			DeprecationFilter filter = new DeprecationFilter();
			Environment env = mock(Environment.class);
			when(env.getProperty(eq(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY), any(), any())).thenReturn(false);
			filter.setEnvironment(env);

			ContainerRequestContext request = mock(ContainerRequestContext.class);
			ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
			doNothing().when(request).abortWith(messageCapture.capture());

			setMethod(TestDeprecationFilter.ClassWithSlash.class, "deprecatedMethod");

			// Act
			filter.filter(request);

			// Assert
			verify(request, times(1)).abortWith(any(Response.class));
			assertEquals(400, messageCapture.getValue().getStatus());
		} finally {
			TestAppender.removeAppender(appender);
			TestAppender.setRootLogLevel(Level.ERROR);
		}
		assertTrue(appender.contains("endpoint [/request/path2] has been deprecated"));
	}*/

	/*@Test
	public void testDeprecatedMethodAllowed() throws Exception {
		TestAppender appender = TestAppender.newBuilder().build();
		TestAppender.setRootLogLevel(Level.WARN);
		TestAppender.addToRootLogger(appender);
		try {
			DeprecationFilter filter = new DeprecationFilter();
			Environment env = mock(Environment.class);
			when(env.getProperty(eq(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY), any(), any())).thenReturn(true);
			filter.setEnvironment(env);

			ContainerRequestContext request = mock(ContainerRequestContext.class);
			ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
			doNothing().when(request).abortWith(messageCapture.capture());

			setMethod(TestDeprecationFilter.ClassWithSlash.class, "deprecatedMethod");

			// Act
			filter.filter(request);

			// Assert
			verify(request, never()).abortWith(any(Response.class));
		} finally {
			TestAppender.removeAppender(appender);
			TestAppender.setRootLogLevel(Level.ERROR);
		}
		assertTrue(appender.getLogLines().isEmpty());
	}*/

	/*@Test
	public void testDeprecatedMethodWithCombinedPath() throws Exception {
		TestAppender appender = TestAppender.newBuilder().build();
		TestAppender.setRootLogLevel(Level.WARN);
		TestAppender.addToRootLogger(appender);
		try {
			DeprecationFilter filter = new DeprecationFilter();
			Environment env = mock(Environment.class);
			when(env.getProperty(eq(DeprecationFilter.ALLOW_DEPRECATED_ENDPOINTS_KEY), any(), any())).thenReturn(true);
			filter.setEnvironment(env);

			ContainerRequestContext request = mock(ContainerRequestContext.class);
			ArgumentCaptor<Response> messageCapture = ArgumentCaptor.forClass(Response.class);
			doNothing().when(request).abortWith(messageCapture.capture());

			setMethod(TestDeprecationFilter.ClassWithCombinedPath.class, "deprecatedMethod");

			// Act
			filter.filter(request);

			// Assert
			verify(request, never()).abortWith(any(Response.class));
		} finally {
			TestAppender.removeAppender(appender);
			TestAppender.setRootLogLevel(Level.ERROR);
		}
		assertTrue(appender.getLogLines().isEmpty());
	}*/

	/*private void setMethod(Class<?> targetClass, String methodName) throws Exception {
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
	}*/

}
