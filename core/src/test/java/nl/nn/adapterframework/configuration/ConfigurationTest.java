package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.scheduler.job.IJob;

public class ConfigurationTest {

	private Configuration configuration;

	@Before
	public void setup() {
		configuration = new Configuration();
	}

	@Test
	public void getScheduledJob() {
		// Act
		IJob job = configuration.getScheduledJob("Job");

		// Assert
		assertFalse(configuration.isActive());
		assertNull(job);
	}

	@Test
	public void getScheduledJobs() {
		// Act / Assert
		assertFalse(configuration.isActive());
		assertTrue(configuration.getScheduledJobs().isEmpty());
	}

	@Test
	public void getRegisteredAdapter() {
		// Act
		Adapter adapter = configuration.getRegisteredAdapter("Adapter");

		// Assert
		assertFalse(configuration.isActive());
		assertNull(adapter);
	}

	@Test
	public void getRegisteredAdapters() {
		// Act / Assert
		assertFalse(configuration.isActive());
		assertTrue(configuration.getRegisteredAdapters().isEmpty());
	}
}
