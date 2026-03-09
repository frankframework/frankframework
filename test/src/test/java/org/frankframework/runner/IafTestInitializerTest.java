package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Tag("slow")
class IafTestInitializerTest {

	@SuppressWarnings({ "NullAway.Init", "java:S2637" })
	private static FrankApplication frankApplication;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		System.setProperty("configurations.names", ""); // Don't load configurations to speed things up.

		frankApplication = new FrankApplication();
		frankApplication.run();
	}

	@AfterAll
	static void tearDown() {
		FrankApplication.exit(frankApplication);

		System.clearProperty("configurations.names");
	}

	@Test
	void contextLoads() {
		assertTrue(frankApplication.isRunning());
	}

	@Test
	void ladybugRuns() {
		// Make sure to use the right context and port for the Tomcat server
		TomcatServletWebServerFactory tomcat = frankApplication.getBean("tomcat");
		String baseUrl = String.format("http://localhost:%d/%s/iaf/ladybug/api/", tomcat.getPort(), tomcat.getContextPath());

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "testtool", String.class);
		assertTrue(response.getStatusCode().is2xxSuccessful(), "Ladybug testtool endpoint should return 2xx status");

		// create a rest call to the ladybug endpoints at 'iaf/ladybug/api/testtool/views' and 'iaf/ladybug/api/report/variables'
		ResponseEntity<String> viewResponse = restTemplate.getForEntity(baseUrl + "testtool/views", String.class);
		assertTrue(viewResponse.getStatusCode().is2xxSuccessful(), "testtool/views should return 2xx status");

		ResponseEntity<String> variablesResponse = restTemplate.getForEntity(baseUrl + "report/variables", String.class);
		assertTrue(variablesResponse.getStatusCode().is2xxSuccessful(), "report/variables should return 2xx status");
	}
}
