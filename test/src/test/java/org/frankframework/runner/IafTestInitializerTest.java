package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Tag("slow")
class IafTestInitializerTest {

	/**
	 * Since we don't use @SpringBootApplication, we can't use @SpringBootTest here and need to manually configure the application
	 */
	@Test
	void contextLoads() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication(false);

		ConfigurableApplicationContext run = springApplication.run();

		assertTrue(run.isRunning());

		run.close();
	}
}
