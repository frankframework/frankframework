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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.receivers.GenericReceiver;
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
		configuration = mock(Configuration.class);
	}

	@After
	public void tearDown() {
		servlets.remove();

		configuration = null;
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
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(2);
			executor.setMaxPoolSize(10);
			executor.initialize();
			taskExecutor = executor;
		}
		return taskExecutor;
	}

	protected HttpServletRequest createRequest(String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.toUpperCase(), uri);
		request.setPathInfo(uri);
		return request;
	}

	protected String callOpenApi() throws ServletException, IOException {
		return service(createRequest("get", "openapi.json"));
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
		private Json2XmlValidator validator;
		private Adapter adapter;
		private List<Integer> exits = new ArrayList<Integer>();

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
		public AdapterBuilder setListener(String uriPattern, String method) {
			return setListener(uriPattern, method, "json");
		}
		public AdapterBuilder setListener(String uriPattern, String method, String produces) {
			listener = new ApiListener();
			listener.setMethod(method);
			listener.setUriPattern(uriPattern);
			listener.setProduces(produces);
			if(method.equalsIgnoreCase("POST")) {
				exits.add(201);
			} else {
				exits.add(200);
			}
			exits.add(500);

			return this;
		}
		public AdapterBuilder setValidator(String xsdSchema, String requestRoot, String responseRoot) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf("."))+"-"+responseRoot;
			validator = new Json2XmlValidator();
			validator.setName(ref);
			String xsd = "/OpenApi/"+xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull("xsd ["+xsdSchema+"] not found", url);
			validator.setSchema(xsd);
			if (requestRoot!=null) {
				validator.setRoot(requestRoot);
			}
			validator.setResponseRoot(responseRoot);
			validator.setThrowException(true);

			return this;
		}
		public AdapterBuilder addExit(int exitCode) {
			this.exits.add(exitCode);
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
			GenericReceiver receiver = new GenericReceiver();
			receiver.setName("receiver");
			receiver.setListener(listener);
			pipeline.setInputValidator(validator);
			for (Integer exit : exits) {
				PipeLineExit ple = new PipeLineExit();
				ple.setPath("success"+exit);
				ple.setState("success");

				switch (exit) {
				case 200:
					ple.setCode("200");
					break;
				case 201:
					ple.setCode("201");
					ple.setEmpty("true");
					break;
				case 500:
					ple.setCode("500");
					ple.setState("error");
				default:
					break;
				}
				pipeline.registerPipeLineExit(ple);
			}
			IPipe pipe = new EchoPipe();
			pipe.setName("echo");
			pipeline.addPipe(pipe);

			adapter.registerPipeLine(pipeline);
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
				while (!adapter.getRunState().equals(RunStateEnum.STARTED)) {
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
