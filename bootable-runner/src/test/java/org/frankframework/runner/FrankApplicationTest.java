package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FrankApplicationTest {

	@SuppressWarnings({ "NullAway.Init", "java:S2637" })
	private static FrankApplication frankApplication;

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@BeforeAll
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
