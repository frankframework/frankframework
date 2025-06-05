package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Tag("slow")
class IafTestInitializerTest {

	private static ConfigurableApplicationContext applicationContext = null;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
	static void setup() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();

		applicationContext = springApplication.run();
	}

	@AfterAll
	static void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	void contextLoads() {
		assertTrue(applicationContext.isRunning());
	}

	@Test
	void ladybugRuns() {
		// Make sure to use the right context and port for the Tomcat server
		TomcatServletWebServerFactory tomcat = applicationContext.getBean("tomcat", TomcatServletWebServerFactory.class);
		String url = String.format("http://localhost:%d/%s/iaf/ladybug/api/testtool", tomcat.getPort(), tomcat.getContextPath());

		// create a rest call to the ladybug endpoint at 'iaf/ladybug/api/testtool'  to check if it is running
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		assertTrue(response.getStatusCode().is2xxSuccessful(), "Ladybug endpoint should return 2xx status");
	}
}
