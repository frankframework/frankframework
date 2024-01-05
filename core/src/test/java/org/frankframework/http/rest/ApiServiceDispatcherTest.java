package org.frankframework.http.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.frankframework.core.ListenerException;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.util.EnumUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiServiceDispatcherTest {

	private ApiServiceDispatcher dispatcher = null;
	private final int amount = 100;

	@BeforeEach
	public void setUp() {
		dispatcher = new ApiServiceDispatcher();
	}

	@AfterEach
	public void tearDown() {
		dispatcher = null;
	}

	@Test
	void testAddManyConcurrentSimultaneousListeners() throws InterruptedException {
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
			listener.setMethod(HttpMethod.GET.name());
			listener.setUriPattern(name);

			try {
				dispatcher.registerServiceClient(listener);
			} catch (ListenerException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private ApiListener createServiceClient(ApiListenerServletTest.Methods method, String uri) {
		return createServiceClient(Arrays.asList(method), uri);
	}

	private ApiListener createServiceClient(List<ApiListenerServletTest.Methods> method, String uri) {
		ApiListener listener = new ApiListener();
		listener.setName("Listener4Uri["+uri+"]");

		String methods = method.stream()
				.map(m -> EnumUtils.parse(HttpMethod.class, m.name()).name())
				.collect(Collectors.joining(","));

		listener.setMethod(methods);
		listener.setUriPattern(uri);
		return listener;
	}

	@Test
	void testMultipleMethodsSameEndpoint() throws Exception {
		String uri = "testEndpoint1";
		dispatcher.registerServiceClient(createServiceClient(ApiListenerServletTest.Methods.GET, uri));
		dispatcher.registerServiceClient(createServiceClient(ApiListenerServletTest.Methods.POST, uri));
		testMultipleMethods(uri);
	}

	@Test
	void testMultipleMethodsSameEndpointSameListener() throws Exception {
		String uri = "testEndpoint1";
		dispatcher.registerServiceClient(createServiceClient(Arrays.asList(ApiListenerServletTest.Methods.GET, ApiListenerServletTest.Methods.POST), uri));
		testMultipleMethods(uri);
	}

	private void testMultipleMethods(String uri){
		ApiDispatchConfig config = dispatcher.findConfigForUri("/"+uri);
		assertNotNull(config);
		assertEquals("[GET, POST]", config.getMethods().toString());

		//Test what happens after we remove 1 ServiceClient
		dispatcher.unregisterServiceClient(createServiceClient(ApiListenerServletTest.Methods.POST, uri));
		ApiDispatchConfig config2 = dispatcher.findConfigForUri("/"+uri);
		assertNotNull(config2);
		assertEquals("[GET]", config2.getMethods().toString());

		//Test what happens after we remove both ServiceClient in the same DispatchConfig
		dispatcher.unregisterServiceClient(createServiceClient(ApiListenerServletTest.Methods.GET, uri));
		ApiDispatchConfig config3 = dispatcher.findConfigForUri("/"+uri);
		assertNull(config3);
	}
}
