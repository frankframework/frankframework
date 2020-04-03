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

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.Json2XmlValidator;
import nl.nn.adapterframework.receivers.GenericReceiver;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.RunStateEnum;

public class OpenApiTestBase extends Mockito {

	private static TaskExecutor taskExecutor;
	private static Configuration configuration = mock(Configuration.class);
	private ApiListenerServlet servlet;

	@Before
	public void setUp() throws ServletException {
		servlet = new ApiListenerServlet();
		servlet.init();
	}

	@After
	public void tearDown() {
		servlet.destroy();
		servlet = null;
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
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		return response.getContentAsString();
	}

	public static class AdapterBuilder {
		private ApiListener listener;
		private Json2XmlValidator pipe;
		private Adapter adapter;
		private List<Integer> exits = new ArrayList<Integer>();

		public static AdapterBuilder create(String name, String description) {
			return new AdapterBuilder(name, description);
		}
		private AdapterBuilder(String name, String description) {
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
		public AdapterBuilder setValidator(String xsdSchema, String root) {
			String ref = xsdSchema.substring(0, xsdSchema.indexOf("."))+"-"+root;
			pipe = new Json2XmlValidator();
			pipe.setName(ref);
			String xsd = "/OpenApi/"+xsdSchema;
			URL url = this.getClass().getResource(xsd);
			assertNotNull("xsd ["+xsdSchema+"] not found", url);
			pipe.setSchema(xsd);
			pipe.setRoot(root);
			pipe.setThrowException(true);

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
			pipeline.addPipe(pipe);
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
					ple.setEmpty("true");
				default:
					break;
				}
				pipeline.registerPipeLineExit(ple);
			}
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

		public static void start(Adapter... adapters) {
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
			public synchronized void add(String message, String level) {
				add(message, null, level);
			}
			@Override
			public synchronized void add(String message, Date date, String level) {
				System.out.println("SysOutMessageKeeper " + level + " - " + message);
				if("ERROR".equals(level)) fail(message);
			}
		}
	}
}
