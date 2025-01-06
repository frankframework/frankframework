package org.frankframework.http.openapi;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.http.rest.ApiListenerServlet;
import org.frankframework.http.rest.ApiServiceDispatcher;
import org.frankframework.http.rest.MediaTypes;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.Json2XmlValidator;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.HttpUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

public class OpenApiTestBase extends Mockito {

	private static TaskExecutor taskExecutor;
	private final Logger log = LogUtil.getLogger(this);
	private final ThreadLocalServlet servlets = new ThreadLocalServlet();
	private Configuration configuration;

	@BeforeAll
	public static void beforeClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	@BeforeEach
	public void setUp() {
		AppConstants.getInstance().setProperty("hostname", "hostname");
		AppConstants.getInstance().setProperty("dtap.stage", "xxx");
		configuration = new TestConfiguration();
	}

	@AfterEach
	public void tearDown() {
		servlets.remove();

		configuration.close();
	}

	@AfterAll
	public static void afterClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	/**
	 * TaskExecutor used to start and configure adapters
	 */
	private static TaskExecutor getTaskExecutor() {
		if (taskExecutor == null) {
			//Make sure all threads are joining the calling thread
			SyncTaskExecutor executor = new SyncTaskExecutor();
			taskExecutor = executor;
		}
		return taskExecutor;
	}

	protected MockHttpServletRequest createRequest(String method, String uri) {
		if (!uri.startsWith("/")) {
			fail("uri must start with a '/'");
		}

		MockHttpServletRequest request = new MockHttpServletRequest(method.toUpperCase(), uri);
		request.setServerName("mock-hostname");
		request.setPathInfo(HttpUtils.urlDecode(uri)); //Should be decoded by the web container
		request.setContextPath("/mock-context-path");
		request.setServletPath("/mock-servlet-path");
		request.setRequestURI(request.getContextPath() + request.getServletPath() + uri);
		return request;
	}

	protected String callOpenApi(String uri) throws ServletException, IOException {
		return service(createRequest("get", uri + "/openapi.json"));
	}

	protected String service(HttpServletRequest request) throws ServletException, IOException {
		try {
			MockHttpServletResponse response = new MockHttpServletResponse();
			ApiListenerServlet servlet = servlets.get();
			assertNotNull(servlet, "Servlet cannot be found!??");

			servlet.service(request, response);

			String res = response.getContentAsString();
			return res.replaceFirst("auto-generated at .* for", "auto-generated at -timestamp- for");
		} catch (Throwable t) {
			//Silly hack to try and make the error visible in Travis.
			fail(ExceptionUtils.getStackTrace(t));
		}

		//This should never happen because of the assertTrue(false) statement in the catch cause.
		return null;
	}

	private static class ThreadLocalServlet extends ThreadLocal<ApiListenerServlet> {
		@Override
		public ApiListenerServlet get() {
			ApiListenerServlet servlet = super.get();
			if (servlet == null) {
				servlet = new ApiListenerServlet();
				try {
					servlet.init();
				} catch (ServletException e) {
					throw new RuntimeException("error starting servlet");
				}
				set(servlet);
			}
			return servlet;
		}

		@Override
		public void remove() {
			ApiListenerServlet servlet = super.get();
			if (servlet != null) {
				servlet.destroy();
			}
			super.remove();
		}
	}

	public class AdapterBuilder {
		private final Adapter adapter;
		private final List<PipeLineExit> exits = new ArrayList<>();
		private ApiListener listener;
		private Json2XmlValidator inputValidator;
		private Json2XmlValidator outputValidator;

		//		public static AdapterBuilder create(String name, String description) {
//			return new AdapterBuilder(name, description);
//		}
		public AdapterBuilder(String name, String description) {
			adapter = spy(SpringUtils.createBean(configuration, Adapter.class));
			when(adapter.getMessageKeeper()).thenReturn(new SysOutMessageKeeper());
			adapter.setName(name);
			adapter.setDescription(description);
			adapter.setConfiguration(configuration);
			adapter.setTaskExecutor(getTaskExecutor());
		}

		public AdapterBuilder setListener(String uriPattern, ApiListener.HttpMethod method, String operationId) {
			return setListener(uriPattern, List.of(method), "json", operationId);
		}

		public AdapterBuilder setListener(String uriPattern, List<ApiListener.HttpMethod> method, String produces, String operationId) {
			listener = new ApiListener();

			if (method != null) {
				listener.setMethods(method.toArray(new ApiListener.HttpMethod[0]));
			}

			listener.setUriPattern(uriPattern);

			if (produces != null) {
				listener.setProduces(EnumUtils.parse(MediaTypes.class, produces));
			}

			if (StringUtils.isNotEmpty(operationId)) {
				listener.setOperationId(operationId);
			}
			return this;
		}

		public AdapterBuilder setHeaderParams(String headerParams) {
			listener.setHeaderParams(headerParams);
			return this;
		}

		public AdapterBuilder setMessageIdHeader(String messageIdHeader) {
			listener.setMessageIdHeader(messageIdHeader);
			return this;
		}

		public AdapterBuilder setInputValidator(String xsdSchema, String requestRoot, String responseRoot, Parameter param) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf(".")) + "-" + responseRoot;
			inputValidator = new Json2XmlValidator();
			inputValidator.setName(ref);
			String xsd = "/OpenApi/" + xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull(url, "xsd [" + xsdSchema + "] not found");
			inputValidator.setSchema(xsd);
			if (requestRoot != null) {
				inputValidator.setRoot(requestRoot);
			}
			inputValidator.setResponseRoot(responseRoot);
			inputValidator.setThrowException(true);
			if (param != null) {
				inputValidator.addParameter(param);
			}

			return this;
		}

		protected AdapterBuilder setOutputValidator(String xsdSchema, String root) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf(".")) + "-" + root;
			outputValidator = new Json2XmlValidator();
			outputValidator.setName(ref);

			String xsd = "/OpenApi/" + xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull(url, "xsd [" + xsdSchema + "] not found");

			outputValidator.setSchema(xsd);
			outputValidator.setThrowException(true);

			if (root != null) {
				outputValidator.setRoot(root);
			}

			return this;
		}

		public AdapterBuilder addExit(int exitCode) {
			return addExit(exitCode, null, false);
		}

		public AdapterBuilder addExit(int exitCode, String responseRoot, boolean isEmpty) {
			PipeLineExit ple = new PipeLineExit();
			ple.setCode(exitCode);
			ple.setResponseRoot(responseRoot);
			ple.setEmpty(isEmpty);

			switch (exitCode) {
				case 200:
				case 201:
					ple.setState(ExitState.SUCCESS);
					break;
				default:
					ple.setState(ExitState.ERROR);
					break;
			}
			this.exits.add(ple);
			return this;
		}

		public Adapter build() throws ConfigurationException {
			return build(false);
		}

		/**
		 * Create the adapter
		 *
		 * @param start automatically start the adapter upon creation
		 */
		public Adapter build(boolean start) throws ConfigurationException {
			PipeLine pipeline = spy(SpringUtils.createBean(configuration, PipeLine.class));

			Receiver receiver = SpringUtils.createBean(configuration, Receiver.class);
			receiver.setName("receiver");
			receiver.setListener(listener);

			pipeline.setInputValidator(inputValidator);
			pipeline.setOutputValidator(outputValidator);

			for (PipeLineExit exit : exits) {
				exit.setName("success" + exit.getExitCode());

				pipeline.addPipeLineExit(exit);
			}
			IPipe pipe = new EchoPipe();
			pipe.setName("echo");
			pipeline.addPipe(pipe);

			adapter.setPipeLine(pipeline);
			adapter.addReceiver(receiver);
			adapter.configure();

			assertTrue(adapter.configurationSucceeded(), "adapter failed to configure!?");
			assertTrue(receiver.configurationSucceeded(), "receiver failed to configure!?");

			if (start) {
				start(adapter);
			}

			return adapter;
		}

		public void start(Adapter... adapters) {
			for (Adapter adapter : adapters) {
				log.info("attempting to start adapter [{}]", adapter::getName);
				adapter.startRunning();
			}
			for (Adapter adapter : adapters) {
				log.info("adapter RunState [{}]", adapter::getRunState);
				await().pollInterval(10, TimeUnit.MILLISECONDS)
						.atMost(5, TimeUnit.SECONDS)
						.until(() -> adapter.getRunState() == RunState.STARTED);
			}
		}

		private class SysOutMessageKeeper extends MessageKeeper {
			@Override
			public synchronized void add(String message, MessageKeeperLevel level) {
				add(message, null, level);
			}

			@Override
			public synchronized void add(String message, Instant date, MessageKeeperLevel level) {
				log.debug("SysOutMessageKeeper {} - {}", level, message);
				if (MessageKeeperLevel.ERROR == level) fail(message);
			}
		}
	}
}
