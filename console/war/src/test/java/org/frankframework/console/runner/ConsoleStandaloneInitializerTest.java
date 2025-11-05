package org.frankframework.console.runner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("slow")
class ConsoleStandaloneInitializerTest {

	private static ConfigurableApplicationContext applicationContext = null;

	@BeforeEach
	void setUp() throws IOException {
		System.setProperty("application.security.console.authentication.type", "NONE");
		SpringApplication springApplication = ConsoleStandaloneInitializer.configureApplication();
		applicationContext = springApplication.run();
	}

	@AfterEach
	void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	void contextLoads() {
		assertTrue(applicationContext.isRunning());
	}

	@Test
	@WithMockUser(roles = {"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	void testStandaloneSpecificEndpoints() {
		// Make sure to use the right context and port for the Tomcat server
		TomcatServletWebServerFactory tomcat = applicationContext.getBean("tomcat", TomcatServletWebServerFactory.class);
		String baseUrl = String.format("http://localhost:%d/%s/iaf/api/", tomcat.getPort(), tomcat.getContextPath());
		RestTemplate restTemplate = new RestTemplateBuilder().errorHandler(ignoreErrorHandler()).build();

		ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "server/health", String.class);
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(503)), "Expected 503 when no cluster members are available");

		response = restTemplate.getForEntity(baseUrl + "securityitems", String.class);
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(503)), "Expected 503 for securityitems when no cluster members are available");

		response = restTemplate.getForEntity(baseUrl + "logging", String.class);
		assertTrue(response.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(503)), "Expected 503 when no cluster members are available");

	}

	private ResponseErrorHandler ignoreErrorHandler() {
		return new DefaultResponseErrorHandler() {
			@Override
			protected boolean hasError(HttpStatusCode statusCode) {
				return false;
			}
		};
	}


}
