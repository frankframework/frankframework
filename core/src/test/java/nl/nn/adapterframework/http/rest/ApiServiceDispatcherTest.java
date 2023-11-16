package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.http.rest.ApiListenerServletTest.Methods;
import nl.nn.adapterframework.util.EnumUtils;

public class ApiServiceDispatcherTest {

	private ApiServiceDispatcher dispatcher = null;
	private int amount = 100;

	@Before
	public void setUp() {
		dispatcher = new ApiServiceDispatcher();
	}

	@After
	public void tearDown() {
		dispatcher = null;
	}

	@Test
	public void testAddManyConcurrentSimultaneousListeners() throws ListenerException, InterruptedException {
		List<Thread> list = new ArrayList<>();

		// Spin up many simultaneous threads to 'bomb' the dispatcher with many concurrent requests
		for (int i = 0; i < amount; i++) {
			String name = "thread"+i;
			CreateListener target = new CreateListener(name);
			Thread t = new Thread(target);
			t.setName(name);
			t.start();
			list.add(t);
		}

		// Join all threads together
		for (Thread thread : list) {
			thread.join();
		}

		// Make sure the expected amount matches the clients registered in the dispatcher!
		assertEquals(amount, dispatcher.getPatternClients().size());
	}

	private class CreateListener implements Runnable {
		private String name = null;

		public CreateListener(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			Double timeout = Math.ceil(Math.random()*3);
			try {
				Thread.sleep(timeout.longValue());
			} catch (InterruptedException e1) {
				//Failed to sleep? unsure why, but doesn't matter much
			}
			ApiListener listener = new ApiListener();
			listener.setName(name);
			listener.setMethod(HttpMethod.GET);
			listener.setUriPattern(name);
			
			try {
				dispatcher.registerServiceClient(listener);
			} catch (ListenerException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private ApiListener createServiceClient(Methods method, String uri) {
		ApiListener listener = new ApiListener();
		listener.setName("Listener4Uri["+uri+"]");
		listener.setMethod(EnumUtils.parse(HttpMethod.class, method.name()));
		listener.setUriPattern(uri);
		return listener;
	}

	@Test
	public void testMultipleMethodsSameEndpoint() throws Exception {
		String uri = "testEndpoint1";
		dispatcher.registerServiceClient(createServiceClient(Methods.GET, uri));
		dispatcher.registerServiceClient(createServiceClient(Methods.POST, uri));
		ApiDispatchConfig config = dispatcher.findConfigForUri("/"+uri);
		assertNotNull(config);
		assertEquals("[GET, POST]", config.getMethods().toString());

		//Test what happens after we remove 1 ServiceClient
		dispatcher.unregisterServiceClient(createServiceClient(Methods.POST, uri));
		ApiDispatchConfig config2 = dispatcher.findConfigForUri("/"+uri);
		assertNotNull(config2);
		assertEquals("[GET]", config2.getMethods().toString());

		//Test what happens after we remove both ServiceClient in the same DispatchConfig
		dispatcher.unregisterServiceClient(createServiceClient(Methods.GET, uri));
		ApiDispatchConfig config3 = dispatcher.findConfigForUri("/"+uri);
		assertNull(config3);
	}
}
