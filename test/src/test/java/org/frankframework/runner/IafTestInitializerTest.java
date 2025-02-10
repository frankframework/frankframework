package org.frankframework.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class IafTestInitializerTest {

	@Test
	public void contextLoads() throws IOException {
		SpringApplication springApplication = IafTestInitializer.configureApplication();

		ConfigurableApplicationContext run = springApplication.run();

		assertTrue(run.isRunning());

		run.close();
	}
}
