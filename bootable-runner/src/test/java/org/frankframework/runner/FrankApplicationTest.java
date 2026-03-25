package org.frankframework.runner;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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

		assertTrue(frankApplication.isRunning());
	}

	@AfterAll
	static void tearDown() {
		FrankApplication.exit(frankApplication);
	}

	@Test
	@DisplayName("Wait max. 1 minute and verify the Frank!Application has started succesfully.")
	void contextLoads() {
		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(1))
				.until(frankApplication::hasStarted);
	}

}
