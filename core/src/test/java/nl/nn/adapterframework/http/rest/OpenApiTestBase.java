package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.RunStateEnum;

public class OpenApiTestBase extends Mockito {

	private static TaskExecutor taskExecutor;
	private Configuration configuration;
	private ThreadLocalServlet servlets = new ThreadLocalServlet();

	@BeforeClass
	public static void beforeClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	@Before
	public void setUp() throws ServletException {
		configuration = new TestConfiguration();
		AppConstants.getInstance().setProperty("hostname", "hostname");
	}

	@After
	public void tearDown() {
		servlets.remove();

		configuration.close();
	}

	@AfterClass
	public static void afterClass() {
		ApiServiceDispatcher.getInstance().clear();
	}

	private static class ThreadLocalServlet extends ThreadLocal<ApiListenerServlet> {
		@Override
		public ApiListenerServlet get() {
			ApiListenerServlet servlet = super.get();
			if(servlet == null) {
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
			if(servlet != null) {
				servlet.destroy();
			}
			super.remove();
		}
	}

	/**
	 * TaskExecutor used to start and configure adapters
	 */
	private static TaskExecutor getTaskExecutor() {
		if(taskExecutor == null) {
			//Make sure all threads are joining the calling thread
			SyncTaskExecutor executor = new SyncTaskExecutor();
			taskExecutor = executor;
		}
		return taskExecutor;
	}

	protected HttpServletRequest createRequest(String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.toUpperCase(), uri);
		request.setServerName("dummy");
		request.setPathInfo(uri);
		return request;
	}

	protected String callOpenApi(String uri) throws ServletException, IOException {
		return service(createRequest("get", uri + "/openapi.json"));
	}

	protected String service(HttpServletRequest request) throws ServletException, IOException {
		try {
			MockHttpServletResponse response = new MockHttpServletResponse();
			ApiListenerServlet servlet = servlets.get();
			assertNotNull("Servlet cannot be found!??", servlet);

			servlet.service(request, response);

			return response.getContentAsString();
		} catch (Throwable t) {
			//Silly hack to try and make the error visible in Travis.
			assertTrue(ExceptionUtils.getStackTrace(t), false);
		}

		//This should never happen because of the assertTrue(false) statement in the catch cause.
		return null;
	}

	public class AdapterBuilder {
		private ApiListener listener;
		private Json2XmlValidator inputValidator;
		private Json2XmlValidator outputValidator;
		private Adapter adapter;
		private List<PipeLineExit> exits = new ArrayList<PipeLineExit>();

//		public static AdapterBuilder create(String name, String description) {
//			return new AdapterBuilder(name, description);
//		}
		public AdapterBuilder(String name, String description) {
			adapter = spy(Adapter.class);
			when(adapter.getMessageKeeper()).thenReturn(new SysOutMessageKeeper());
			adapter.setName(name);
			adapter.setDescription(description);
			adapter.setConfiguration(configuration);
			adapter.setTaskExecutor(getTaskExecutor());
		}
		public AdapterBuilder setListener(String uriPattern, String method, String operationId) {
			return setListener(uriPattern, method, "json", operationId);
		}
		public AdapterBuilder setListener(String uriPattern, String method, String produces, String operationId) {
			listener = new ApiListener();
			if (method!=null) listener.setMethod(EnumUtils.parse(HttpMethod.class,method));
			listener.setUriPattern(uriPattern);
			if (produces!=null) listener.setProduces(EnumUtils.parse(MediaTypes.class,produces));
			if(StringUtils.isNotEmpty(operationId)) {
				listener.setOperationId(operationId);
			}
			return this;
		}
		public AdapterBuilder setHeaderParams(String headerParams) {
			listener.setHeaderParams(headerParams);
			return this;
		}
//		public AdapterBuilder setCookieParams(String cookieParams) {
//			listener.setCookieParams(cookieParams);
//			return this;
//		}
		public AdapterBuilder setMessageIdHeader(String messageIdHeader) {
			listener.setMessageIdHeader(messageIdHeader);
			return this;
		}
		
		public AdapterBuilder setInputValidator(String xsdSchema, String requestRoot, String responseRoot, Parameter param) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf("."))+"-"+responseRoot;
			inputValidator = new Json2XmlValidator();
			inputValidator.setName(ref);
			String xsd = "/OpenApi/"+xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull("xsd ["+xsdSchema+"] not found", url);
			inputValidator.setSchema(xsd);
			if (requestRoot!=null) {
				inputValidator.setRoot(requestRoot);
			}
			inputValidator.setResponseRoot(responseRoot);
			inputValidator.setThrowException(true);
			if(param != null) {
				inputValidator.addParameter(param);
			}

			return this;
		}
		protected AdapterBuilder setOutputValidator(String xsdSchema, String root) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf("."))+"-"+root;
			outputValidator = new Json2XmlValidator();
			outputValidator.setName(ref);
			String xsd = "/OpenApi/"+xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull("xsd ["+xsdSchema+"] not found", url);
			outputValidator.setSchema(xsd);
			outputValidator.setThrowException(true);
			if (root!=null) {
				outputValidator.setRoot(root);
			}
			return this;
		}
		public AdapterBuilder addExit(String exitCode) {
			return addExit(exitCode, null, "false");
		}
		public AdapterBuilder addExit(String exitCode, String responseRoot, String isEmpty) {
			PipeLineExit ple = new PipeLineExit();
			ple.setCode(exitCode);
			ple.setResponseRoot(responseRoot);
			ple.setEmpty(isEmpty);
			switch (exitCode) {
				case "200":
					ple.setState("success");
					break;
				case "201":
					ple.setState("success");
					break;
				default:
					ple.setState("error");
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
		 * @param start automatically start the adapter upon creation
		 */
		public Adapter build(boolean start) throws ConfigurationException {
			PipeLine pipeline = spy(PipeLine.class);
			Receiver receiver = new Receiver();
			receiver.setName("receiver");
			receiver.setListener(listener);
			pipeline.setInputValidator(inputValidator);
			pipeline.setOutputValidator(outputValidator);
			for (PipeLineExit exit : exits) {
				exit.setPath("success"+exit.getExitCode());

				pipeline.registerPipeLineExit(exit);
			}
			IPipe pipe = new EchoPipe();
			pipe.setName("echo");
			pipeline.addPipe(pipe);

			adapter.setPipeLine(pipeline);
			adapter.registerReceiver(receiver);

			adapter.configure();
			assertTrue("adapter failed to configure!?", adapter.configurationSucceeded());
			assertTrue("receiver failed to configure!?", receiver.configurationSucceeded());

			if(start) {
				start(adapter);
			}

			return adapter;
		}

		public void start(Adapter... adapters) {
			for (Adapter adapter : adapters) {
				System.out.println("attempting to start adapter "+ adapter.getName());
				adapter.startRunning();
			}
			for (Adapter adapter : adapters) {
				while (adapter.getRunState()!=RunStateEnum.STARTED) {
					System.out.println("Adapter RunState: " + adapter.getRunStateAsString());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						fail("InterruptedException occurred");
					}
				}
			}
		}

		private class SysOutMessageKeeper extends MessageKeeper {
			@Override
			public synchronized void add(String message, MessageKeeperLevel level) {
				add(message, null, level);
			}
			@Override
			public synchronized void add(String message, Date date, MessageKeeperLevel level) {
				System.out.println("SysOutMessageKeeper " + level + " - " + message);
				if(MessageKeeperLevel.ERROR.equals(level)) fail(message);
			}
		}
	}
}
