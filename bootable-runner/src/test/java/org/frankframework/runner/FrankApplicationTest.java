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
class FrankApplicationTest {

	@SuppressWarnings({ "NullAway.Init", "java:S2637" })
	private static FrankApplication frankApplication;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
//	Enable this once JUNIT 6.1.0 has been released
//	@SetSystemProperty(key = "configurations.names", value = "") // Don't load configurations to speed things up.
	static void setup() throws IOException {
		frankApplication = new FrankApplication();
		frankApplication.run();
	}

	@AfterAll
	static void tearDown() {
		FrankApplication.exit(frankApplication);
	}

	@Test
	void contextLoads() {
		assertTrue(frankApplication.isRunning());
	}

}
