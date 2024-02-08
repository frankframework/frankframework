package org.frankframework.http.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.core.ListenerException;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.util.EnumUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ApiServiceDispatcherTest {

	private ApiServiceDispatcher dispatcher = null;
	private static final int NR_OF_THREADS = 100;

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
		for (int i = 0; i < NR_OF_THREADS; i++) {
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
		assertEquals(NR_OF_THREADS, dispatcher.getPatternClients().size());
	}

	private class CreateListener implements Runnable {
		private final String name;

		public CreateListener(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			double timeout = Math.ceil(Math.random()*3);
			try {
				Thread.sleep((long) timeout);
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
			"/customers/123/addresses/345, 1, /customers/**",
			"/customers/123/addresses/345, 2, /customers/**|/customers/*/addresses/*/**",
			"/employees/123/departments/456/seats/52, 1, /employees/**",
			"/customers/123/addresses/345, 1, /customers/**",
			"/customers/123/addresses/345, 0, /employees/**",
			"/employees/123/departments/456/seats/52, 1, /employees/*/departments/**",
			"/employees/123/departments/456/seats/52, 1, /employees/*/departments/*/seats/*",
			"/customers/123/departments/456/seats/52, 0, /employees/*/departments/*/seats/*",
			"/employees/123/departments/456/seats/52, 0, /employees/*/departments/*",
	})
	void testFindMatchWithWildcards(String requestUri, int expectedNrOfMatches, String uriPatterns) throws ListenerException {
		for (String uriPattern : uriPatterns.split("\\|")) {
			ApiListener listener = createServiceClient(ApiListenerServletTest.Methods.GET, uriPattern);
			dispatcher.registerServiceClient(listener);
		}

		List<ApiDispatchConfig> matchingConfig = dispatcher.findMatchingConfigsForUri(requestUri);

		assertEquals(expectedNrOfMatches, matchingConfig.size());
	}

	@ParameterizedTest
	@CsvSource({
			"/customers/123/addresses/345, /customers/**, /customers/**",
			"/customers/123/addresses/345, /customers/*/addresses/**, /customers/**|/customers/*/addresses/**",
			"/customers/123/addresses/345, /customers/**, /customers/**|/customers/*/addresses/*/**",
			"/employees/123/departments/456/seats/52, , /customers/**",
			"/employees/123/departments/456/seats/52, , /employees/*/departments/*",
			"/customers/123/addresses/345, /customers/**, /customers/**|/employees/**",
			"/customers/123/addresses/345, /customers/*/addresses/**, /customers/**|/customers/*/addresses/**",
			"/customers/123/addresses/345, /customers/*/addresses/**, /customers/**|/customers/{custno}/addresses/**",
			"/customers/123/addresses/345, /customers/*/addresses/*, /customers/**|/customers/*/addresses/**|/customers/*/addresses/*",
			"/employees/123/departments/456/seats/52, /employees/*/departments/*/seats/*, /employees/**|/employees/*/departments/*/seats/*",
	})
	void testFindConfigBestMatchWithWildcards(String requestUri, String expectedMatch, String uriPatterns) throws ListenerException {
		for (String uriPattern : uriPatterns.split("\\|")) {
			ApiListener listener = createServiceClient(ApiListenerServletTest.Methods.GET, uriPattern);
			dispatcher.registerServiceClient(listener);
		}

		ApiDispatchConfig matchingConfig = dispatcher.findConfigForUri(requestUri);

		if (StringUtils.isBlank(expectedMatch)) {
			assertNull(matchingConfig);
		} else {
			assertEquals(expectedMatch, matchingConfig.getUriPattern());
		}
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
