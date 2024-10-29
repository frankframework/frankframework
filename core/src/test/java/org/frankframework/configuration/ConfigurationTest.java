package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.Adapter;
import org.frankframework.scheduler.job.IJob;

class ConfigurationTest {

	private Configuration configuration;

	@BeforeEach
	void setup() {
		configuration = new Configuration();
	}

	@Test
	void getScheduledJob() {
		// Act
		IJob job = configuration.getScheduledJob("Job");

		// Assert
		assertFalse(configuration.isActive());
		assertNull(job);
	}

	@Test
	void getScheduledJobs() {
		// Act / Assert
		assertFalse(configuration.isActive());
		assertTrue(configuration.getScheduledJobs().isEmpty());
	}

	@Test
	void getRegisteredAdapter() {
		// Act
		Adapter adapter = configuration.getRegisteredAdapter("Adapter");

		// Assert
		assertFalse(configuration.isActive());
		assertNull(adapter);
	}

	@Test
	void getRegisteredAdapters() {
		// Act / Assert
		assertFalse(configuration.isActive());
		assertTrue(configuration.getRegisteredAdapters().isEmpty());
	}
}
