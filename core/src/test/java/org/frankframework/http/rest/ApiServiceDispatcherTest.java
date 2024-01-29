package org.frankframework.http.rest;

import org.frankframework.core.ListenerException;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.util.EnumUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ApiServiceDispatcherTest {

	private ApiServiceDispatcher dispatcher = null;
	private final int amount = 100;
	private static final String PATH = "/test/config/all/to/test/wildcards";

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
			listener.setMethod(HttpMethod.GET);
			listener.setUriPattern(name);

			try {
				dispatcher.registerServiceClient(listener);
			} catch (ListenerException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private ApiListener createServiceClient(ApiListenerServletTest.Methods method, String uri) {
		return createServiceClient(List.of(method), uri);
	}

	private ApiListener createServiceClient(List<ApiListenerServletTest.Methods> method, String uri) {
		ApiListener listener = new ApiListener();
		listener.setName("Listener4Uri["+uri+"]");

		String methods = method.stream()
				.map(m -> EnumUtils.parse(HttpMethod.class, m.name()).name())
				.collect(Collectors.joining(","));

		listener.setMethods(methods);
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
		dispatcher.registerServiceClient(createServiceClient(List.of(ApiListenerServletTest.Methods.GET, ApiListenerServletTest.Methods.POST), uri));
		testMultipleMethods(uri);
	}

	@Test
	void testFindMatchSingleAsterisk() throws ListenerException {
		ApiListener listener = createServiceClient(ApiListenerServletTest.Methods.GET, "/customers/*/addresses/345");
		dispatcher.registerServiceClient(listener);

		List<ApiDispatchConfig> matchingConfig = dispatcher.findMatchingConfigsForUri("/customers/123/addresses/345");

		assertEquals(1, matchingConfig.size());
	}

	@ParameterizedTest
	@CsvSource({
			"/customers/**, /customers/123/addresses/345",
			"/employees/**, /employees/123/departments/456/seats/52",
			"/customers/**, /customers/123/addresses/345"
	})
	void testFindMatchDoubleAsterisk(String uriPattern, String expectedUri) throws ListenerException {
		ApiListener listener = createServiceClient(ApiListenerServletTest.Methods.GET, uriPattern);
		dispatcher.registerServiceClient(listener);

		List<ApiDispatchConfig> matchingConfig = dispatcher.findMatchingConfigsForUri(expectedUri);

		assertEquals(1, matchingConfig.size());
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
